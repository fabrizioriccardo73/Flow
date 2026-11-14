package io.github.aedev.flow.subgroups

import io.github.aedev.flow.data.model.SubscriptionGroup

/**
 * The Groups as the channel's group sheet lists them: each parent followed by its Subgroups, which
 * are [indented]. Each Group keeps its own checkbox, so ticking a Subgroup leaves the parent alone.
 */
class SubgroupSheetLayout(
    groups: List<SubscriptionGroup>,
    links: Map<String, String>,
) {
    private val tree = SubgroupTree.of(groups, links)

    val groups: List<SubscriptionGroup> = treeOrder(groups, links) { it.name }

    fun indented(name: String): Boolean = tree.isSubgroup(name)
}
