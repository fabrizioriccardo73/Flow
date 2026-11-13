package io.github.aedev.flow.coda

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.model.Video
import org.junit.Test

internal fun video(id: String) =
    Video(
        id = id,
        title = "Video $id",
        channelName = "Channel",
        channelId = "UC123",
        thumbnailUrl = "https://example.test/$id.jpg",
        duration = 120,
        viewCount = 10L,
        uploadDate = "today",
    )

internal fun codaOf(
    vararg ids: String,
    playingId: String? = null,
) = Coda(ids.map(::video), playingId)

internal fun Coda.ids() = videos.map(Video::id)

/**
 * The Coda is what the viewer chose to watch soon: nothing but its own operations may change it,
 * a video appears once, and a video leaves only when it is done with.
 */
class CodaTest {
    @Test
    fun `append adds at the end and keeps a video already queued where it is`() {
        val coda = codaOf("a", "b").append(video("c")).append(video("a"))

        assertThat(coda.ids()).containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun `putFirst moves a queued video to the top instead of adding it twice`() {
        val coda = codaOf("a", "b", "c").putFirst(video("c"))

        assertThat(coda.ids()).containsExactly("c", "a", "b").inOrder()
    }

    @Test
    fun `putFirst goes after the video in riproduzione`() {
        val coda = codaOf("a", "b", playingId = "a").putFirst(video("n"))

        assertThat(coda.ids()).containsExactly("a", "n", "b").inOrder()
        assertThat(coda.playingId).isEqualTo("a")
    }

    @Test
    fun `a queued video that starts playing moves to the top in riproduzione`() {
        val coda = codaOf("a", "b", "c").markPlaying("c")

        assertThat(coda.ids()).containsExactly("c", "a", "b").inOrder()
        assertThat(coda.playingId).isEqualTo("c")
    }

    @Test
    fun `a video outside the Coda that starts playing changes nothing`() {
        val coda = codaOf("a", "b", playingId = "a")

        assertThat(coda.markPlaying("x")).isEqualTo(coda)
    }

    @Test
    fun `an interrupted video stays at the top until it is finished`() {
        // "a" plays, the viewer opens "x" from the home feed: "a" is still waiting, first.
        val coda = codaOf("a", "b").markPlaying("a").markPlaying("x")

        assertThat(coda.ids()).containsExactly("a", "b").inOrder()
        assertThat(coda.nextAfter("x")?.id).isEqualTo("a")
    }

    @Test
    fun `finishing a video takes it out and clears riproduzione`() {
        val coda = codaOf("a", "b", playingId = "a").finish("a")

        assertThat(coda.ids()).containsExactly("b")
        assertThat(coda.playingId).isNull()
    }

    @Test
    fun `finishing another video keeps the one in riproduzione`() {
        val coda = codaOf("a", "b", playingId = "a").finish("b")

        assertThat(coda.playingId).isEqualTo("a")
    }

    @Test
    fun `nextAfter skips the video that is playing`() {
        val coda = codaOf("a", "b", playingId = "a")

        assertThat(coda.nextAfter("a")?.id).isEqualTo("b")
        assertThat(coda.nextAfter("x")?.id).isEqualTo("a")
        assertThat(codaOf("a").nextAfter("a")).isNull()
    }

    @Test
    fun `a saved Coda merged with early additions keeps both without duplicates`() {
        val merged = codaOf("a", "b", playingId = "a").mergedWith(codaOf("b", "c"))

        assertThat(merged.ids()).containsExactly("a", "b", "c").inOrder()
        assertThat(merged.playingId).isEqualTo("a")
    }

    @Test
    fun `the stored Coda reads back the same, even with one video`() {
        val one = codaOf("a", playingId = "a")
        val many = codaOf("a", "b", "c", playingId = "a")

        assertThat(CodaCodec.decode(CodaCodec.encode(one))?.ids()).containsExactly("a")
        assertThat(CodaCodec.decode(CodaCodec.encode(one))?.playingId).isEqualTo("a")
        assertThat(CodaCodec.decode(CodaCodec.encode(many))?.ids()).containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun `stored text that is not a Coda reads as nothing`() {
        assertThat(CodaCodec.decode("not json")).isNull()
    }

    @Test
    fun `move reorders and ignores positions outside the Coda`() {
        val coda = codaOf("a", "b", "c")

        assertThat(coda.move(0, 2).ids()).containsExactly("b", "c", "a").inOrder()
        assertThat(coda.move(0, 5)).isEqualTo(coda)
    }

    @Test
    fun `a removed video comes back where it was`() {
        val (rest, removed) = codaOf("a", "b", "c", playingId = "b").remove("b")!!

        assertThat(rest.ids()).containsExactly("a", "c").inOrder()
        assertThat(rest.playingId).isNull()

        val restored = rest.restore(removed)
        assertThat(restored.ids()).containsExactly("a", "b", "c").inOrder()
        assertThat(restored.playingId).isEqualTo("b")
    }

    @Test
    fun `undoing a removal does not duplicate a video added again meanwhile`() {
        val (rest, removed) = codaOf("a", "b").remove("a")!!

        assertThat(rest.append(video("a")).restore(removed).ids()).containsExactly("b", "a").inOrder()
    }

    @Test
    fun `removing a video that is not queued gives nothing to undo`() {
        assertThat(codaOf("a").remove("x")).isNull()
    }
}
