package io.github.aedev.flow.youtubesync.remote

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.youtubesync.net.YouTubeSyncHttpClient
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertThrows
import org.junit.Test

/** The YouTube Data API adapter against canned Google responses (spec: secondary seam). */
class YouTubeDataApiAccountTest {
    private val seen = mutableListOf<Request>()
    private val answers = ArrayDeque<Pair<Int, String>>()

    private val account =
        YouTubeDataApiAccount(
            YouTubeSyncHttpClient
                .builder()
                .addInterceptor(
                    Interceptor { chain ->
                        seen += chain.request()
                        val (code, body) = answers.removeFirst()
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(code)
                            .message("x")
                            .body(body.toResponseBody("application/json".toMediaType()))
                            .build()
                    },
                ).build(),
            AccessTokenSource { "token-1" },
        )

    private fun error(reason: String) = """{"error":{"code":0,"errors":[{"reason":"$reason"}]}}"""

    private fun item(
        id: String,
        channel: String,
    ) =
        """{"id":"$id","snippet":{"title":"T $channel","resourceId":{"kind":"youtube#channel","channelId":"$channel"},"thumbnails":{"default":{"url":"d-$channel"},"high":{"url":"h-$channel"}}}}"""

    @Test
    fun `subscriptions reads every page`() =
        runBlocking<Unit> {
            answers += 200 to """{"items":[${item("s1", "A")}],"nextPageToken":"p2"}"""
            answers += 200 to """{"items":[${item("s2", "B")}]}"""

            val subscriptions = account.subscriptions()

            assertThat(subscriptions)
                .containsExactly(RemoteSubscription("s1", "A", "T A", "h-A"), RemoteSubscription("s2", "B", "T B", "h-B"))
                .inOrder()
            assertThat(seen[0].url.queryParameter("mine")).isEqualTo("true")
            assertThat(seen[0].url.queryParameter("pageToken")).isNull()
            assertThat(seen[1].url.queryParameter("pageToken")).isEqualTo("p2")
            assertThat(seen.map { it.header("Authorization") }).containsExactly("Bearer token-1", "Bearer token-1")
        }

