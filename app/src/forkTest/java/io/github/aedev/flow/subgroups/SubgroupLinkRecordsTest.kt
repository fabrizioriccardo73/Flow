package io.github.aedev.flow.subgroups

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SubgroupLinkRecordsTest {
    private val ai = SubgroupLinkRecord("AI", "Tech", hlc = "100:0:aaaaaaaa")
    private val android = SubgroupLinkRecord("Android", "Tech", hlc = "200:0:aaaaaaaa")

    @Test
    fun `records survive storage`() {
        val stored = SubgroupLinkRecords.encode(listOf(ai, android))
        assertThat(SubgroupLinkRecords.decode(stored)).containsExactly(android, ai)
    }

    @Test
    fun `missing or unreadable storage reads as no links`() {
        assertThat(SubgroupLinkRecords.decode(null)).isEmpty()
        assertThat(SubgroupLinkRecords.decode("not json")).isEmpty()
    }

    @Test
    fun `a tombstone takes the link out and replaces the old record`() {
        val removed = SubgroupLinkRecord("AI", hlc = "300:0:aaaaaaaa", deleted = true)
        val records = SubgroupLinkRecords.replace(listOf(ai, android), "AI", removed)
        assertThat(records).containsExactly(android, removed)
        assertThat(SubgroupLinkRecords.live(records)).containsExactly("Android", "Tech")
    }
}
