package io.github.aedev.flow.youtubesync.remote

/** A subscription on the Linked Account. [subscriptionId] is YouTube's id of the subscription itself, needed to remove it. */
data class RemoteSubscription(
    val subscriptionId: String,
    val channelId: String,
    val title: String,
    val thumbnailUrl: String,
)

/**
 * The Linked Account as the YouTube Sync sees it (spec: "Interfaccia YouTubeAccount"). The real one
 * calls the YouTube Data API through the official channel; tests use an in-memory one.
 * Errors are [YouTubeAccountException]s.
 */
interface YouTubeAccount {
    /** Every subscription of the account, all pages read. */
    suspend fun subscriptions(): List<RemoteSubscription>

    /** Subscribes to [channelId]. Already being subscribed counts as success. */
    suspend fun subscribe(channelId: String)

    /** Removes the subscription [subscriptionId]. Already gone counts as success. */
    suspend fun unsubscribe(subscriptionId: String)
}

/** Typed failures of [YouTubeAccount]. */
sealed class YouTubeAccountException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** The daily API quota is used up: retry after it resets. */
    class QuotaExceeded : YouTubeAccountException("YouTube Data API quota exceeded")

    /** The authorization was revoked or expired: the account must be linked again. */
    class AuthorizationInvalid(
        cause: Throwable? = null,
    ) : YouTubeAccountException("Authorization is no longer valid", cause)

    /** No account is linked to this profile. */
    class NotLinked : YouTubeAccountException("No YouTube account is linked")

    /** YouTube refused this one item (e.g. the channel no longer exists); other items can go on. */
    class ItemRejected(
        val reason: String,
    ) : YouTubeAccountException("YouTube refused the item: $reason")

    /** A network or server error that may go away on retry. */
    class Transient(
        cause: Throwable? = null,
    ) : YouTubeAccountException("Temporary failure", cause)
}
