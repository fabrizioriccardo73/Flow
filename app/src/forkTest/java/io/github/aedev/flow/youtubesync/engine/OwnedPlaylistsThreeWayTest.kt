package io.github.aedev.flow.youtubesync.engine

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.youtubesync.remote.YouTubeAccountException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Ticket 09, through the seam: Owned Playlists stay in step both ways. */
class OwnedPlaylistsThreeWayTest : SyncFixture() {
    private val id = "PLowned0000001"

    /** Every test starts after a first link where both sides hold "Mine" with a, b, c, d. */
    @Before
    fun firstLink() =
        runBlocking<Unit> {
            youTube.playlist(id, "Mine", "a", "b", "c", "d")
            syncNow()
            youTube.writes.clear()
        }

    @After
    fun tearDown() = close()

    private fun flowVideos() = playlists.videoIds(id)

    private fun youTubeVideos() = youTube.playlists.getValue(id).videoIds

    private fun setFlow(vararg videos: String) = playlists.edit(id) { it.copy(videos = videos.map(::video)) }

    private fun setYouTube(vararg videos: String) {
        val items = youTube.playlists.getValue(id).items
        val byVideo = items.associateBy { it.videoId }
        items.clear()
        videos.forEach { items += byVideo[it] ?: youTube.item(it) }
    }

    private suspend fun assertConverged(vararg videos: String) {
        assertThat(flowVideos()).containsExactlyElementsIn(videos.toList()).inOrder()
        assertThat(youTubeVideos()).containsExactlyElementsIn(videos.toList()).inOrder()
        assertThat(
            baselines
                .read()!!
                .playlists
                .getValue(id)
                .videos,
        ).containsExactlyElementsIn(videos.toList()).inOrder()
    }

    @Test
    fun `a video added in Flow is added on YouTube`() =
        runBlocking<Unit> {
            setFlow("a", "b", "c", "d", "e")
            syncNow()
            assertConverged("a", "b", "c", "d", "e")
            assertThat(youTube.writes).containsExactly("add")
        }

    @Test
    fun `a video removed on YouTube is removed in Flow`() =
        runBlocking<Unit> {
            setYouTube("a", "c", "d")
            syncNow()
            assertConverged("a", "c", "d")
            assertThat(youTube.writes).isEmpty()
        }

    @Test
    fun `a video removed in Flow is removed on YouTube`() =
        runBlocking<Unit> {
            setFlow("a", "b", "d")
            syncNow()
            assertConverged("a", "b", "d")
            assertThat(youTube.writes).containsExactly("remove")
        }

    @Test
    fun `a video added on both sides appears once`() =
        runBlocking<Unit> {
            setFlow("a", "b", "c", "d", "x")
            setYouTube("a", "b", "c", "d", "x")
            syncNow()
            assertConverged("a", "b", "c", "d", "x")
            assertThat(youTube.writes).isEmpty()
        }

    @Test
    fun `a reorder in Flow reaches YouTube with the fewest moves`() =
        runBlocking<Unit> {
            setFlow("b", "c", "d", "a")
            syncNow()
            assertConverged("b", "c", "d", "a")
            assertThat(youTube.writes).containsExactly("move")
        }

    @Test
    fun `when both sides reorder YouTube wins`() =
        runBlocking<Unit> {
            setFlow("d", "c", "b", "a")
            setYouTube("b", "a", "c", "d")
            syncNow()
            assertConverged("b", "a", "c", "d")
        }

    @Test
    fun `a rename in Flow reaches YouTube and on both sides YouTube wins`() =
        runBlocking<Unit> {
            playlists.edit(id) { it.copy(title = "Renamed in Flow") }
            syncNow()
            assertThat(youTube.playlists.getValue(id).title).isEqualTo("Renamed in Flow")

            playlists.edit(id) { it.copy(title = "Flow again") }
            youTube.playlists.getValue(id).title = "YouTube again"
            syncNow()
            assertThat(playlists.byYouTubeId(id)!!.title).isEqualTo("YouTube again")
            assertThat(youTube.playlists.getValue(id).title).isEqualTo("YouTube again")
        }

