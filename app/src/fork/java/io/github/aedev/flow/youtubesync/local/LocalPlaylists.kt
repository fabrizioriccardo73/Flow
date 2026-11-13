package io.github.aedev.flow.youtubesync.local

import io.github.aedev.flow.data.local.PlaylistRepository
import io.github.aedev.flow.data.local.dao.PlaylistDao
import io.github.aedev.flow.data.local.dao.VideoDao
import io.github.aedev.flow.data.local.entity.PlaylistEntity
import io.github.aedev.flow.data.local.entity.PlaylistVideoCrossRef
import io.github.aedev.flow.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** What a playlist in Flow is to the YouTube Sync (glossary: Owned Playlist, Saved Playlist, Flow-only). */
enum class LocalPlaylistKind { OWNED, SAVED, FLOW_ONLY }

/** A video in a Flow playlist, with what Flow needs to show it. */
data class LocalVideo(
    val videoId: String,
    val title: String = "",
    val channelName: String = "",
    val channelId: String = "",
    val thumbnailUrl: String = "",
)

/**
 * A playlist in Flow. [youTubeId] is the id of the playlist on YouTube for an Owned Playlist or a Saved
 * Playlist, null for a Flow-only one. [videos] are in Flow's order.
 */
data class LocalPlaylist(
    val localId: String,
    val youTubeId: String?,
    val title: String,
    val kind: LocalPlaylistKind,
    val videos: List<LocalVideo>,
)

/**
 * Flow's playlists as the YouTube Sync sees them (ADR 0002). Reads and writes go through Flow's own
 * database, so the Device Sync carries every change to the other devices by itself.
 *
 * How the link is stored (ADR 0003): a playlist from YouTube keeps the YouTube id as its own id (or,
 * after a Device Sync restore, as its sync id), so the link travels with the playlist to every device.
 * An Owned Playlist is one of the viewer's own playlists ("user created"); a Saved Playlist is a
 * saved one, which Flow already shows read-only.
 */
interface LocalPlaylists {
    /** Every playlist but Flow's built-in ones (Watch Later, Saved Shorts). */
    suspend fun all(): List<LocalPlaylist>

    /** Adds a playlist that came from YouTube, keyed by its YouTube id, and returns its local id. */
    suspend fun import(
        youTubeId: String,
        title: String,
        kind: LocalPlaylistKind,
        videos: List<LocalVideo>,
    ): String

    /** Makes [localId] hold exactly [videos], in that order. */
    suspend fun setVideos(
        localId: String,
        videos: List<LocalVideo>,
    )

    suspend fun rename(
        localId: String,
        title: String,
    )

    suspend fun delete(localId: String)

    /**
     * Gives the playlist [localId] a new identity, keeping its title and videos: linked to [youTubeId]
     * as [kind], or Flow-only when [youTubeId] is null. Returns the new local id.
     */
    suspend fun relink(
        localId: String,
        youTubeId: String?,
        kind: LocalPlaylistKind,
    ): String
}

/** Recognises the id of a YouTube playlist, as opposed to Flow's own playlist ids (numbers, "sync_<uuid>"). */
object YouTubePlaylistId {
    private val pattern = Regex("^(PL|OL|FL|UU|LL|RD)[A-Za-z0-9_-]{10,}$")

    fun matches(id: String?): Boolean = id != null && pattern.matches(id)
}