    @Test
    fun `subscribing posts the channel as a resource id`() =
        runBlocking<Unit> {
            answers += 200 to "{}"

            account.subscribe("UC1")

            val request = seen.single()
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.url.queryParameter("part")).isEqualTo("snippet")
            val body = Buffer().also { request.body!!.writeTo(it) }.readUtf8()
            assertThat(body).contains("\"channelId\":\"UC1\"")
            assertThat(body).contains("\"kind\":\"youtube#channel\"")
        }

    @Test
    fun `subscribing to a channel already subscribed counts as success`() =
        runBlocking<Unit> {
            answers += 400 to error("subscriptionDuplicate")

            account.subscribe("UC1")
        }

    @Test
    fun `unsubscribing from a subscription already gone counts as success`() =
        runBlocking<Unit> {
            answers += 404 to error("subscriptionNotFound")

            account.unsubscribe("s1")

            assertThat(seen.single().method).isEqualTo("DELETE")
            assertThat(seen.single().url.queryParameter("id")).isEqualTo("s1")
        }

    @Test
    fun `errors map to typed failures`() {
        fun failure(
            code: Int,
            body: String,
        ): Throwable {
            answers += code to body
            return assertThrows(YouTubeAccountException::class.java) { runBlocking { account.subscribe("UC1") } }
        }

        assertThat(failure(403, error("quotaExceeded"))).isInstanceOf(YouTubeAccountException.QuotaExceeded::class.java)
        assertThat(failure(401, error("authError"))).isInstanceOf(YouTubeAccountException.AuthorizationInvalid::class.java)
        assertThat(failure(403, error("insufficientPermissions"))).isInstanceOf(YouTubeAccountException.AuthorizationInvalid::class.java)
        assertThat(failure(503, "")).isInstanceOf(YouTubeAccountException.Transient::class.java)
        val rejected = failure(403, error("subscriptionForbidden"))
        assertThat(rejected).isInstanceOf(YouTubeAccountException.ItemRejected::class.java)
        assertThat((rejected as YouTubeAccountException.ItemRejected).reason).isEqualTo("subscriptionForbidden")
    }

    private fun Request.bodyText() = Buffer().also { body!!.writeTo(it) }.readUtf8()

    @Test
    fun `playlists reads every page of the account's own playlists`() =
        runBlocking<Unit> {
            answers += 200 to """{"items":[{"id":"PL1","snippet":{"title":"One"}}],"nextPageToken":"n"}"""
            answers += 200 to """{"items":[{"id":"PL2","snippet":{"title":"Two"}}]}"""

            val playlists = account.playlists()

            assertThat(playlists.map { it.playlistId to it.kind })
                .containsExactly("PL1" to PlaylistKind.OWNED, "PL2" to PlaylistKind.OWNED)
                .inOrder()
            assertThat(seen[0].url.queryParameter("mine")).isEqualTo("true")
            assertThat(seen[1].url.queryParameter("pageToken")).isEqualTo("n")
        }

    @Test
    fun `playlist items mark deleted and private videos as unavailable`() =
        runBlocking<Unit> {
            answers += 200 to
                """{"items":[
                {"id":"i1","snippet":{"title":"Ok","resourceId":{"videoId":"v1"},"videoOwnerChannelId":"UC1","videoOwnerChannelTitle":"C"}},
                {"id":"i2","snippet":{"title":"Deleted video","resourceId":{"videoId":"v2"}}}
                ]}"""

            val items = account.playlistItems("PL1")

            assertThat(items.map { it.videoId to it.available }).containsExactly("v1" to true, "v2" to false).inOrder()
            assertThat(items.first().channelTitle).isEqualTo("C")
            assertThat(seen.single().url.queryParameter("playlistId")).isEqualTo("PL1")
        }

    @Test
    fun `a new playlist is created private`() =
        runBlocking<Unit> {
            answers += 200 to """{"id":"PLnew"}"""

            assertThat(account.createPlaylist("Mix")).isEqualTo("PLnew")
            val body = seen.single().bodyText()
            assertThat(body).contains("\"privacyStatus\":\"private\"")
            assertThat(body).contains("\"title\":\"Mix\"")
        }

    @Test
    fun `a rename sends the current description back unchanged`() =
        runBlocking<Unit> {
            answers += 200 to """{"items":[{"id":"PL1","snippet":{"title":"Old","description":"Keep me"}}]}"""
            answers += 200 to "{}"

            account.renamePlaylist("PL1", "New")

            assertThat(seen[1].method).isEqualTo("PUT")
            val body = seen[1].bodyText()
            assertThat(body).contains("\"title\":\"New\"")
            assertThat(body).contains("\"description\":\"Keep me\"")
            assertThat(body).doesNotContain("privacyStatus")
        }

    @Test
    fun `adding, moving and removing playlist entries`() =
        runBlocking<Unit> {
            answers += 200 to """{"id":"item-9"}"""
            answers += 200 to "{}"
            answers += 404 to error("playlistItemNotFound")

            assertThat(account.addPlaylistItem("PL1", "v9")).isEqualTo("item-9")
            account.movePlaylistItem("PL1", "item-9", "v9", 0)
            account.removePlaylistItem("item-gone")

            assertThat(seen.map { it.method }).containsExactly("POST", "PUT", "DELETE").inOrder()
            assertThat(seen[1].bodyText()).contains("\"position\":0")
        }

    @Test
    fun `deleting a playlist already gone counts as success`() =
        runBlocking<Unit> {
            answers += 404 to error("playlistNotFound")

            account.deletePlaylist("PL1")
        }

    @Test
    fun `playlist errors map to typed failures`() {
        fun failure(
            code: Int,
            body: String,
        ): Throwable {
            answers += code to body
            return assertThrows(YouTubeAccountException::class.java) { runBlocking { account.addPlaylistItem("PL1", "v1") } }
        }

        assertThat(failure(403, error("quotaExceeded"))).isInstanceOf(YouTubeAccountException.QuotaExceeded::class.java)
        assertThat(failure(401, error("authError"))).isInstanceOf(YouTubeAccountException.AuthorizationInvalid::class.java)
        assertThat(failure(500, error("backendError"))).isInstanceOf(YouTubeAccountException.Transient::class.java)
        assertThat(failure(404, error("videoNotFound"))).isInstanceOf(YouTubeAccountException.ItemRejected::class.java)
    }
}
