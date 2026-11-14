package io.github.aedev.flow.subsfilters

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.subsfilters.sync.ChannelLanguageRecord
import io.github.aedev.flow.subsfilters.sync.ChannelLanguageRecords
import io.github.aedev.flow.sync.protocol.SyncCollection
import org.junit.Test

class ChannelLanguageRecordsTest {
    private val itOld = ChannelLanguageRecord("c1", "it", hlc = "100:0:aaaaaaaa")
    private val enNew = ChannelLanguageRecord("c1", "en", hlc = "200:0:bbbbbbbb")
    private val removedNewest = ChannelLanguageRecord("c1", hlc = "300:0:aaaaaaaa", deleted = true)
    private val other = ChannelLanguageRecord("c2", "fr", hlc = "150:0:bbbbbbbb")

    @Test
    fun `the latest change of a channel wins`() {
        assertThat(ChannelLanguageRecords.merge(listOf(itOld), listOf(enNew))).containsExactly(enNew)
        assertThat(ChannelLanguageRecords.merge(listOf(enNew), listOf(itOld))).containsExactly(enNew)
    }

    @Test
    fun `a newer removal beats an older language and an older one does not`() {
        assertThat(ChannelLanguageRecords.merge(listOf(enNew), listOf(removedNewest))).containsExactly(removedNewest)
        val oldRemoval = removedNewest.copy(hlc = "50:0:aaaaaaaa")
        assertThat(ChannelLanguageRecords.merge(listOf(oldRemoval), listOf(enNew))).containsExactly(enNew)
    }

    @Test
    fun `merging converges in any order and is idempotent`() {
        val a = listOf(itOld, other)
        val b = listOf(enNew)
        val c = listOf(removedNewest)
        val left = ChannelLanguageRecords.merge(ChannelLanguageRecords.merge(a, b), c)
        val right = ChannelLanguageRecords.merge(c, ChannelLanguageRecords.merge(b, a))
        assertThat(left).isEqualTo(right)
        assertThat(ChannelLanguageRecords.merge(left, left)).isEqualTo(left)
    }

    @Test
    fun `records survive the wire`() {
        val wire = ChannelLanguageRecords.encode(listOf(other, enNew))
        assertThat(ChannelLanguageRecords.decode(wire.lines)).containsExactly(enNew, other).inOrder()
        assertThat(wire.recordCount).isEqualTo(2)
    }

    @Test
    fun `the collection is exchanged by Android and named apart from the original ones`() {
        assertThat(SyncCollection.ANDROID_SYNCABLE).contains(ChannelLanguageRecords.COLLECTION)
        assertThat(ChannelLanguageRecords.COLLECTION).startsWith("fork_")
    }
}
