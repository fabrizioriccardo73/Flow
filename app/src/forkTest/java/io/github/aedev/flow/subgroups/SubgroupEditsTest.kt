package io.github.aedev.flow.subgroups

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.model.SubscriptionGroup
import io.github.aedev.flow.ui.screens.subscriptions.UNGROUPED_GROUP_KEY
import org.junit.Test

class SubgroupEditsTest {
    private val old = "100:0:aaaaaaaa"
    private val now = "200:0:aaaaaaaa"
    private val ai = SubgroupLinkRecord("AI", "Tech", hlc = old)
    private val android = SubgroupLinkRecord("Android", "Tech", hlc = old)

    private fun live(records: List<SubgroupLinkRecord>) = SubgroupLinkRecords.live(records)

    @Test
    fun `a new Group saved inside a parent gets a link`() {
        val records = SubgroupLinkRecords.afterSave(emptyList(), null, "AI", "Tech", now)
        assertThat(records).containsExactly(SubgroupLinkRecord("AI", "Tech", now))
    }

    @Test
    fun `saving without changes writes nothing`() {
        assertThat(SubgroupLinkRecords.afterSave(listOf(ai), "AI", "AI", "Tech", now)).containsExactly(ai)
        assertThat(SubgroupLinkRecords.afterSave(emptyList(), null, "Music", null, now)).isEmpty()
    }

    @Test
    fun `taking a Subgroup out of its parent leaves a tombstone`() {
        val records = SubgroupLinkRecords.afterSave(listOf(ai), "AI", "AI", null, now)
        assertThat(records).containsExactly(SubgroupLinkRecord("AI", hlc = now, deleted = true))
    }

    @Test
    fun `renaming a Subgroup keeps it in its parent`() {
        val records = SubgroupLinkRecords.afterSave(listOf(ai), "AI", "ML", "Tech", now)
        assertThat(live(records)).containsExactly("ML", "Tech")
        assertThat(records).contains(SubgroupLinkRecord("AI", hlc = now, deleted = true))
    }

    @Test
    fun `renaming a parent keeps its Subgroups`() {
        val records = SubgroupLinkRecords.afterSave(listOf(ai, android), "Tech", "Tecnologia", null, now)
        assertThat(live(records)).containsExactly("AI", "Tecnologia", "Android", "Tecnologia")
        assertThat(records.none { it.group == "Tech" }).isTrue()
    }

    @Test
    fun `deleting a parent ends its Subgroups' links and nothing else`() {
        val music = SubgroupLinkRecord("Jazz", "Music", hlc = old)
        val records = SubgroupLinkRecords.afterDelete(listOf(ai, android, music), "Tech", now)
        assertThat(live(records)).containsExactly("Jazz", "Music")
        assertThat(records).contains(SubgroupLinkRecord("AI", hlc = now, deleted = true))
    }

    @Test
    fun `deleting a Subgroup ends its own link`() {
        val records = SubgroupLinkRecords.afterDelete(listOf(ai, android), "AI", now)
        assertThat(live(records)).containsExactly("Android", "Tech")
    }

    @Test
    fun `a parent can only be a Group that is not inside another, and never the edited one`() {
        val groups = listOf("Tech", "AI", "Music").map { SubscriptionGroup(it, emptyList()) }
        val tree = SubgroupTree.of(groups, mapOf("AI" to "Tech"))
        assertThat(tree.parentOptions("Music").map { it.name }).containsExactly("Tech")
        assertThat(tree.parentOptions(null).map { it.name }).containsExactly("Tech", "Music").inOrder()
        assertThat(tree.hasSubgroups("Tech")).isTrue()
        assertThat(tree.hasSubgroups("Music")).isFalse()
    }

    @Test
    fun `the tree order puts each parent's Subgroups right after it`() {
        val links = mapOf("AI" to "Tech", "Android" to "Tech")
        val order = treeOrder(listOf("AI", "Music", "Tech", UNGROUPED_GROUP_KEY, "Android"), links) { it }
        assertThat(order).containsExactly("Music", "Tech", "AI", "Android", UNGROUPED_GROUP_KEY).inOrder()
    }

    @Test
    fun `a Subgroup dragged out of its parent snaps back under it in its new sibling order`() {
        val links = mapOf("AI" to "Tech", "Android" to "Tech")
        // Was Tech, AI, Android, Music; Android dragged to the very top.
        val order = treeOrder(listOf("Android", "Tech", "AI", "Music"), links) { it }
        assertThat(order).containsExactly("Tech", "Android", "AI", "Music").inOrder()
    }

    @Test
    fun `the move buttons keep Subgroups among their siblings and move a parent with its Subgroups`() {
        val links = mapOf("AI" to "Tech", "Android" to "Tech")
        val list = listOf("Music", "Tech", "AI", "Android", "News")
        assertThat(treeMove(list, links, 3, up = true) { it }).containsExactly("Music", "Tech", "Android", "AI", "News").inOrder()
        assertThat(treeMove(list, links, 2, up = true) { it }).isEqualTo(list)
        assertThat(treeMove(list, links, 1, up = false) { it }).containsExactly("Music", "News", "Tech", "AI", "Android").inOrder()
        assertThat(treeMove(list, links, 1, up = true) { it }).containsExactly("Tech", "AI", "Android", "Music", "News").inOrder()
        assertThat(treeMove(list, links, 4, up = false) { it }).isEqualTo(list)
    }
}
