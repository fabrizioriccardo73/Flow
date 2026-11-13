package io.github.aedev.flow.ui.components.shared.quickactions

import androidx.annotation.StringRes
import io.github.aedev.flow.coda.Coda // fork: Coda
import io.github.aedev.flow.coda.RemovedFromCoda // fork: Coda
import io.github.aedev.flow.data.local.LikedVideoInfo
import io.github.aedev.flow.data.local.entity.PlaylistVideoCrossRef
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.player.EnhancedMusicPlayerManager
import io.github.aedev.flow.player.RemovedQueueEntry

/**
 * A confirmation for the app's snackbar: [text] formatted with [arg], or [plainText] already
 * resolved by a screen, and an [undo] when the action has one.
 */
data class QuickActionMessage(
    @StringRes val text: Int = 0,
    val arg: String? = null,
    val undo: QuickActionUndo? = null,
    val plainText: String? = null,
    val play: Video? = null, // fork: Coda, offers to play this video instead of an undo
)

/** How to put an action back. Each carries the state to restore, not the state that was set. */
sealed interface QuickActionUndo {
    data class WatchLater(
        val video: Video,
        val saved: Boolean,
    ) : QuickActionUndo

    data class Subscription(
        val channelId: String,
        val channelName: String,
        val channelThumbnail: String,
        val subscribed: Boolean,
    ) : QuickActionUndo

    data class ChannelBlock(
        val channelId: String,
    ) : QuickActionUndo

    data class PlaylistRemoval(
        val entries: List<PlaylistVideoCrossRef>,
    ) : QuickActionUndo

    /** Likes taken off the Liked videos or Liked music page. */
    data class Unlike(
        val likes: List<LikedVideoInfo>,
    ) : QuickActionUndo

    /** Files moved to the system trash; putting them back needs the system's consent, asked by the host. */
    data class RestoreFromTrash(
        val contentUris: List<String>,
    ) : QuickActionUndo

    /** A video swiped out of the player's queue. */
    data class QueueRemoval(
        val entry: RemovedQueueEntry,
    ) : QuickActionUndo

    /** A song swiped out of the music queue. */
    data class MusicQueueRemoval(
        val removed: EnhancedMusicPlayerManager.RemovedQueueTrack,
    ) : QuickActionUndo

    /** A video taken out of the Coda by hand (fork: Coda). */
    data class CodaRemoval(
        val removed: RemovedFromCoda,
    ) : QuickActionUndo

    /** The Coda emptied with Clear (fork: Coda). */
    data class CodaCleared(
        val before: Coda,
    ) : QuickActionUndo

    /** A download queued because the viewer opened the video. */
    data class AutoDownload(
        val videoId: String,
    ) : QuickActionUndo
}
