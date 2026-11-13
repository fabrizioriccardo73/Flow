package io.github.aedev.flow.youtubesync.engine

import io.github.aedev.flow.youtubesync.local.LocalPlaylist
import io.github.aedev.flow.youtubesync.local.LocalPlaylistKind
import io.github.aedev.flow.youtubesync.local.LocalPlaylists
import io.github.aedev.flow.youtubesync.local.LocalVideo
import io.github.aedev.flow.youtubesync.remote.PlaylistKind
import io.github.aedev.flow.youtubesync.remote.RemotePlaylist
import io.github.aedev.flow.youtubesync.remote.RemotePlaylistItem
import io.github.aedev.flow.youtubesync.remote.YouTubeAccount
import io.github.aedev.flow.youtubesync.remote.YouTubeAccountException

/** A playlist that arrives in Flow from YouTube. */
data class PlaylistImport(
    val playlist: RemotePlaylist,
    val videos: List<LocalVideo>,
)

/** A playlist that goes away on one side because it went away on the other. */
data class PlaylistDeletion(
    val youTubeId: String,
    val title: String,
    /** The Flow playlist to delete; null when the deletion is on YouTube. */
    val localId: String? = null,
)

/**
 * One playlist on both sides and what the YouTube Sync does to it. [title] and [videos] are what both
 * sides hold after the run; the other fields say what each side must do to get there.
 */
data class PlaylistUpdate(
    val youTubeId: String,
    val localId: String,
    val kind: PlaylistKind,
    val title: String,
    val videos: List<LocalVideo>,
    val renameInFlow: Boolean = false,
    val renameOnYouTube: Boolean = false,
    val setVideosInFlow: Boolean = false,
    val addedToFlow: Int = 0,
    /** How many videos the playlist holds in Flow before the run. */
    val flowVideoCount: Int = 0,
    val removedFromFlow: List<String> = emptyList(),
    val addToYouTube: List<String> = emptyList(),
    val removeFromYouTube: List<RemotePlaylistItem> = emptyList(),
    /** Every entry on YouTube before the run, unavailable ones included: where the moves start from. */
    val youTubeItems: List<RemotePlaylistItem> = emptyList(),
    val reorderOnYouTube: Boolean = false,
) {
    val changesFlow: Boolean get() = renameInFlow || setVideosInFlow
    val changesYouTube: Boolean
        get() = renameOnYouTube || addToYouTube.isNotEmpty() || removeFromYouTube.isNotEmpty() || reorderOnYouTube
}

/** The playlist part of a [SyncPlan]. */
data class PlaylistPlan(
    val importToFlow: List<PlaylistImport> = emptyList(),
    val deleteFromFlow: List<PlaylistDeletion> = emptyList(),
    val deleteFromYouTube: List<PlaylistDeletion> = emptyList(),
    /** Every playlist on both sides, changed or not, so its baseline is recorded. */
    val updates: List<PlaylistUpdate> = emptyList(),
    /** Titles of playlists arriving from YouTube that a Flow-only playlist already uses: never merged. */
    val sameTitles: List<String> = emptyList(),
    /** Baseline entries of playlists gone from both sides, to forget. */
    val forget: Set<String> = emptySet(),
) {
    val changed: List<PlaylistUpdate> get() = updates.filter { it.changesFlow || it.changesYouTube }

    val isEmpty: Boolean
        get() = importToFlow.isEmpty() && deleteFromFlow.isEmpty() && deleteFromYouTube.isEmpty() && changed.isEmpty()
}

/** How applying the playlist part went. */
data class PlaylistResult(
    val imported: Int = 0,
    val updated: Int = 0,
    val deletedFromFlow: Int = 0,
    val deletedFromYouTube: Int = 0,
    val rejected: Int = 0,
    val stoppedBy: YouTubeAccountException? = null,
)

/**
 * The YouTube Sync of Owned Playlists and Saved Playlists (spec: "Motore di confronto a tre vie").
 *
 * For each playlist, Flow, YouTube and its Sync Baseline entry are compared:
 * - a playlist new on one side is carried to the other (from YouTube only: a playlist created in Flow
 *   stays Flow-only until the viewer links it, ticket 10);
 * - a playlist deleted on one side is deleted on the other, even if the other side changed it;
 * - videos added on either side are kept, videos removed on either side are removed;
 * - YouTube wins conflicting edits: a title changed on both sides, or an order changed on both sides;
 * - a playlist on both sides with no baseline entry (first link, or a playlist just linked) is a union.
 *
 * Saved Playlists only ever flow from YouTube to Flow. Deleted or private videos on YouTube are left
 * out of everything: never shown in Flow, never removed or moved on YouTube.
 */
