package io.github.aedev.flow.youtubesync.account

import kotlinx.serialization.Serializable

/** The YouTube channel of the Linked Account, as shown in settings. */
@Serializable
data class LinkedAccount(
    val channelId: String,
    val channelTitle: String,
)

/** Why linking or using the Linked Account failed. */
sealed class LinkedAccountException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** The build has no OAuth client id (see FORK.md). */
    class NotConfigured : LinkedAccountException("YouTube Sync OAuth client id is not configured")

    /** The viewer closed the browser or refused consent. */
    class Cancelled : LinkedAccountException("Authorization cancelled")

    /** Google refused the authorization or the refresh: the account must be linked again. */
    class AuthorizationInvalid(
        cause: Throwable? = null,
    ) : LinkedAccountException("Authorization is no longer valid", cause)

    /** No account is linked to this profile. */
    class NotLinked : LinkedAccountException("No YouTube account is linked")

    /** A network or server error that may go away on retry. */
    class Transient(
        cause: Throwable? = null,
    ) : LinkedAccountException("Temporary failure", cause)
}
