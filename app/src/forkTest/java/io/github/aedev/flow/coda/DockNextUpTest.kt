package io.github.aedev.flow.coda

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The dock announces what will really play next, so it must follow the same order as playback. */
class DockNextUpTest {
    @Test
    fun `while the playlist has videos left the dock shows the playlist's next`() {
        val dock = dockNextUp(IndexedValue(1, video("p2")), 3, "Playlist", codaOf("c1"), "p1", "Queue")

        assertThat(dock.copy(video = null)).isEqualTo(DockNextUp(null, 2, "Playlist", 3))
        assertThat(dock.video?.id).isEqualTo("p2")
    }

    @Test
    fun `once the playlist is over the dock shows the Coda`() {
        val dock = dockNextUp(null, 3, "Playlist", codaOf("c1", "c2"), "p3", "Queue")

        assertThat(dock.copy(video = null)).isEqualTo(DockNextUp(null, 1, "Queue", 2))
        assertThat(dock.video?.id).isEqualTo("c1")
    }

    @Test
    fun `the Coda video that is playing is not announced again`() {
        val dock = dockNextUp(null, 0, null, codaOf("c1", "c2", playingId = "c1"), "c1", "Queue")

        assertThat(dock.video?.id).isEqualTo("c2")
        assertThat(dock.position).isEqualTo(2)
    }

    @Test
    fun `with nothing left the dock has no video`() {
        assertThat(dockNextUp(null, 2, "Playlist", Coda(), "p2", "Queue").video).isNull()
    }
}
