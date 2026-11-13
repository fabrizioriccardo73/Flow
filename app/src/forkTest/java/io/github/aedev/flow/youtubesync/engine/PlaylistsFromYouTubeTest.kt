package io.github.aedev.flow.youtubesync.engine

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.youtubesync.local.LocalPlaylistKind
import io.github.aedev.flow.youtubesync.remote.PlaylistKind
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

/** Ticket 08, through the seam: Owned Playlists and Saved Playlists arrive in Flow from YouTube. */
class PlaylistsFromYouTubeTest : SyncFixture() {
    @After
    fun tearDown() = close()

    @Test
    fun `on the first link Owned Playlists arrive with their YouTube id and kind`() =
        runBlocking<Unit> {
            youTube.playlist("PLowned0000001", "Mine", "v1", "v2")

            val plan = sync.plan()
            assertThat(plan.isFirstLink).isTrue()
            assertThat(plan.playlists.importToFlow.map { it.playlist.title }).containsExactly("Mine")
            sync.apply(plan)

            val imported = playlists.byYouTubeId("PLowned0000001")!!
            assertThat(imported.kind).isEqualTo(LocalPlaylistKind.OWNED)
            assertThat(imported.title).isEqualTo("Mine")
            assertThat(imported.videos.map { it.videoId }).containsExactly("v1", "v2").inOrder()
            assertThat(baselines.read()!!.playlists.keys).containsExactly("PLowned0000001")
        }

    @Test
    fun `Saved Playlists arrive read-only and follow YouTube`() =
        runBlocking<Unit> {
            youTube.playlist("PLsaved0000001", "Theirs", "v1", kind = PlaylistKind.SAVED)
            syncNow()
            assertThat(playlists.byYouTubeId("PLsaved0000001")!!.kind).isEqualTo(LocalPlaylistKind.SAVED)

            youTube.playlists.getValue("PLsaved0000001").items += youTube.item("v2")
            youTube.playlists.getValue("PLsaved0000001").title = "Theirs, renamed"
            syncNow()

            val saved = playlists.byYouTubeId("PLsaved0000001")!!
            assertThat(saved.title).isEqualTo("Theirs, renamed")
            assertThat(saved.videos.map { it.videoId }).containsExactly("v1", "v2").inOrder()
            assertThat(youTube.writes).isEmpty()
        }

    @Test
    fun `a Saved Playlist removed from the library on YouTube goes away in Flow`() =
        runBlocking<Unit> {
            youTube.playlist("PLsaved0000001", "Theirs", "v1", kind = PlaylistKind.SAVED)
            syncNow()

            youTube.playlists.remove("PLsaved0000001")
            syncNow()

            assertThat(playlists.byYouTubeId("PLsaved0000001")).isNull()
        }

    @Test
    fun `deleted and private videos never appear in Flow`() =
        runBlocking<Unit> {
            youTube.playlist("PLowned0000001", "Mine", "v1")
            youTube.playlists.getValue("PLowned0000001").items += youTube.item("gone", available = false)
            youTube.playlists.getValue("PLowned0000001").items += youTube.item("v2")

            syncNow()

            assertThat(playlists.videoIds("PLowned0000001")).containsExactly("v1", "v2").inOrder()
        }

    @Test
    fun `Flow-only playlists are never sent to YouTube`() =
        runBlocking<Unit> {
            playlists.flowOnly("1700000000000", "Music for later", "v1")
            syncNow()
            syncNow()

            assertThat(youTube.playlists).isEmpty()
            assertThat(youTube.writes).isEmpty()
            assertThat(playlists.playlists.getValue("1700000000000").kind).isEqualTo(LocalPlaylistKind.FLOW_ONLY)
        }

    @Test
    fun `the first link warns about a Flow-only playlist with the same title and does not merge them`() =
        runBlocking<Unit> {
            playlists.flowOnly("1700000000000", "Favourites", "a")
            youTube.playlist("PLowned0000001", "favourites ", "b")

            val plan = sync.plan()
            assertThat(plan.playlists.sameTitles).containsExactly("favourites ")
            sync.apply(plan)

            assertThat(playlists.playlists).hasSize(2)
            assertThat(
                playlists.playlists
                    .getValue("1700000000000")
                    .videos
                    .map { it.videoId },
            ).containsExactly("a")
        }

    @Test
    fun `a playlist created on YouTube after the first link arrives in Flow as an Owned Playlist`() =
        runBlocking<Unit> {
            syncNow()
            youTube.playlist("PLlater0000001", "New one", "v1")

            syncNow()

            assertThat(playlists.byYouTubeId("PLlater0000001")!!.kind).isEqualTo(LocalPlaylistKind.OWNED)
        }
}
