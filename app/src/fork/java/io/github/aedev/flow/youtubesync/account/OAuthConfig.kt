package io.github.aedev.flow.youtubesync.account

import io.github.aedev.flow.BuildConfig

/** The Google OAuth client the Linked Account uses, set at build time (see FORK.md). */
data class OAuthConfig(
    val clientId: String,
    val redirectUri: String,
) {
    val isConfigured: Boolean get() = clientId.isNotBlank()

    companion object {
        const val AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        const val REVOCATION_ENDPOINT = "https://oauth2.googleapis.com/revoke"

        /**
         * Manage the account's subscriptions and playlists: the narrowest YouTube scope that allows writing them.
         * No profile or email scope: the account is shown by its channel.
         */
        const val YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube"

        fun fromBuild() =
            OAuthConfig(
                clientId = BuildConfig.YOUTUBE_SYNC_OAUTH_CLIENT_ID,
                redirectUri = BuildConfig.YOUTUBE_SYNC_OAUTH_REDIRECT_URI,
            )
    }
}