class FlowLocalPlaylists
    @Inject
    constructor(
        private val playlistDao: PlaylistDao,
        private val videoDao: VideoDao,
    ) : LocalPlaylists {
        override suspend fun all(): List<LocalPlaylist> {
            val refs = playlistDao.getAllPlaylistVideoCrossRefs().groupBy { it.playlistId }
            val playlists = playlistDao.getAllPlaylists().first().filter { it.id !in BUILT_IN }
            val videos = playlists.flatMap { p -> playlistDao.getVideosForPlaylist(p.id).first() }.associateBy { it.id }
            return playlists.map { p ->
                val key = p.syncId ?: p.id
                val youTubeId = key.takeIf(YouTubePlaylistId::matches)
                LocalPlaylist(
                    localId = p.id,
                    youTubeId = youTubeId,
                    title = p.name,
                    kind =
                        when {
                            youTubeId == null -> LocalPlaylistKind.FLOW_ONLY
                            p.isUserCreated -> LocalPlaylistKind.OWNED
                            else -> LocalPlaylistKind.SAVED
                        },
                    videos =
                        (refs[p.id] ?: emptyList())
                            .sortedBy { it.position }
                            .map { ref -> videos[ref.videoId].toLocalVideo(ref.videoId) },
                )
            }
        }

        override suspend fun import(
            youTubeId: String,
            title: String,
            kind: LocalPlaylistKind,
            videos: List<LocalVideo>,
        ): String {
            videoDao.insertVideosOrIgnore(videos.map { it.toEntity() })
            val now = System.currentTimeMillis()
            playlistDao.insertPlaylistWithVideos(
                playlist =
                    PlaylistEntity(
                        id = youTubeId,
                        name = title,
                        description = "",
                        thumbnailUrl = videos.firstOrNull()?.thumbnailUrl.orEmpty(),
                        isPrivate = true,
                        createdAt = now,
                        isUserCreated = kind != LocalPlaylistKind.SAVED,
                    ),
                entries =
                    videos.mapIndexed { index, video ->
                        PlaylistVideoCrossRef(playlistId = youTubeId, videoId = video.videoId, position = index.toLong(), addedAt = now)
                    },
            )
            // A playlist Flow already had under this id (a bookmark of it) now holds exactly YouTube's videos.
            playlistDao.replacePlaylistVideos(youTubeId, videos.map { it.videoId })
            return youTubeId
        }

        override suspend fun setVideos(
            localId: String,
            videos: List<LocalVideo>,
        ) {
            videoDao.insertVideosOrIgnore(videos.map { it.toEntity() })
            playlistDao.replacePlaylistVideos(localId, videos.map { it.videoId })
        }

        override suspend fun rename(
            localId: String,
            title: String,
        ) = playlistDao.updatePlaylistName(localId, title)

        override suspend fun delete(localId: String) = playlistDao.deletePlaylist(localId)

        override suspend fun relink(
            localId: String,
            youTubeId: String?,
            kind: LocalPlaylistKind,
        ): String {
            val old = playlistDao.getPlaylist(localId) ?: return localId
            val refs = playlistDao.getAllPlaylistVideoCrossRefs().filter { it.playlistId == localId }
            val newId = youTubeId ?: System.currentTimeMillis().toString()
            playlistDao.insertPlaylistWithVideos(
                playlist =
                    old.copy(
                        id = newId,
                        syncId = null,
                        isUserCreated = kind != LocalPlaylistKind.SAVED,
                    ),
                entries = refs.map { it.copy(playlistId = newId) },
            )
            if (newId != localId) playlistDao.deletePlaylist(localId)
            return newId
        }

        private companion object {
            val BUILT_IN = setOf(PlaylistRepository.WATCH_LATER_ID, PlaylistRepository.SAVED_SHORTS_ID)
        }
    }

private fun VideoEntity?.toLocalVideo(videoId: String) =
    LocalVideo(
        videoId = videoId,
        title = this?.title.orEmpty(),
        channelName = this?.channelName.orEmpty(),
        channelId = this?.channelId.orEmpty(),
        thumbnailUrl = this?.thumbnailUrl.orEmpty(),
    )

private fun LocalVideo.toEntity() =
    VideoEntity(
        id = videoId,
        title = title,
        channelName = channelName,
        channelId = channelId,
        thumbnailUrl = thumbnailUrl,
        duration = 0,
        viewCount = 0L,
        uploadDate = "",
        description = "",
        channelThumbnailUrl = "",
    )
