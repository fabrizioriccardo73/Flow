package io.github.aedev.flow.subgroups

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.model.Channel
import io.github.aedev.flow.data.model.SubscriptionGroup
import io.github.aedev.flow.ui.screens.subscriptions.SubscriptionsUiState
import io.github.aedev.flow.ui.screens.subscriptions.UNGROUPED_GROUP_KEY
import org.junit.Test

class SubgroupTreeTest {
    private val tech = SubscriptionGroup("Tech", listOf("UCt"))
    private val android = SubscriptionGroup("Android", listOf("UCa", "UCt"))
    private val ai = SubscriptionGroup("AI", listOf("UCi"))
    private val music = SubscriptionGroup("Music", listOf("UCm"))
    private val groups = listOf(tech, android, ai, music)

    private fun tree(links: Map<String, String>) = SubgroupTree.of(groups, links)

    @Test
    fun `a parent lists its Subgroups in the Groups' order and keeps them out of the top level`() {
        val tree = tree(mapOf("AI" to "Tech", "Android" to "Tech"))
        assertThat(tree.subgroupsOf("Tech")).containsExactly(android, ai).inOrder()
        assertThat(tree.topLevel()).containsExactly(tech, music).inOrder()
        assertThat(tree.isSubgroup("AI")).isTrue()
        assertThat(tree.isSubgroup("Tech")).isFalse()
    }

    @Test
    fun `a link to or from a missing Group is ignored`() {
        val tree = tree(mapOf("AI" to "Gone", "Gone" to "Tech"))
        assertThat(tree.parentOf).isEmpty()
        assertThat(tree.topLevel()).isEqualTo(groups)
    }

    @Test
    fun `two levels after a merge leave the deeper Group as a plain Group`() {
        // Phone: AI inside Tech. TV, before syncing: Tech inside Music.
        val tree = tree(mapOf("AI" to "Tech", "Tech" to "Music"))
        assertThat(tree.parentOf).containsExactly("Tech", "Music")
        assertThat(tree.subgroupsOf("Tech")).isEmpty()
        assertThat(tree.topLevel()).containsExactly(android, ai, music).inOrder()
    }

    @Test
    fun `a cycle or a self link holds no link`() {
        assertThat(tree(mapOf("AI" to "Tech", "Tech" to "AI")).parentOf).isEmpty()
        assertThat(tree(mapOf("AI" to "AI")).parentOf).isEmpty()
    }

    @Test
    fun `a parent's scope adds its Subgroups' channels once each`() {
        val scope = scopeWithSubgroups("Tech", setOf("UCt"), groups, mapOf("Android" to "Tech", "AI" to "Tech"))
        assertThat(scope).containsExactly("UCt", "UCa", "UCi").inOrder()
    }

    @Test
    fun `a Subgroup, All and a Group without Subgroups keep their own scope`() {
        val links = mapOf("AI" to "Tech")
        assertThat(scopeWithSubgroups("AI", setOf("UCi"), groups, links)).containsExactly("UCi")
        assertThat(scopeWithSubgroups(null, null, groups, links)).isNull()
        assertThat(scopeWithSubgroups("Music", setOf("UCm"), groups, links)).containsExactly("UCm")
    }

    @Test
    fun `the Subscriptions state scopes a parent with its Subgroups and leaves Ungrouped alone`() {
        val subscribed = listOf("UCt", "UCa", "UCi", "UCm", "UCx").map { Channel(id = it, name = it, thumbnailUrl = "", subscriberCount = 0L) }
        val state =
            SubscriptionsUiState(
                subscribedChannels = subscribed,
                groups = groups,
                ungroupedEnabled = true,
                subgroupLinks = mapOf("AI" to "Tech"),
            )
        assertThat(state.copy(selectedGroupName = "Tech").selectedGroupScope()).containsExactly("UCt", "UCi")
        // A channel only in a Subgroup is still grouped.
        assertThat(state.copy(selectedGroupName = UNGROUPED_GROUP_KEY).selectedGroupScope()).containsExactly("UCx")
    }
}
