package io.github.aedev.flow.subgroups

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.model.SubscriptionGroup
import io.github.aedev.flow.subsfilters.sync.ForkSync
import io.github.aedev.flow.sync.protocol.SyncCollection
import io.github.aedev.flow.sync2.changes.outgoingBatches
import io.github.aedev.flow.sync2.changes.parseBatch
import io.github.aedev.flow.sync2.data.LastSent
import io.github.aedev.flow.sync2.data.SentDao
import io.github.aedev.flow.sync2.data.SentRecord
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SubgroupLinkWireTest {
    private val androidInTechOld = SubgroupLinkRecord("Android", "Tech", hlc = "100:0:aaaaaaaa")
    private val androidInMusicNew = SubgroupLinkRecord("Android", "Music", hlc = "200:0:bbbbbbbb")
    private val androidUnlinkedNewest = SubgroupLinkRecord("Android", hlc = "300:0:aaaaaaaa", deleted = true)
    private val aiInTech = SubgroupLinkRecord("AI", "Tech", hlc = "150:0:bbbbbbbb")

    @Test
    fun `the latest change of a Subgroup wins`() {
        assertThat(SubgroupLinkWire.merge(listOf(androidInTechOld), listOf(androidInMusicNew))).containsExactly(androidInMusicNew)
        assertThat(SubgroupLinkWire.merge(listOf(androidInMusicNew), listOf(androidInTechOld))).containsExactly(androidInMusicNew)
    }

    @Test
    fun `a newer unlink beats an older link and an older one does not`() {
        assertThat(SubgroupLinkWire.merge(listOf(androidInMusicNew), listOf(androidUnlinkedNewest))).containsExactly(androidUnlinkedNewest)
        val oldUnlink = androidUnlinkedNewest.copy(hlc = "50:0:aaaaaaaa")
        assertThat(SubgroupLinkWire.merge(listOf(oldUnlink), listOf(androidInMusicNew))).containsExactly(androidInMusicNew)
    }

    @Test
    fun `merging converges in any order and is idempotent`() {
        val a = listOf(androidInTechOld, aiInTech)
        val b = listOf(androidInMusicNew)
        val c = listOf(androidUnlinkedNewest)
        val left = SubgroupLinkWire.merge(SubgroupLinkWire.merge(a, b), c)
        val right = SubgroupLinkWire.merge(c, SubgroupLinkWire.merge(b, a))
        assertThat(left).isEqualTo(right)
        assertThat(SubgroupLinkWire.merge(left, left)).isEqualTo(left)
    }

    @Test
    fun `records survive the wire`() {
        val wire = SubgroupLinkWire.encode(listOf(androidUnlinkedNewest, aiInTech))
        assertThat(SubgroupLinkWire.decode(wire.lines)).containsExactly(aiInTech, androidUnlinkedNewest).inOrder()
        assertThat(wire.recordCount).isEqualTo(2)
    }

    @Test
    fun `a rename that reaches the other device keeps its Subgroups under the renamed parent`() {
        val phone = listOf(androidInTechOld, aiInTech)
        val tv = phone
        val renamed = SubgroupLinkRecords.afterSave(phone, oldName = "Tech", newName = "Tecnologia", parent = null, stamp = "400:0:aaaaaaaa")

        val tvAfter = SubgroupLinkWire.merge(tv, SubgroupLinkWire.decode(SubgroupLinkWire.encode(renamed).lines))

        val groupsAfterRename = listOf(SubscriptionGroup("Tecnologia", emptyList()), SubscriptionGroup("Android", emptyList()), SubscriptionGroup("AI", emptyList()))
        val tree = SubgroupTree.of(groupsAfterRename, SubgroupLinkRecords.live(tvAfter))
        assertThat(tree.parentOf).containsExactly("Android", "Tecnologia", "AI", "Tecnologia")
        assertThat(SubgroupLinkRecords.live(tvAfter)).isEqualTo(SubgroupLinkRecords.live(renamed))
    }

    @Test
    fun `a Subgroup linked to a parent renamed meanwhile on the other device shows as a plain Group, nothing lost`() {
        val base = listOf(androidInTechOld)
        val phone = SubgroupLinkRecords.afterSave(base, oldName = "Tech", newName = "Tecnologia", parent = null, stamp = "400:0:aaaaaaaa")
        val tv = SubgroupLinkRecords.afterSave(base, oldName = null, newName = "AI", parent = "Tech", stamp = "410:0:bbbbbbbb")

        val merged = SubgroupLinkWire.merge(phone, tv)
        assertThat(SubgroupLinkWire.merge(tv, phone)).isEqualTo(merged)

        val groups = listOf(SubscriptionGroup("Tecnologia", emptyList()), SubscriptionGroup("Android", emptyList()), SubscriptionGroup("AI", emptyList()))
        val tree = SubgroupTree.of(groups, SubgroupLinkRecords.live(merged))
        assertThat(tree.parentOf).containsExactly("Android", "Tecnologia")
        assertThat(tree.topLevel().map { it.name }).containsExactly("Tecnologia", "AI").inOrder()
    }

    @Test
    fun `a peer that does not declare the collection leaves the local links as they are`() {
        val local = listOf(androidInTechOld, aiInTech)
        assertThat(SubgroupLinkWire.merge(local, emptyList())).isEqualTo(SubgroupLinkWire.merge(local, local))
        assertThat(SubgroupLinkWire.decode(emptyList())).isEmpty()
    }

    @Test
    fun `the collection is exchanged by Android and named apart from the original ones`() {
        assertThat(SyncCollection.ANDROID_SYNCABLE).contains(SubgroupLinkWire.COLLECTION)
        assertThat(ForkSync.COLLECTIONS).contains(SubgroupLinkWire.COLLECTION)
        assertThat(SubgroupLinkWire.COLLECTION).startsWith("fork_")
        assertThat(ForkSync.keyField(SubgroupLinkWire.COLLECTION)).isEqualTo("group")
    }

    @Test
    fun `change batches carry one record per Subgroup and only what changed`() =
        runTest {
            val sent = MemorySent()
            val head = "ab".repeat(32)
            val first = outgoingBatches(sent, mapOf(SubgroupLinkWire.COLLECTION to SubgroupLinkWire.encode(listOf(androidInTechOld, aiInTech))), head, "link", 1L)
            assertThat(first.batches).hasSize(1)
            val parsed = parseBatch(first.batches.single().content)
            assertThat(parsed.collection).isEqualTo(SubgroupLinkWire.COLLECTION)
            assertThat(SubgroupLinkWire.decode(parsed.lines)).containsExactly(aiInTech, androidInTechOld)
            sent.upsert(first.sent)

            val second = outgoingBatches(sent, mapOf(SubgroupLinkWire.COLLECTION to SubgroupLinkWire.encode(listOf(androidInMusicNew, aiInTech))), head, "link", 2L)
            assertThat(SubgroupLinkWire.decode(parseBatch(second.batches.single().content).lines)).containsExactly(androidInMusicNew)
        }

    private class MemorySent : SentDao {
        val records = HashMap<Triple<String, String, String>, SentRecord>()

        override suspend fun forCollection(
            link: String,
            collection: String,
        ) = records.values.filter { it.link == link && it.collection == collection }

        override suspend fun lastSent(link: String) =
            records.values
                .filter { it.link == link }
                .groupBy { it.collection }
                .map { (collection, sent) -> LastSent(collection, sent.maxOf { it.sentAt }) }

        override suspend fun upsert(records: List<SentRecord>) {
            records.forEach { this.records[Triple(it.link, it.collection, it.recordKey)] = it }
        }
    }
}
