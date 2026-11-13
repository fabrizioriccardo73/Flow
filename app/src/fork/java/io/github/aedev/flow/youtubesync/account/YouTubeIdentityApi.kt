package io.github.aedev.flow.youtubesync.account

import io.github.aedev.flow.youtubesync.net.YouTubeSyncHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

/** Who the Linked Account is, and revoking it. A seam: the real one calls Google, tests use a fake. */
interface YouTubeIdentityApi {
    /** The channel of the account [accessToken] belongs to. */
    suspend fun ownChannel(accessToken: String): LinkedAccount

    /** Revokes the grant behind [token] (an access or refresh token) at Google. */
    suspend fun revoke(token: String)
}

class GoogleYouTubeIdentityApi
    @Inject
    constructor(
        @YouTubeSyncHttp private val client: OkHttpClient,
    ) : YouTubeIdentityApi {
        private val json = Json { ignoreUnknownKeys = true }

        override suspend fun ownChannel(accessToken: String): LinkedAccount =
            withContext(Dispatchers.IO) {
                val url =
                    "https://www.googleapis.com/youtube/v3/channels"
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("part", "snippet")
                        .addQueryParameter("mine", "true")
                        .build()
                val request =
                    Request
                        .Builder()
                        .url(url)
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                val body = call(request)
                val channel =
                    json.decodeFromString<ChannelList>(body).items.firstOrNull()
                        ?: throw LinkedAccountException.AuthorizationInvalid(IllegalStateException("The account has no YouTube channel"))
                LinkedAccount(channelId = channel.id, channelTitle = channel.snippet.title)
            }

        override suspend fun revoke(token: String) {
            withContext(Dispatchers.IO) {
                val request =
                    Request
                        .Builder()
                        .url(OAuthConfig.REVOCATION_ENDPOINT)
                        .post(FormBody.Builder().add("token", token).build())
                        .build()
                call(request)
            }
        }

        private fun call(request: Request): String =
            try {
                client.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> {
                            response.body.string()
                        }

                        response.code == 400 || response.code == 401 || response.code == 403 -> {
                            throw LinkedAccountException.AuthorizationInvalid(IOException("HTTP ${response.code}"))
                        }

                        else -> {
                            throw LinkedAccountException.Transient(IOException("HTTP ${response.code}"))
                        }
                    }
                }
            } catch (e: IOException) {
                throw LinkedAccountException.Transient(e)
            }

        @Serializable
        private data class ChannelList(
            val items: List<Channel> = emptyList(),
        )

        @Serializable
        private data class Channel(
            val id: String,
            val snippet: Snippet,
        )

        @Serializable
        private data class Snippet(
            val title: String,
        )
    }
