package io.github.aedev.flow.youtubesync.remote

import io.github.aedev.flow.youtubesync.account.LinkedAccountException
import io.github.aedev.flow.youtubesync.account.LinkedAccountManager
import io.github.aedev.flow.youtubesync.net.YouTubeSyncHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject

/** Gives the access token of the Linked Account. A seam so the adapter can be tested without OAuth. */
fun interface AccessTokenSource {
    suspend fun accessToken(): String
}

class LinkedAccountTokenSource
    @Inject
    constructor(
        private val manager: LinkedAccountManager,
    ) : AccessTokenSource {
        override suspend fun accessToken(): String =
            try {
                manager.accessToken()
            } catch (e: LinkedAccountException.NotLinked) {
                throw YouTubeAccountException.NotLinked()
            } catch (e: LinkedAccountException.Transient) {
                throw YouTubeAccountException.Transient(e)
            } catch (e: LinkedAccountException) {
                throw YouTubeAccountException.AuthorizationInvalid(e)
            }
    }

/** [YouTubeAccount] over the YouTube Data API v3, through the official channel's HTTP client. */
class YouTubeDataApiAccount
    @Inject
    constructor(
        @YouTubeSyncHttp private val client: OkHttpClient,
        private val tokens: AccessTokenSource,
    ) : YouTubeAccount {
        private val json = Json { ignoreUnknownKeys = true }
        private val base = "https://www.googleapis.com/youtube/v3/"

        override suspend fun subscriptions(): List<RemoteSubscription> {
            val all = mutableListOf<RemoteSubscription>()
            var pageToken: String? = null
            do {
                val url =
                    "${base}subscriptions"
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("part", "snippet")
                        .addQueryParameter("mine", "true")
                        .addQueryParameter("maxResults", "50")
                        .apply { pageToken?.let { addQueryParameter("pageToken", it) } }
                        .build()
                val page = json.decodeFromString<SubscriptionPage>(call(get(url)))
                page.items.mapNotNullTo(all) { item ->
                    val channelId = item.snippet.resourceId.channelId ?: return@mapNotNullTo null
                    RemoteSubscription(
                        subscriptionId = item.id,
                        channelId = channelId,
                        title = item.snippet.title,
                        thumbnailUrl = item.snippet.thumbnails.best(),
                    )
                }
                pageToken = page.nextPageToken
            } while (pageToken != null)
            return all
        }

        override suspend fun subscribe(channelId: String) {
            val body =
                buildJsonObject {
                    putJsonObject("snippet") {
                        putJsonObject("resourceId") {
                            put("kind", "youtube#channel")
                            put("channelId", channelId)
                        }
                    }
                }.toString().toRequestBody(JSON)
            val url = "${base}subscriptions".toHttpUrl().newBuilder().addQueryParameter("part", "snippet").build()
            try {
                call(authorized(url).post(body).build())
            } catch (e: Duplicate) {
                // Already subscribed: the goal is reached.
            }
        }

        override suspend fun unsubscribe(subscriptionId: String) {
            val url = "${base}subscriptions".toHttpUrl().newBuilder().addQueryParameter("id", subscriptionId).build()
            try {
                call(authorized(url).delete().build())
            } catch (e: YouTubeAccountException.ItemRejected) {
                if (e.reason !in NOT_FOUND) throw e
            }
        }

        override suspend fun playlists(): List<RemotePlaylist> =
            pages<PlaylistPage>("playlists") {
                addQueryParameter("part", "snippet")
                addQueryParameter("mine", "true")
            }.flatMap { page ->
                page.items.map { RemotePlaylist(it.id, it.snippet.title, PlaylistKind.OWNED, it.snippet.thumbnails.best()) }
            }

        override suspend fun playlistItems(playlistId: String): List<RemotePlaylistItem> =
            pages<PlaylistItemPage>("playlistItems") {
                addQueryParameter("part", "snippet")
                addQueryParameter("playlistId", playlistId)
            }.flatMap { page ->
                page.items.mapNotNull { item ->
                    val snippet = item.snippet
                    val videoId = snippet.resourceId.videoId ?: return@mapNotNull null
                    RemotePlaylistItem(
                        itemId = item.id,
                        videoId = videoId,
                        title = snippet.title,
                        channelTitle = snippet.videoOwnerChannelTitle.orEmpty(),
                        channelId = snippet.videoOwnerChannelId.orEmpty(),
                        thumbnailUrl = snippet.thumbnails.best(),
                        // Deleted and private videos come without an owner channel.
                        available = snippet.videoOwnerChannelId != null,
                    )
                }
            }

        override suspend fun createPlaylist(title: String): String {
            val body =
                buildJsonObject {
                    putJsonObject("snippet") { put("title", title) }
                    putJsonObject("status") { put("privacyStatus", "private") }
                }
            val url = "${base}playlists".toHttpUrl().newBuilder().addQueryParameter("part", "snippet,status").build()
            return json.decodeFromString<IdOnly>(call(authorized(url).post(body.toString().toRequestBody(JSON)).build())).id
        }

        override suspend fun renamePlaylist(
            playlistId: String,
            title: String,
        ) {
            // An update replaces the whole snippet: send the description back so it is never changed.
            val readUrl =
                "${base}playlists"
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("part", "snippet")
                    .addQueryParameter("id", playlistId)
                    .build()
            val current =
                json.decodeFromString<PlaylistPage>(call(get(readUrl))).items.firstOrNull()
                    ?: throw YouTubeAccountException.ItemRejected("playlistNotFound")
            val body =
                buildJsonObject {
                    put("id", playlistId)
                    putJsonObject("snippet") {
                        put("title", title)
                        put("description", current.snippet.description)
                        current.snippet.defaultLanguage?.let { put("defaultLanguage", it) }
                    }
                }
            val url = "${base}playlists".toHttpUrl().newBuilder().addQueryParameter("part", "snippet").build()
            call(authorized(url).put(body.toString().toRequestBody(JSON)).build())
        }

        override suspend fun deletePlaylist(playlistId: String) {
            val url = "${base}playlists".toHttpUrl().newBuilder().addQueryParameter("id", playlistId).build()
            ignoringNotFound { call(authorized(url).delete().build()) }
        }

        override suspend fun addPlaylistItem(
            playlistId: String,
            videoId: String,
        ): String {
            val body =
                buildJsonObject {
                    putJsonObject("snippet") {
                        put("playlistId", playlistId)
                        putJsonObject("resourceId") {
                            put("kind", "youtube#video")
                            put("videoId", videoId)
                        }
                    }
                }
            val url = "${base}playlistItems".toHttpUrl().newBuilder().addQueryParameter("part", "snippet").build()
            return json.decodeFromString<IdOnly>(call(authorized(url).post(body.toString().toRequestBody(JSON)).build())).id
        }

        override suspend fun removePlaylistItem(itemId: String) {
            val url = "${base}playlistItems".toHttpUrl().newBuilder().addQueryParameter("id", itemId).build()
            ignoringNotFound { call(authorized(url).delete().build()) }
        }

        override suspend fun movePlaylistItem(
            playlistId: String,
            itemId: String,
            videoId: String,
            position: Int,
        ) {
            val body =
                buildJsonObject {
                    put("id", itemId)
                    putJsonObject("snippet") {
                        put("playlistId", playlistId)
                        put("position", position)
                        putJsonObject("resourceId") {
                            put("kind", "youtube#video")
                            put("videoId", videoId)
                        }
                    }
                }
            val url = "${base}playlistItems".toHttpUrl().newBuilder().addQueryParameter("part", "snippet").build()
            call(authorized(url).put(body.toString().toRequestBody(JSON)).build())
        }

        /** Reads every page of a list call, 50 items a time. */
        private suspend inline fun <reified P : Paged> pages(
            resource: String,
            query: HttpUrl.Builder.() -> Unit,
        ): List<P> {
            val all = mutableListOf<P>()
            var pageToken: String? = null
            do {
                val url =
                    "$base$resource"
                        .toHttpUrl()
                        .newBuilder()
                        .apply(query)
                        .addQueryParameter("maxResults", "50")
                        .apply { pageToken?.let { addQueryParameter("pageToken", it) } }
                        .build()
                val page = json.decodeFromString<P>(call(get(url)))
                all += page
                pageToken = page.nextPageToken
            } while (pageToken != null)
            return all
        }

        private inline fun ignoringNotFound(block: () -> Unit) {
            try {
                block()
            } catch (e: YouTubeAccountException.ItemRejected) {
                if (e.reason !in NOT_FOUND) throw e
            }
        }

        private suspend fun get(url: HttpUrl): Request = authorized(url).get().build()

        private suspend fun authorized(url: HttpUrl): Request.Builder =
            Request.Builder().url(url).header("Authorization", "Bearer ${tokens.accessToken()}")

        private suspend fun call(request: Request): String =
            withContext(Dispatchers.IO) {
                val (code, body) =
                    try {
                        client.newCall(request).execute().use { it.code to it.body.string() }
                    } catch (e: IOException) {
                        throw YouTubeAccountException.Transient(e)
                    }
                if (code in 200..299) return@withContext body
                val reason =
                    runCatching {
                        json
                            .decodeFromString<ErrorBody>(body)
                            .error.errors
                            .firstOrNull()
                            ?.reason
                    }.getOrNull()
                throw errorFor(code, reason)
            }

        /** Maps an API error to a [YouTubeAccountException] (reasons from the YouTube Data API error docs). */
        private fun errorFor(
            code: Int,
            reason: String?,
        ): Exception =
            when {
                reason == "subscriptionDuplicate" -> {
                    Duplicate()
                }

                reason == "quotaExceeded" || reason == "dailyLimitExceeded" || reason == "rateLimitExceeded" -> {
                    YouTubeAccountException.QuotaExceeded()
                }

                code == 401 -> {
                    YouTubeAccountException.AuthorizationInvalid(IOException("HTTP 401"))
                }

                code == 403 && (reason == "insufficientPermissions" || reason == "forbidden") -> {
                    YouTubeAccountException.AuthorizationInvalid(IOException("HTTP 403 $reason"))
                }

                code >= 500 || code == 429 -> {
                    YouTubeAccountException.Transient(IOException("HTTP $code"))
                }

                else -> {
                    YouTubeAccountException.ItemRejected(reason ?: "HTTP $code")
                }
            }

        /** Internal marker: the subscription already exists. */
        private class Duplicate : Exception()

        /** A page of a list call. */
        interface Paged {
            val nextPageToken: String?
        }

        @Serializable
        private data class IdOnly(
            val id: String,
        )

        @Serializable
        data class PlaylistPage(
            val items: List<PlaylistResource> = emptyList(),
            override val nextPageToken: String? = null,
        ) : Paged

        @Serializable
        data class PlaylistResource(
            val id: String,
            val snippet: PlaylistSnippet = PlaylistSnippet(),
        )

        @Serializable
        data class PlaylistSnippet(
            val title: String = "",
            val description: String = "",
            val defaultLanguage: String? = null,
            val thumbnails: Thumbnails = Thumbnails(),
        )

        @Serializable
        data class PlaylistItemPage(
            val items: List<PlaylistItemResource> = emptyList(),
            override val nextPageToken: String? = null,
        ) : Paged

        @Serializable
        data class PlaylistItemResource(
            val id: String,
            val snippet: PlaylistItemSnippet = PlaylistItemSnippet(),
        )

        @Serializable
        data class PlaylistItemSnippet(
            val title: String = "",
            val resourceId: ResourceId = ResourceId(),
            val thumbnails: Thumbnails = Thumbnails(),
            val videoOwnerChannelId: String? = null,
            val videoOwnerChannelTitle: String? = null,
        )

        @Serializable
        private data class SubscriptionPage(
            val items: List<SubscriptionItem> = emptyList(),
            val nextPageToken: String? = null,
        )

        @Serializable
        private data class SubscriptionItem(
            val id: String,
            val snippet: SubscriptionSnippet,
        )

        @Serializable
        private data class SubscriptionSnippet(
            val title: String = "",
            val resourceId: ResourceId = ResourceId(),
            val thumbnails: Thumbnails = Thumbnails(),
        )

        @Serializable
        data class ResourceId(
            val channelId: String? = null,
            val videoId: String? = null,
        )

        @Serializable
        data class Thumbnails(
            val default: Thumbnail? = null,
            val medium: Thumbnail? = null,
            val high: Thumbnail? = null,
        ) {
            fun best(): String = (high ?: medium ?: default)?.url.orEmpty()
        }

        @Serializable
        data class Thumbnail(
            val url: String = "",
        )

        @Serializable
        private data class ErrorBody(
            val error: ErrorDetail = ErrorDetail(),
        )

        @Serializable
        private data class ErrorDetail(
            val errors: List<ErrorReason> = emptyList(),
        )

        @Serializable
        private data class ErrorReason(
            val reason: String? = null,
        )

        private companion object {
            val JSON = "application/json; charset=utf-8".toMediaType()

            /** Reasons meaning "already gone", for removals: the goal is reached. */
            val NOT_FOUND = setOf("notFound", "subscriptionNotFound", "playlistNotFound", "playlistItemNotFound")
        }
    }
