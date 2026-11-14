package io.github.aedev.flow.subgroups

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.model.SubscriptionGroup
import io.github.aedev.flow.ui.screens.subscriptions.UNGROUPED_GROUP_KEY
import org.junit.Test

class SubgroupChipsTest {
    private val tech = SubscriptionGroup("Tech", listOf("UCt"))
    private val android = SubscriptionGroup("Android", listOf("UCa"))
    private val ai = SubscriptionGroup("AI", listOf("UCi"))
    private val music = SubscriptionGroup("Music", listOf("UCm"))
    private val groups = listOf(tech, android, ai, music)
    private val links = mapOf("Android" to "Tech", "AI" to "Tech")

    private fun chips(selected: String?) = SubgroupChips(groups, links, selected)

    @Test
    fun `the main row keeps Ungrouped and the top-level Groups only`() {
        val ungrouped = SubscriptionGroup(UNGROUPED_GROUP_KEY, listOf("UCx"))
        val row = chips(null).topLevel(groups + ungrouped)
        assertThat(row).containsExactly(tech, music, ungrouped).inOrder()
    }

    @Test
    fun `no second row for All or for a Group without Subgroups`() {
        assertThat(chips(null).subgroupRow).isEmpty()
        assertThat(chips("Music").subgroupRow).isEmpty()
    }

    @Test
    fun `selecting a parent shows its Subgroups with none of them lit`() {
        val chips = chips("Tech")
        assertThat(chips.selectedTop).isEqualTo("Tech")
        assertThat(chips.selectedSubgroup).isNull()
        assertThat(chips.subgroupRow).containsExactly(android, ai).inOrder()
    }

    @Test
    fun `a selected Subgroup lights its parent in the main row and itself in the second`() {
        val chips = chips("AI")
        assertThat(chips.selectedTop).isEqualTo("Tech")
        assertThat(chips.selectedSubgroup).isEqualTo("AI")
        assertThat(chips.subgroupRow).containsExactly(android, ai).inOrder()
    }

    @Test
    fun `a tap narrows to a Subgroup, switches between them, and a second tap goes back to the parent`() {
        assertThat(chips("Tech").afterTap("AI")).isEqualTo("AI")
        assertThat(chips("AI").afterTap("Android")).isEqualTo("Android")
        assertThat(chips("AI").afterTap("AI")).isEqualTo("Tech")
    }

    @Test
    fun `a saved Subgroup whose link no longer holds is selected as a plain Group`() {
        val chips = SubgroupChips(groups, mapOf("AI" to "Gone"), "AI")
        assertThat(chips.selectedTop).isEqualTo("AI")
        assertThat(chips.selectedSubgroup).isNull()
        assertThat(chips.subgroupRow).isEmpty()
        assertThat(chips.topLevel(groups)).contains(ai)
    }

    @Test
    fun `deleting the selected Subgroup selects its parent, anything else keeps the original behaviour`() {
        assertThat(selectionAfterDelete("AI", "AI", groups, links)).isEqualTo("Tech")
        assertThat(selectionAfterDelete("AI", "Tech", groups, links)).isNull()
        assertThat(selectionAfterDelete("Music", "Music", groups, links)).isNull()
    }
}
