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
                if (e.reason != "subscriptionNotFound" && e.reason != "notFound") throw e
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
        private data class ResourceId(
            val channelId: String? = null,
        )

        @Serializable
        private data class Thumbnails(
            val default: Thumbnail? = null,
            val medium: Thumbnail? = null,
            val high: Thumbnail? = null,
        ) {
            fun best(): String = (high ?: medium ?: default)?.url.orEmpty()
        }

        @Serializable
        private data class Thumbnail(
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
        }
    }
