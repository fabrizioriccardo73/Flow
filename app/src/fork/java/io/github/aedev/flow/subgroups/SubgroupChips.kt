package io.github.aedev.flow.subgroups

import io.github.aedev.flow.data.model.SubscriptionGroup

/**
 * The two rows of group chips. The stored selection stays one name, the narrowest chip picked: a
 * Subgroup when one is selected, otherwise a top-level Group. So the original code scopes the feed,
 * the Language filter and Quick access by it unchanged, and a Subgroup whose link no longer holds is
 * simply selected as the plain Group it has become.
 */
class SubgroupChips(
    groups: List<SubscriptionGroup>,
    links: Map<String, String>,
    /** The selected chip, as [io.github.aedev.flow.ui.screens.subscriptions.SubscriptionsUiState.selectedChip] gives it. */
    private val selected: String?,
) {
    private val tree = SubgroupTree.of(groups, links)

    /** The chip lit in the main row: the selected Subgroup's parent, or the selection itself. */
    val selectedTop: String? = selected?.let { tree.parentOf[it] ?: it }

    /** The lit chip of the second row, if a Subgroup is selected. */
    val selectedSubgroup: String? = selected?.takeIf { tree.isSubgroup(it) }

    /** The second row: [selectedTop]'s Subgroups, empty when it has none. */
    val subgroupRow: List<SubscriptionGroup> = selectedTop?.let(tree::subgroupsOf).orEmpty()

    /** [chips] (the main row as the original code builds it, Ungrouped included) without the Subgroups. */
    fun topLevel(chips: List<SubscriptionGroup>): List<SubscriptionGroup> = chips.filterNot { tree.isSubgroup(it.name) }

    /** What to select after tapping [subgroup]: it, or back to its whole parent when it was already selected. */
    fun afterTap(subgroup: String): String? = if (subgroup == selectedSubgroup) selectedTop else subgroup
}

/**
 * What to select once [deleted] is gone, when it was the selection [selected]: its parent if it
 * was a Subgroup, so the feed stays on the parent instead of falling back to All; otherwise null,
 * leaving the original behaviour.
 */
fun selectionAfterDelete(
    deleted: String,
    selected: String?,
    groups: List<SubscriptionGroup>,
    links: Map<String, String>,
): String? = if (deleted == selected) SubgroupTree.of(groups, links).parentOf[deleted] else null
