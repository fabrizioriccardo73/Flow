package io.github.aedev.flow.youtubesync.account

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** [OAuthGateway] over AppAuth: authorization code with PKCE in a browser tab, no Google Play Services. */
@Singleton
class AppAuthGateway
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val config: OAuthConfig,
    ) : OAuthGateway {
        private val serviceConfig =
            AuthorizationServiceConfiguration(
                Uri.parse(OAuthConfig.AUTHORIZATION_ENDPOINT),
                Uri.parse(OAuthConfig.TOKEN_ENDPOINT),
            )

        private val service by lazy { AuthorizationService(context) }

        override fun authorizationIntent(): Intent {
            if (!config.isConfigured) throw LinkedAccountException.NotConfigured()
            val request =
                AuthorizationRequest
                    .Builder(serviceConfig, config.clientId, ResponseTypeValues.CODE, Uri.parse(config.redirectUri))
                    .setScopes(OAuthConfig.YOUTUBE_SCOPE)
                    // Ask again every time, so Google always returns a refresh token on a new link.
                    .setPrompt("consent")
                    .build()
            return service.getAuthorizationRequestIntent(request)
        }

        override suspend fun completeAuthorization(result: Intent?): String {
            val response = result?.let { AuthorizationResponse.fromIntent(it) }
            val error = result?.let { AuthorizationException.fromIntent(it) }
            if (response == null) throw error.toLinkedAccountException(cancelledIfUnknown = true)
            val state = AuthState(response, error)
            return suspendCancellableCoroutine { cont ->
                service.performTokenRequest(response.createTokenExchangeRequest()) { tokens, ex ->
                    state.update(tokens, ex)
                    if (tokens != null && state.refreshToken != null) {
                        cont.resume(state.jsonSerializeString())
                    } else {
                        cont.resumeWithException(ex.toLinkedAccountException(cancelledIfUnknown = false))
                    }
                }
            }
        }

        override suspend fun freshAccessToken(authState: String): OAuthGateway.FreshToken {
            val state =
                runCatching { AuthState.jsonDeserialize(authState) }
                    .getOrElse { throw LinkedAccountException.AuthorizationInvalid(it) }
            return suspendCancellableCoroutine { cont ->
                state.performActionWithFreshTokens(service) { accessToken, _, ex ->
                    if (accessToken != null) {
                        cont.resume(OAuthGateway.FreshToken(accessToken, state.jsonSerializeString()))
                    } else {
                        cont.resumeWithException(ex.toLinkedAccountException(cancelledIfUnknown = false))
                    }
                }
            }
        }

        override fun revocableToken(authState: String): String? =
            runCatching { AuthState.jsonDeserialize(authState) }.getOrNull()?.let { it.refreshToken ?: it.accessToken }

        private fun AuthorizationException?.toLinkedAccountException(cancelledIfUnknown: Boolean): LinkedAccountException =
            when {
                this == null -> {
                    if (cancelledIfUnknown) LinkedAccountException.Cancelled() else LinkedAccountException.Transient()
                }

                this == AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW ||
                    this == AuthorizationException.GeneralErrors.PROGRAM_CANCELED_AUTH_FLOW -> {
                    LinkedAccountException.Cancelled()
                }

                type == AuthorizationException.TYPE_GENERAL_ERROR && code == AuthorizationException.GeneralErrors.NETWORK_ERROR.code -> {
                    LinkedAccountException.Transient(this)
                }

                else -> {
                    LinkedAccountException.AuthorizationInvalid(this)
                }
            }
    }
