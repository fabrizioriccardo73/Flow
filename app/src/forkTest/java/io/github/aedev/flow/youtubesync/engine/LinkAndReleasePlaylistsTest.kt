package io.github.aedev.flow.youtubesync.engine

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.youtubesync.local.LocalPlaylistKind
import io.github.aedev.flow.youtubesync.remote.PlaylistKind
import io.github.aedev.flow.youtubesync.remote.YouTubeAccountException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

/** Tickets 10 and 11, through the seam: linking a Flow-only playlist, and unlinking everything. */
class LinkAndReleasePlaylistsTest : SyncFixture() {
    @After
    fun tearDown() = close()

    @Test
    fun `linking a Flow-only playlist creates a new private playlist with its videos and order`() =
        runBlocking<Unit> {
            youTube.playlist("PLowned0000001", "Road trip", "z")
            playlists.flowOnly("1700000000000", "Road trip", "c", "a", "b")
            syncNow()

            val youTubeId = sync.linkPlaylist("1700000000000")
            syncNow()

            assertThat(youTubeId).isNotEqualTo("PLowned0000001")
            assertThat(youTube.playlists.getValue(youTubeId).videoIds).containsExactly("c", "a", "b").inOrder()
            assertThat(youTube.playlists.getValue(youTubeId).title).isEqualTo("Road trip")
            val linked = playlists.byYouTubeId(youTubeId)!!
            assertThat(linked.kind).isEqualTo(LocalPlaylistKind.OWNED)
            assertThat(linked.videos.map { it.videoId }).containsExactly("c", "a", "b").inOrder()
            assertThat(youTube.playlists.getValue("PLowned0000001").videoIds).containsExactly("z")
        }

    @Test
    fun `an interrupted link is completed by the next YouTube Sync without a second playlist`() =
        runBlocking<Unit> {
            playlists.flowOnly("1700000000000", "Mix", "a", "b")
            syncNow()

            val youTubeId = sync.linkPlaylist("1700000000000")
            youTube.failWrite["add"] = YouTubeAccountException.Transient()
            syncNow()
            syncNow()

            assertThat(youTube.playlists.keys).containsExactly(youTubeId)
            assertThat(youTube.playlists.getValue(youTubeId).videoIds).containsExactly("a", "b").inOrder()
            assertThat(youTube.writes.count { it == "create" }).isEqualTo(1)
        }

    @Test
    fun `only Flow-only playlists can be linked`() =
        runBlocking<Unit> {
            youTube.playlist("PLowned0000001", "Mine", "a")
            syncNow()
            val owned = playlists.byYouTubeId("PLowned0000001")!!.localId

            assertThat(runCatching { sync.linkPlaylist(owned) }.isFailure).isTrue()
        }

    @Test
    fun `unlinking turns Owned and Saved Playlists into Flow-only ones and writes nothing to YouTube`() =
        runBlocking<Unit> {
            youTube.playlist("PLowned0000001", "Mine", "a", "b")
            youTube.playlist("PLsaved0000001", "Theirs", "c", kind = PlaylistKind.SAVED)
            playlists.flowOnly("1700000000000", "Local", "d")
            syncNow()
            youTube.writes.clear()

            sync.releaseAll()

            assertThat(youTube.writes).isEmpty()
            assertThat(
                playlists.playlists.values
                    .map { it.kind }
                    .toSet(),
            ).containsExactly(LocalPlaylistKind.FLOW_ONLY)
            assertThat(playlists.playlists.values.map { it.title }).containsExactly("Mine", "Theirs", "Local")
            assertThat(
                playlists.playlists.values
                    .first { it.title == "Mine" }
                    .videos
                    .map { it.videoId },
            ).containsExactly("a", "b")
                .inOrder()
            assertThat(baselines.read()).isNull()
            assertThat(youTube.playlists.keys).containsExactly("PLowned0000001", "PLsaved0000001")
        }

    @Test
    fun `linking again after an unlink starts over like a first link`() =
        runBlocking<Unit> {
            youTube.playlist("PLowned0000001", "Mine", "a")
            syncNow()
            sync.releaseAll()

            val plan = sync.plan()

            assertThat(plan.isFirstLink).isTrue()
            assertThat(plan.playlists.importToFlow.map { it.playlist.title }).containsExactly("Mine")
            assertThat(plan.playlists.sameTitles).containsExactly("Mine")
        }
}
