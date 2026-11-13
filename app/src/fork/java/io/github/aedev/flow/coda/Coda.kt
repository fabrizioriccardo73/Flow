package io.github.aedev.flow.coda

import io.github.aedev.flow.data.model.Video

/** A video taken out of the Coda by hand, with its place, so an undo can put it back. */
data class RemovedFromCoda(
    val video: Video,
    val index: Int,
    val wasPlaying: Boolean,
)

/**
 * The Coda as it stands: the videos the viewer chose to watch soon, in order, and the one in
 * riproduzione, kept at the top until it is done with.
 *
 * A video is done with when it ends, is skipped or is marked as watched; interrupting it keeps it.
 * Each video appears once.
 */
data class Coda(
    val videos: List<Video> = emptyList(),
    val playingId: String? = null,
) {
    val isEmpty: Boolean
        get() = videos.isEmpty()

    operator fun contains(videoId: String): Boolean = videos.any { it.id == videoId }

    /** Adds [video] at the end; one already in the Coda stays where it is. */
    fun append(video: Video): Coda = if (video.id in this) this else copy(videos = videos + video)

    /** Puts [video] first, right after the video in riproduzione; one already in the Coda moves there. */
    fun putFirst(video: Video): Coda {
        val rest = videos.filterNot { it.id == video.id }
        val at = if (playingId != null && rest.firstOrNull()?.id == playingId) 1 else 0
        return copy(videos = rest.toMutableList().apply { add(at, video) })
    }

    /** [videoId] started playing: it moves to the top, in riproduzione. A video not in the Coda changes nothing. */
    fun markPlaying(videoId: String): Coda {
        val video = videos.firstOrNull { it.id == videoId } ?: return this
        return Coda(listOf(video) + videos.filterNot { it.id == videoId }, playingId = videoId)
    }

    /** [videoId] is done with: it leaves the Coda. */
    fun finish(videoId: String): Coda =
        if (videoId !in this) {
            this
        } else {
            Coda(videos.filterNot { it.id == videoId }, playingId.takeUnless { it == videoId })
        }

    /** Moves the video at [from] to [to]; out-of-range positions change nothing. */
    fun move(
        from: Int,
        to: Int,
    ): Coda {
        if (from !in videos.indices || to !in videos.indices || from == to) return this
        return copy(videos = videos.toMutableList().apply { add(to, removeAt(from)) })
    }

    /** Takes [videoId] out by hand, with what it takes to put it back; null when it is not in the Coda. */
    fun remove(videoId: String): Pair<Coda, RemovedFromCoda>? {
        val index = videos.indexOfFirst { it.id == videoId }.takeIf { it >= 0 } ?: return null
        val removed = RemovedFromCoda(videos[index], index, wasPlaying = playingId == videoId)
        return finish(videoId) to removed
    }

    /** Puts [removed] back where it was, unless it has been added again since. */
    fun restore(removed: RemovedFromCoda): Coda {
        if (removed.video.id in this) return this
        val restored = videos.toMutableList().apply { add(removed.index.coerceIn(0, size), removed.video) }
        return Coda(restored, if (removed.wasPlaying && playingId == null) removed.video.id else playingId)
    }

    /** What the Coda plays once [currentVideoId] is over. */
    fun nextAfter(currentVideoId: String?): Video? = videos.firstOrNull { it.id != currentVideoId }

    /** This Coda with [other]'s videos added after its own, as when a saved Coda loads after early additions. */
    fun mergedWith(other: Coda): Coda =
        Coda(
            videos = videos + other.videos.filterNot { it.id in this },
            playingId = other.playingId ?: playingId,
        )
}