internal class PlaylistSync(
    private val local: LocalPlaylists,
    private val remote: YouTubeAccount,
) {
    suspend fun plan(baseline: Map<String, PlaylistBaseline>): PlaylistPlan {
        val flow = local.all()
        val flowLinked = flow.filter { it.youTubeId != null && it.kind != LocalPlaylistKind.FLOW_ONLY }.associateBy { it.youTubeId!! }
        val youTube = remote.playlists().associateBy { it.playlistId }
        val imports = mutableListOf<PlaylistImport>()
        val deleteFromFlow = mutableListOf<PlaylistDeletion>()
        val deleteFromYouTube = mutableListOf<PlaylistDeletion>()
        val updates = mutableListOf<PlaylistUpdate>()
        val forget = mutableSetOf<String>()

        for (id in youTube.keys + flowLinked.keys + baseline.keys) {
            val y = youTube[id]
            val f = flowLinked[id]
            val b = baseline[id]
            when {
                y != null && f != null && y.kind == PlaylistKind.OWNED && f.kind == LocalPlaylistKind.SAVED -> {
                    // One of the viewer's own playlists that Flow only had bookmarked: it becomes an Owned Playlist.
                    imports += PlaylistImport(y, remote.playlistItems(id).representable().map { it.toLocalVideo() })
                }

                y != null && f != null -> {
                    updates += pair(f, y, remote.playlistItems(id), b)
                }

                y != null && b == null -> {
                    imports += PlaylistImport(y, remote.playlistItems(id).representable().map { it.toLocalVideo() })
                }

                y != null -> {
                    // Gone from Flow since the last run. A Saved Playlist cannot be removed from YouTube:
                    // it simply comes back, as YouTube decides what is saved.
                    if (y.kind == PlaylistKind.SAVED) {
                        imports += PlaylistImport(y, remote.playlistItems(id).representable().map { it.toLocalVideo() })
                    } else {
                        deleteFromYouTube += PlaylistDeletion(id, b!!.title)
                    }
                }

                f != null && b != null -> {
                    deleteFromFlow += PlaylistDeletion(id, f.title, f.localId)
                }

                f != null -> {
                    // Linked in Flow but unknown to the baseline and missing on YouTube: not ours to
                    // decide (another account's, or deleted before it was ever synced). Left alone.
                }

                else -> {
                    forget += id
                }
            }
        }

        val flowOnlyTitles = flow.filter { it.kind == LocalPlaylistKind.FLOW_ONLY }.map { it.title.normalized() }.toSet()
        return PlaylistPlan(
            importToFlow = imports,
            deleteFromFlow = deleteFromFlow,
            deleteFromYouTube = deleteFromYouTube,
            updates = updates,
            sameTitles = imports.map { it.playlist.title }.filter { it.normalized() in flowOnlyTitles },
            forget = forget,
        )
    }

    /** The guard's view of [plan]: whole playlists, and the videos of each playlist, per side. */
    fun massRemovals(
        plan: PlaylistPlan,
        flowCount: Int,
        youTubeCount: Int,
    ): List<MassRemoval> =
        listOfNotNull(
            RemovalGuard.check(SyncCollection.PLAYLISTS, SyncSide.FLOW, plan.deleteFromFlow.map { it.title }, flowCount),
            RemovalGuard.check(SyncCollection.PLAYLISTS, SyncSide.YOUTUBE, plan.deleteFromYouTube.map { it.title }, youTubeCount),
        ) +
            plan.updates.flatMap { u ->
                listOfNotNull(
                    RemovalGuard.check(SyncCollection.PLAYLIST_VIDEOS, SyncSide.FLOW, u.removedFromFlow, u.flowVideoCount, u.title),
                    RemovalGuard.check(
                        SyncCollection.PLAYLIST_VIDEOS,
                        SyncSide.YOUTUBE,
                        u.removeFromYouTube.map { it.title.ifBlank { it.videoId } },
                        u.youTubeItems.representable().size,
                        u.title,
                    ),
                )
            }

    private fun pair(
        f: LocalPlaylist,
        y: RemotePlaylist,
        items: List<RemotePlaylistItem>,
        b: PlaylistBaseline?,
    ): PlaylistUpdate {
        val available = items.representable()
        val yIds = available.map { it.videoId }
        val fIds = f.videos.map { it.videoId }.distinct()
        val metadata = f.videos.associateBy { it.videoId } + available.associate { it.videoId to it.toLocalVideo() }

        if (y.kind == PlaylistKind.SAVED || f.kind == LocalPlaylistKind.SAVED) {
            // Read-only in Flow: it simply follows YouTube.
            return PlaylistUpdate(
                youTubeId = y.playlistId,
                localId = f.localId,
                kind = PlaylistKind.SAVED,
                title = y.title,
                videos = yIds.map(metadata::getValue),
                renameInFlow = f.title != y.title,
                setVideosInFlow = fIds != yIds,
                addedToFlow = (yIds - fIds.toSet()).size,
                flowVideoCount = f.videos.size,
                removedFromFlow = f.videos.filter { it.videoId !in yIds }.map { it.title.ifBlank { it.videoId } },
            )
        }

        val merged = mergeVideos(fIds, yIds, b?.videos)
        val title =
            when {
                b == null || y.title != b.title -> y.title
                else -> f.title
            }
        val keep = merged.toSet()
        val removeOnYouTube = available.filter { it.videoId !in keep }
        val add = merged.filter { it !in yIds }
        val afterEdits = yIds.filter { it in keep } + add
        return PlaylistUpdate(
            youTubeId = y.playlistId,
            localId = f.localId,
            kind = PlaylistKind.OWNED,
            title = title,
            videos = merged.map(metadata::getValue),
            renameInFlow = f.title != title,
            renameOnYouTube = y.title != title,
            setVideosInFlow = fIds != merged || f.videos.size != fIds.size,
            addedToFlow = merged.count { it !in fIds },
            flowVideoCount = f.videos.size,
            removedFromFlow = f.videos.filter { it.videoId !in keep }.map { it.title.ifBlank { it.videoId } },
            addToYouTube = add,
            removeFromYouTube = removeOnYouTube,
            youTubeItems = items,
            reorderOnYouTube = afterEdits != merged,
        )
    }

    suspend fun apply(
        plan: PlaylistPlan,
        baseline: Map<String, PlaylistBaseline>,
        stoppedAlready: YouTubeAccountException?,
    ): Pair<PlaylistResult, Map<String, PlaylistBaseline>> {
        val next = baseline.toMutableMap()
        next -= plan.forget
        var result = PlaylistResult()

        // Flow first: local writes cannot run out of quota.
        for (import in plan.importToFlow) {
            val kind = if (import.playlist.kind == PlaylistKind.SAVED) LocalPlaylistKind.SAVED else LocalPlaylistKind.OWNED
            local.import(import.playlist.playlistId, import.playlist.title, kind, import.videos)
            next[import.playlist.playlistId] =
                PlaylistBaseline(import.playlist.title, import.videos.map { it.videoId }, saved = kind == LocalPlaylistKind.SAVED)
            result = result.copy(imported = result.imported + 1)
        }
        for (deletion in plan.deleteFromFlow) {
            deletion.localId?.let { local.delete(it) }
            next -= deletion.youTubeId
            result = result.copy(deletedFromFlow = result.deletedFromFlow + 1)
        }
        for (update in plan.updates) {
            if (update.renameInFlow) local.rename(update.localId, update.title)
            if (update.setVideosInFlow) local.setVideos(update.localId, update.videos)
        }

        var stoppedBy = stoppedAlready
        var rejected = 0
        if (stoppedBy == null) {
            for (deletion in plan.deleteFromYouTube) {
                try {
                    remote.deletePlaylist(deletion.youTubeId)
                    next -= deletion.youTubeId
                    result = result.copy(deletedFromYouTube = result.deletedFromYouTube + 1)
                } catch (e: YouTubeAccountException.ItemRejected) {
                    rejected++
                } catch (e: YouTubeAccountException) {
                    stoppedBy = e
                    break
                }
            }
        }
        for (update in plan.updates) {
            val done =
                when {
                    !update.changesYouTube -> {
                        true
                    }

                    stoppedBy != null -> {
                        false
                    }

                    else -> {
                        try {
                            pushToYouTube(update)
                        } catch (e: YouTubeAccountException.ItemRejected) {
                            rejected++
                            false
                        } catch (e: YouTubeAccountException) {
                            stoppedBy = e
                            false
                        }
                    }
                }
            // A playlist whose YouTube part did not complete keeps its old entry, so it is planned again.
            if (done) {
                next[update.youTubeId] =
                    PlaylistBaseline(update.title, update.videos.map { it.videoId }, saved = update.kind == PlaylistKind.SAVED)
            }
            if (update.changesFlow || (done && update.changesYouTube)) result = result.copy(updated = result.updated + 1)
        }
        return result.copy(rejected = rejected, stoppedBy = stoppedBy) to next
    }

    /** Title, removals, additions, then the fewest moves that give YouTube the agreed order. */
    private suspend fun pushToYouTube(update: PlaylistUpdate): Boolean {
        val id = update.youTubeId
        if (update.renameOnYouTube) remote.renamePlaylist(id, update.title)
        var complete = true
        val removedItems = mutableSetOf<String>()
        for (item in update.removeFromYouTube) {
            try {
                remote.removePlaylistItem(item.itemId)
                removedItems += item.itemId
            } catch (e: YouTubeAccountException.ItemRejected) {
                complete = false
            }
        }
        val current = update.youTubeItems.filter { it.itemId !in removedItems }.toMutableList()
        for (videoId in update.addToYouTube) {
            try {
                current += RemotePlaylistItem(remote.addPlaylistItem(id, videoId), videoId)
            } catch (e: YouTubeAccountException.ItemRejected) {
                complete = false
            }
        }
        if (update.reorderOnYouTube) {
            // Only the first entry of each available video takes part; the rest stay where they are.
            val movable = current.filter { it.available }.distinctBy { it.videoId }.associateBy { it.videoId }
            val target = update.videos.map { it.videoId }.filter { it in movable }
            val keys = current.map { item -> item.itemId }
            for ((itemId, position) in reorderMoves(keys, target.map { movable.getValue(it).itemId })) {
                val item = current.first { it.itemId == itemId }
                remote.movePlaylistItem(id, itemId, item.videoId, position)
            }
        }
        if (!complete) throw YouTubeAccountException.ItemRejected("playlistItem")
        return true
    }
}

