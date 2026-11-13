package io.github.aedev.flow.youtubesync.account

import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Links, uses and unlinks the Linked Account of the running profile through the official channel
 * (ADR 0001). This is the seam the YouTube Sync and the settings screen talk to.
 */
@Singleton
class LinkedAccountManager
    @Inject
    constructor(
        private val store: LinkedAccountStore,
        private val oauth: OAuthGateway,
        private val identity: YouTubeIdentityApi,
        private val config: OAuthConfig,
    ) {
        private val mutex = Mutex()

        val account: Flow<LinkedAccount?> = store.account

        val isConfigured: Boolean get() = config.isConfigured

        /** The browser intent that starts linking. Throws [LinkedAccountException.NotConfigured] without a client id. */
        fun authorizationIntent(): Intent = oauth.authorizationIntent()

        /**
         * Finishes linking with the browser's [result]: exchanges the code, reads the account's channel
         * and saves both. A previously linked account is replaced.
         */
        suspend fun completeLink(result: Intent?): LinkedAccount =
            mutex.withLock {
                val authState = oauth.completeAuthorization(result)
                val fresh = oauth.freshAccessToken(authState)
                val account = identity.ownChannel(fresh.accessToken)
                store.save(account, fresh.authState)
                account
            }

        /** A valid access token for the YouTube Data API, refreshed and saved when needed. */
        suspend fun accessToken(): String =
            mutex.withLock {
                val authState = store.authState() ?: throw LinkedAccountException.NotLinked()
                val fresh = oauth.freshAccessToken(authState)
                if (fresh.authState != authState) store.updateAuthState(fresh.authState)
                fresh.accessToken
            }

        /**
         * Unlinks the account: revokes the grant at Google and forgets the tokens. Nothing on YouTube is
         * touched. The local state is cleared even when revoking fails (offline, grant already revoked).
         */
        suspend fun unlink() {
            mutex.withLock {
                val authState = store.authState()
                store.clear()
                val token = authState?.let(oauth::revocableToken) ?: return
                runCatching { identity.revoke(token) }
                    .onFailure { Log.w(TAG, "Revoking the YouTube grant failed; local tokens are cleared anyway", it) }
            }
        }

        private companion object {
            const val TAG = "LinkedAccountManager"
        }
    }
