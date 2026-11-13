package io.github.aedev.flow.coda

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.player.PlaybackQueueController
import org.junit.Test

/**
 * Playback order is Contesto di riproduzione, then Coda, then Autoplay. The Contesto here is the
 * real queue controller, so "Play next" and shuffle behave as they do in the player.
 */
class UpNextTest {
    private val autoplay = video("auto")

    // Video carries a creation timestamp, so results compare by id and source.
    private fun next(
        contesto: PlaybackQueueController,
        coda: Coda,
        currentVideoId: String? = contesto.currentVideo?.id,
    ) = upNext(contesto.nextVideo(), coda, currentVideoId, autoplay)?.let { it.video.id to it.source }

    private fun playlist(
        vararg ids: String,
        at: Int = 0,
    ) = PlaybackQueueController().apply { setQueue(ids.map(::video), at, title = "Playlist") }

    @Test
    fun `a playlist plays to its end before the Coda`() {
        val contesto = playlist("p1", "p2")
        val coda = codaOf("c1")

        assertThat(next(contesto, coda)).isEqualTo("p2" to UpNextSource.CONTESTO)

        contesto.moveTo(1)
        assertThat(next(contesto, coda)).isEqualTo("c1" to UpNextSource.CODA)
    }

    @Test
    fun `a single tapped video is followed by the Coda`() {
        // A tapped video leaves the Contesto empty: it is the whole Contesto.
        val contesto = PlaybackQueueController()

        assertThat(next(contesto, codaOf("c1", "c2"), currentVideoId = "x")?.first).isEqualTo("c1")
    }

    @Test
    fun `Autoplay waits for an empty Coda`() {
        val contesto = playlist("p1")

        assertThat(next(contesto, codaOf("c1"))?.second).isEqualTo(UpNextSource.CODA)
        assertThat(next(contesto, Coda())).isEqualTo("auto" to UpNextSource.AUTOPLAY)
    }

    @Test
    fun `Play next during a playlist plays that video and then the playlist resumes`() {
        val contesto = playlist("p1", "p2", "p3")
        var coda = codaOf("c1")
        val n = video("n")

        coda = coda.putFirst(n)
        contesto.addNext(n, currentlyPlaying = video("p1"))

        assertThat(next(contesto, coda)?.first).isEqualTo("n")
        contesto.moveTo(1)
        coda = coda.markPlaying("n").finish("n")
        assertThat(next(contesto, coda)?.first).isEqualTo("p2")
        assertThat(coda.ids()).containsExactly("c1")
    }

    @Test
    fun `a Coda video that is playing is not offered again as next`() {
        val coda = codaOf("c1", "c2").markPlaying("c1")

        assertThat(next(PlaybackQueueController(), coda, currentVideoId = "c1")?.first).isEqualTo("c2")
    }

    @Test
    fun `an interrupted Coda video comes back after the video that interrupted it`() {
        val coda = codaOf("c1", "c2").markPlaying("c1").markPlaying("x")

        assertThat(next(PlaybackQueueController(), coda, currentVideoId = "x")?.first).isEqualTo("c1")
    }

    @Test
    fun `shuffle and loop of the playlist leave the Coda alone`() {
        val contesto = playlist("p1", "p2", "p3")
        val coda = codaOf("c1", "c2")

        contesto.setShuffleEnabled(true)
        contesto.setLoopEnabled(true)

        assertThat(coda.ids()).containsExactly("c1", "c2").inOrder()
        // A looping playlist never runs out, so the Coda waits for it to be stopped.
        contesto.moveTo(contesto.size - 1)
        assertThat(next(contesto, coda)?.second).isEqualTo(UpNextSource.CONTESTO)
    }
}
