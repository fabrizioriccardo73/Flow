package io.github.aedev.flow.youtubesync.remote

/** A subscription on the Linked Account. [subscriptionId] is YouTube's id of the subscription itself, needed to remove it. */
data class RemoteSubscription(
    val subscriptionId: String,
    val channelId: String,
    val title: String,
    val thumbnailUrl: String,
)

/** Whose playlist it is (glossary: Owned Playlist, Saved Playlist). */
enum class PlaylistKind { OWNED, SAVED }

/** A playlist of the Linked Account: one it owns, or one of somebody else it saved. */
data class RemotePlaylist(
    val playlistId: String,
    val title: String,
    val kind: PlaylistKind,
    val thumbnailUrl: String = "",
)

/**
 * One entry of a playlist on YouTube. [itemId] is YouTube's id of the entry, needed to move or remove it.
 * [available] is false for a deleted or private video: the YouTube Sync never shows or touches it.
 */
data class RemotePlaylistItem(
    val itemId: String,
    val videoId: String,
    val title: String = "",
    val channelTitle: String = "",
    val channelId: String = "",
    val thumbnailUrl: String = "",
    val available: Boolean = true,
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

    /**
     * The account's Owned Playlists and Saved Playlists, all pages read. The official channel can only
     * list owned ones: the YouTube Data API has no way to read the playlists saved to the library.
     */
    suspend fun playlists(): List<RemotePlaylist>

    /** Every entry of [playlistId] in order, unavailable videos included (marked), all pages read. */
    suspend fun playlistItems(playlistId: String): List<RemotePlaylistItem>

    /** Creates a private playlist titled [title] and returns its id. */
    suspend fun createPlaylist(title: String): String

    /** Changes only the title of [playlistId]: description and visibility stay as they are. */
    suspend fun renamePlaylist(
        playlistId: String,
        title: String,
    )

    /** Deletes [playlistId]. Already gone counts as success. */
    suspend fun deletePlaylist(playlistId: String)

    /** Appends [videoId] to [playlistId] and returns the new entry's id. */
    suspend fun addPlaylistItem(
        playlistId: String,
        videoId: String,
    ): String

    /** Removes the entry [itemId]. Already gone counts as success. */
    suspend fun removePlaylistItem(itemId: String)

    /** Moves the entry [itemId] (holding [videoId]) of [playlistId] to the 0-based [position]. */
    suspend fun movePlaylistItem(
        playlistId: String,
        itemId: String,
        videoId: String,
        position: Int,
    )
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
