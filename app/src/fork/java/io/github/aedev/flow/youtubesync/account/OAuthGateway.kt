package io.github.aedev.flow.youtubesync.account

import android.content.Intent

/** The OAuth side of the Linked Account. A seam: the real one is [AppAuthGateway], tests use a fake. */
interface OAuthGateway {
    /** The browser intent that asks the viewer to authorize Flow. */
    fun authorizationIntent(): Intent

    /** Turns the browser's [result] into a serialized auth state holding the tokens. */
    suspend fun completeAuthorization(result: Intent?): String

    /** A fresh access token for [authState], refreshing it when needed, with the auth state to save afterwards. */
    suspend fun freshAccessToken(authState: String): FreshToken

    /** The token to revoke for [authState] (the refresh token), or null when there is none. */
    fun revocableToken(authState: String): String?

    data class FreshToken(
        val accessToken: String,
        val authState: String,
    )
}