/**
 * Three-way merge of a playlist's videos. Without a [base] (first time the playlist is on both sides)
 * it is a union in YouTube's order. Otherwise a video removed on either side is removed, a video added
 * on either side is kept, and the order of the side that changed it wins, YouTube's when both did.
 */
internal fun mergeVideos(
    flow: List<String>,
    youTube: List<String>,
    base: List<String>?,
): List<String> {
    if (base == null) return youTube + flow.filter { it !in youTube }
    val baseSet = base.toSet()
    val removed = (baseSet - flow.toSet()) + (baseSet - youTube.toSet())
    val keep = (flow + youTube).filter { it !in removed }.toSet()
    val youTubeReordered = youTube.filter { it in baseSet } != base.filter { it in youTube }
    val flowReordered = flow.filter { it in baseSet } != base.filter { it in flow }
    val (first, second) = if (youTubeReordered || !flowReordered) youTube to flow else flow to youTube
    val ordered = first.filter { it in keep }
    return ordered + second.filter { it in keep && it !in ordered }
}

/**
 * The fewest single-entry moves that turn [current] into [target] (a sub-sequence of entries that
 * must end up in that order; other entries, such as unavailable videos, stay put). Each move is an
 * entry and the 0-based position to move it to, applied in order.
 */
internal fun reorderMoves(
    current: List<String>,
    target: List<String>,
): List<Pair<String, Int>> {
    val rank = target.withIndex().associate { it.value to it.index }
    val sequence = current.filter { it in rank }
    val kept = longestIncreasing(sequence) { rank.getValue(it) }.toSet()
    val list = current.toMutableList()
    val moves = mutableListOf<Pair<String, Int>>()
    target.forEachIndexed { index, key ->
        if (key in kept) return@forEachIndexed
        list.remove(key)
        val position =
            if (index == 0) {
                list.indexOfFirst { it in rank }.takeIf { it >= 0 } ?: 0
            } else {
                list.indexOf(target[index - 1]) + 1
            }
        list.add(position, key)
        moves += key to position
    }
    return moves
}

/** The longest sub-sequence of [items] whose [key]s increase (patience sorting, O(n log n)). */
private fun <T> longestIncreasing(
    items: List<T>,
    key: (T) -> Int,
): List<T> {
    if (items.isEmpty()) return emptyList()
    val tails = mutableListOf<Int>() // index into items of the smallest tail of each length
    val previous = IntArray(items.size) { -1 }
    for (i in items.indices) {
        var lo = 0
        var hi = tails.size
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (key(items[tails[mid]]) < key(items[i])) lo = mid + 1 else hi = mid
        }
        if (lo > 0) previous[i] = tails[lo - 1]
        if (lo == tails.size) tails += i else tails[lo] = i
    }
    val result = ArrayDeque<T>()
    var i = tails.last()
    while (i >= 0) {
        result.addFirst(items[i])
        i = previous[i]
    }
    return result.toList()
}

private fun List<RemotePlaylistItem>.representable(): List<RemotePlaylistItem> = filter { it.available }.distinctBy { it.videoId }

private fun RemotePlaylistItem.toLocalVideo() = LocalVideo(videoId, title, channelTitle, channelId, thumbnailUrl)

private fun String.normalized() = trim().lowercase()
