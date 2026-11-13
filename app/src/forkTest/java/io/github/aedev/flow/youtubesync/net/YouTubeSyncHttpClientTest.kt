package io.github.aedev.flow.youtubesync.net

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.youtubesync.account.GoogleYouTubeIdentityApi
import io.github.aedev.flow.youtubesync.account.LinkedAccount
import kotlinx.coroutines.runBlocking
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

/** The official channel's client stays apart from playback and browsing (ADR 0001). */
class YouTubeSyncHttpClientTest {
    @Test
    fun `the client keeps no cookies and no cache and adds nothing to requests`() {
        val client = YouTubeSyncHttpClient.builder().build()

        assertThat(client.cookieJar).isSameInstanceAs(CookieJar.NO_COOKIES)
        assertThat(client.cache).isNull()
        assertThat(client.interceptors).isEmpty()
        assertThat(client.networkInterceptors).isEmpty()
    }

    @Test
    fun `reading the channel sends only the bearer token`() =
        runBlocking<Unit> {
            val seen = mutableListOf<Request>()
            val client =
                YouTubeSyncHttpClient
                    .builder()
                    .addInterceptor(answer(seen, """{"items":[{"id":"UC123","snippet":{"title":"My channel"}}]}"""))
                    .build()

            val account = GoogleYouTubeIdentityApi(client).ownChannel("access-1")

            assertThat(account).isEqualTo(LinkedAccount("UC123", "My channel"))
            val request = seen.single()
            assertThat(request.url.host).isEqualTo("www.googleapis.com")
            assertThat(request.url.queryParameter("mine")).isEqualTo("true")
            assertThat(request.headers.names()).containsExactly("Authorization")
            assertThat(request.header("Authorization")).isEqualTo("Bearer access-1")
        }

    @Test
    fun `revoking posts the token to Google's revocation endpoint`() =
        runBlocking<Unit> {
            val seen = mutableListOf<Request>()
            val client = YouTubeSyncHttpClient.builder().addInterceptor(answer(seen, "{}")).build()

            GoogleYouTubeIdentityApi(client).revoke("refresh-secret")

            val request = seen.single()
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.url.toString()).isEqualTo("https://oauth2.googleapis.com/revoke")
            assertThat(request.header("Cookie")).isNull()
        }

    private fun answer(
        seen: MutableList<Request>,
        body: String,
    ) = Interceptor { chain ->
        seen += chain.request()
        Response
            .Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }
}
