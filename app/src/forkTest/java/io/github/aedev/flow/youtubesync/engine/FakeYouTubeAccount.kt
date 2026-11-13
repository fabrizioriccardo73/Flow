package io.github.aedev.flow.youtubesync.engine

import io.github.aedev.flow.youtubesync.local.LocalPlaylist
import io.github.aedev.flow.youtubesync.local.LocalPlaylistKind
import io.github.aedev.flow.youtubesync.local.LocalPlaylists
import io.github.aedev.flow.youtubesync.local.LocalSubscription
import io.github.aedev.flow.youtubesync.local.LocalSubscriptions
import io.github.aedev.flow.youtubesync.local.LocalVideo
import io.github.aedev.flow.youtubesync.remote.PlaylistKind
import io.github.aedev.flow.youtubesync.remote.RemotePlaylist
import io.github.aedev.flow.youtubesync.remote.RemotePlaylistItem
import io.github.aedev.flow.youtubesync.remote.RemoteSubscription
import io.github.aedev.flow.youtubesync.remote.YouTubeAccount
import io.github.aedev.flow.youtubesync.remote.YouTubeAccountException

/** An in-memory Linked Account. [failOn] makes subscribing to a channel throw. */
class FakeYouTubeAccount(
    vararg channels: String,
) : YouTubeAccount {
    val subscribed = channels.associateWith { "sub-$it" }.toMutableMap()
    val failOn = mutableMapOf<String, YouTubeAccountException>()
    var subscribeCalls = 0

    val channelIds: Set<String> get() = subscribed.keys

    override suspend fun subscriptions() =
        subscribed.map { (channel, id) -> RemoteSubscription(id, channel, "Title $channel", "thumb-$channel") }

    override suspend fun subscribe(channelId: String) {
        subscribeCalls++
        failOn[channelId]?.let { throw it }
        subscribed.getOrPut(channelId) { "sub-$channelId" }
    }

    override suspend fun unsubscribe(subscriptionId: String) {
        subscribed.entries.removeAll { it.value == subscriptionId }
    }

    /** A playlist on the fake account: [items] in order, unavailable entries included. */
    class Playlist(
        var title: String,
        val kind: PlaylistKind = PlaylistKind.OWNED,
        val items: MutableList<RemotePlaylistItem> = mutableListOf(),
        var description: String = "",
    ) {
        val videoIds: List<String> get() = items.filter { it.available }.map { it.videoId }
    }

    val playlists = linkedMapOf<String, Playlist>()

    /** Every write call, in order ("create", "rename", "delete", "add", "remove", "move"). */
    val writes = mutableListOf<String>()

    /** Makes the next write of this kind (e.g. "add") throw. */
    val failWrite = mutableMapOf<String, YouTubeAccountException>()
    private var nextId = 0

    fun playlist(
        id: String,
        title: String,
        vararg videos: String,
        kind: PlaylistKind = PlaylistKind.OWNED,
    ) {
        playlists[id] = Playlist(title, kind, videos.map { item(it) }.toMutableList())
    }

    fun item(
        videoId: String,
        available: Boolean = true,
    ) = RemotePlaylistItem("item-${nextId++}", videoId, "Video $videoId", "Channel", "ch", "thumb-$videoId", available)

    private fun write(kind: String) {
        writes += kind
        failWrite.remove(kind)?.let { throw it }
    }

    override suspend fun playlists() = playlists.map { (id, p) -> RemotePlaylist(id, p.title, p.kind) }

    override suspend fun playlistItems(playlistId: String) =
        playlists[playlistId]?.items?.toList() ?: throw YouTubeAccountException.ItemRejected("playlistNotFound")

    override suspend fun createPlaylist(title: String): String {
        write("create")
        val id = "PLcreated000000${nextId++}"
        playlists[id] = Playlist(title)
        return id
    }

    override suspend fun renamePlaylist(
        playlistId: String,
        title: String,
    ) {
        write("rename")
        playlists.getValue(playlistId).title = title
    }

    override suspend fun deletePlaylist(playlistId: String) {
        write("delete")
        playlists.remove(playlistId)
    }

    override suspend fun addPlaylistItem(
        playlistId: String,
        videoId: String,
    ): String {
        write("add")
        val item = item(videoId)
        playlists.getValue(playlistId).items += item
        return item.itemId
    }

    override suspend fun removePlaylistItem(itemId: String) {
        write("remove")
        playlists.values.forEach { p -> p.items.removeAll { it.itemId == itemId } }
    }

    override suspend fun movePlaylistItem(
        playlistId: String,
        itemId: String,
        videoId: String,
        position: Int,
    ) {
        write("move")
        val items = playlists.getValue(playlistId).items
        val item = items.first { it.itemId == itemId }
        items.remove(item)
        items.add(position, item)
    }
}

/** Flow's playlists, in memory, with the same identity rules as the real adapter. */
class FakeLocalPlaylists : LocalPlaylists {
    val playlists = linkedMapOf<String, LocalPlaylist>()
    private var nextId = 0

    fun flowOnly(
        localId: String,
        title: String,
        vararg videos: String,
    ) {
        playlists[localId] = LocalPlaylist(localId, null, title, LocalPlaylistKind.FLOW_ONLY, videos.map(::video))
    }

    fun byYouTubeId(id: String): LocalPlaylist? = playlists.values.firstOrNull { it.youTubeId == id }

    fun videoIds(youTubeId: String): List<String>? = byYouTubeId(youTubeId)?.videos?.map { it.videoId }

    /** What the viewer does in Flow's UI. */
    fun edit(
        youTubeId: String,
        change: (LocalPlaylist) -> LocalPlaylist,
    ) {
        val p = byYouTubeId(youTubeId)!!
        playlists[p.localId] = change(p)
    }

    override suspend fun all() = playlists.values.toList()

    override suspend fun import(
        youTubeId: String,
        title: String,
        kind: LocalPlaylistKind,
        videos: List<LocalVideo>,
    ): String {
        playlists[youTubeId] = LocalPlaylist(youTubeId, youTubeId, title, kind, videos)
        return youTubeId
    }

    override suspend fun setVideos(
        localId: String,
        videos: List<LocalVideo>,
    ) {
        playlists[localId] = playlists.getValue(localId).copy(videos = videos)
    }

    override suspend fun rename(
        localId: String,
        title: String,
    ) {
        playlists[localId] = playlists.getValue(localId).copy(title = title)
    }

    override suspend fun delete(localId: String) {
        playlists.remove(localId)
    }

    override suspend fun relink(
        localId: String,
        youTubeId: String?,
        kind: LocalPlaylistKind,
    ): String {
        val old = playlists.remove(localId)!!
        val newId = youTubeId ?: "local-${nextId++}"
        playlists[newId] = old.copy(localId = newId, youTubeId = youTubeId, kind = kind)
        return newId
    }
}

fun video(id: String) = LocalVideo(id, "Video $id", "Channel", "ch", "thumb-$id")

/** Flow's subscriptions, in memory. */
class FakeLocalSubscriptions(
    vararg channels: String,
) : LocalSubscriptions {
    val subscriptions = channels.associateWith { LocalSubscription(it, "Local $it", "local-thumb-$it") }.toMutableMap()

    val channelIds: Set<String> get() = subscriptions.keys

    override suspend fun all() = subscriptions.values.toList()

    override suspend fun add(subscriptions: Collection<LocalSubscription>) {
        subscriptions.forEach { this.subscriptions[it.channelId] = it }
    }

    override suspend fun remove(channelIds: Collection<String>) {
        channelIds.forEach { subscriptions.remove(it) }
    }
}