    @Test
    fun `a rename never touches the description`() =
        runBlocking<Unit> {
            youTube.playlists.getValue(id).description = "Kept"
            playlists.edit(id) { it.copy(title = "New") }
            syncNow()
            assertThat(youTube.playlists.getValue(id).description).isEqualTo("Kept")
        }

    @Test
    fun `a playlist deleted in Flow is deleted on YouTube`() =
        runBlocking<Unit> {
            playlists.delete(id)
            syncNow()
            assertThat(youTube.playlists).doesNotContainKey(id)
            assertThat(baselines.read()!!.playlists).doesNotContainKey(id)
        }

    @Test
    fun `a deletion beats a change - renamed in Flow, deleted on YouTube`() =
        runBlocking<Unit> {
            playlists.edit(id) { it.copy(title = "Renamed") }
            youTube.playlists.remove(id)
            syncNow()
            assertThat(playlists.byYouTubeId(id)).isNull()
            assertThat(youTube.writes).isEmpty()
        }

    @Test
    fun `deleted and private videos are never removed or moved on YouTube`() =
        runBlocking<Unit> {
            val gone = youTube.item("gone", available = false)
            youTube.playlists
                .getValue(id)
                .items
                .add(1, gone)
            setFlow("d", "c", "b")
            syncNow()

            assertThat(youTube.playlists.getValue(id).items).contains(gone)
            assertThat(youTubeVideos()).containsExactly("d", "c", "b").inOrder()
            assertThat(flowVideos()).containsExactly("d", "c", "b").inOrder()
        }

    @Test
    fun `a second run with nothing changed writes nothing`() =
        runBlocking<Unit> {
            setFlow("d", "a", "x")
            youTube.playlists.getValue(id).title = "T"
            syncNow()
            youTube.writes.clear()
            val before = baselines.read()

            val plan = sync.plan()
            assertThat(plan.isEmpty).isTrue()
            sync.apply(plan)

            assertThat(youTube.writes).isEmpty()
            assertThat(baselines.read()).isEqualTo(before)
            assertThat(sync.hasLocalChanges()).isFalse()
        }

    @Test
    fun `losing most videos of a playlist needs confirmation`() =
        runBlocking<Unit> {
            setYouTube()
            val plan = sync.plan()
            assertThat(plan.massRemovals.map { it.collection }).containsExactly(SyncCollection.PLAYLIST_VIDEOS)
            assertThat(plan.massRemovals.single().container).isEqualTo("Mine")

            val refused = runCatching { sync.apply(plan) }.exceptionOrNull()
            assertThat(refused).isInstanceOf(MassRemovalNotConfirmedException::class.java)
            assertThat(flowVideos()).containsExactly("a", "b", "c", "d")
        }

    @Test
    fun `losing many playlists needs confirmation`() =
        runBlocking<Unit> {
            (2..5).forEach { youTube.playlist("PLowned000000$it", "P$it", "v$it") }
            syncNow()
            (1..5).map { if (it == 1) id else "PLowned000000$it" }.forEach { youTube.playlists.remove(it) }

            val plan = sync.plan()
            assertThat(plan.massRemovals.map { it.collection }).contains(SyncCollection.PLAYLISTS)
        }

    @Test
    fun `a run stopped by the quota is completed by the next one`() =
        runBlocking<Unit> {
            setFlow("a", "b", "c", "d", "e", "f")
            youTube.failWrite["add"] = YouTubeAccountException.QuotaExceeded()
            val result = syncNow()
            assertThat(result.stoppedBy).isInstanceOf(YouTubeAccountException.QuotaExceeded::class.java)
            assertThat(sync.hasLocalChanges()).isTrue()

            syncNow()
            assertConverged("a", "b", "c", "d", "e", "f")
            assertThat(sync.hasLocalChanges()).isFalse()
        }

    @Test
    fun `changes on both sides converge`() =
        runBlocking<Unit> {
            setFlow("a", "c", "d", "f")
            setYouTube("a", "b", "c", "e")
            syncNow()
            assertConverged("a", "c", "e", "f")
        }
}
