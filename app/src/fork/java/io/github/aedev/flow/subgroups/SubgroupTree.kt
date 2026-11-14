package io.github.aedev.flow.subgroups

import io.github.aedev.flow.data.model.SubscriptionGroup

/**
 * The Groups arranged one level deep: each Subgroup under its parent. Built from the stored links
 * (Subgroup name to parent name) by [of]; links that do not hold are ignored, never rewritten.
 */
class SubgroupTree private constructor(
    private val groups: List<SubscriptionGroup>,
    /** Valid links only: Subgroup name to parent name. */
    val parentOf: Map<String, String>,
    /** Links between existing, different Groups, valid or not. */
    private val candidates: Map<String, String>,
) {
    fun isSubgroup(name: String): Boolean = name in parentOf

    /** [parent]'s Subgroups, in the Groups' own order. */
    fun subgroupsOf(parent: String): List<SubscriptionGroup> = groups.filter { parentOf[it.name] == parent }

    /** The Groups with no parent, in their own order. */
    fun topLevel(): List<SubscriptionGroup> = groups.filterNot { isSubgroup(it.name) }

    /** Whether some Group points at [name] as its parent, so [name] cannot become a Subgroup itself. */
    fun hasSubgroups(name: String): Boolean = candidates.values.any { it == name }

    /**
     * The Groups that [editing] (null for a new Group) can be put inside: the ones that point at no
     * parent themselves, so the new link is sure to hold. Never [editing] itself.
     */
    fun parentOptions(editing: String?): List<SubscriptionGroup> = groups.filter { it.name != editing && it.name !in candidates }

    companion object {
        /**
         * A link holds when both Groups exist, they differ, and the parent has no link of its own to
         * an existing Group. So after concurrent edits that would nest two levels (or a cycle), the
         * deeper Groups simply show as plain Groups, and a link to a deleted parent drops out.
         */
        fun of(
            groups: List<SubscriptionGroup>,
            links: Map<String, String>,
        ): SubgroupTree {
            val names = groups.mapTo(HashSet()) { it.name }
            val candidates = links.filter { (group, parent) -> group != parent && group in names && parent in names }
            val valid = candidates.filterValues { parent -> parent !in candidates }
            return SubgroupTree(groups, valid, candidates)
        }
    }
}

/**
 * [scope] (what the selected chip covers on its own) widened with the channels of [selected]'s
 * Subgroups; unchanged for every other selection, including All (null) and Ungrouped.
 */
fun scopeWithSubgroups(
    selected: String?,
    scope: Set<String>?,
    groups: List<SubscriptionGroup>,
    links: Map<String, String>,
): Set<String>? {
    if (selected == null || scope == null || links.isEmpty()) return scope
    val subgroups = SubgroupTree.of(groups, links).subgroupsOf(selected)
    if (subgroups.isEmpty()) return scope
    return subgroups.flatMapTo(LinkedHashSet(scope)) { it.channelIds }
}

/**
 * [items] (named by [name]) rearranged as the tree reads: each top-level item keeps its place among
 * the top-level ones and is followed by its Subgroups, which keep their order among themselves. An
 * item with no Group behind it, like Ungrouped, counts as top-level. Used both to show Manage groups
 * and to save a drag, so a Subgroup dragged out of its parent snaps back under it.
 */
fun <T> treeOrder(
    items: List<T>,
    links: Map<String, String>,
    name: (T) -> String,
): List<T> {
    if (links.isEmpty()) return items
    val tree = SubgroupTree.of(items.map { SubscriptionGroup(name(it), emptyList()) }, links)
    val children = items.filter { tree.isSubgroup(name(it)) }.groupBy { tree.parentOf.getValue(name(it)) }
    return items.filterNot { tree.isSubgroup(name(it)) }.flatMap { listOf(it) + children[name(it)].orEmpty() }
}

/**
 * [items], already in [treeOrder], after the item at [from] moved one step [up] or down: a Subgroup
 * swaps with its neighbouring sibling, a top-level item swaps with the neighbouring top-level item,
 * taking its Subgroups along. Unchanged when there is nowhere to go.
 */
fun <T> treeMove(
    items: List<T>,
    links: Map<String, String>,
    from: Int,
    up: Boolean,
    name: (T) -> String,
): List<T> {
    val tree = SubgroupTree.of(items.map { SubscriptionGroup(name(it), emptyList()) }, links)
    val moving = name(items[from])
    val parent = tree.parentOf[moving]
    if (parent != null) {
        val siblings = items.indices.filter { tree.parentOf[name(items[it])] == parent }
        val at = siblings.indexOf(from)
        val target = siblings.getOrNull(if (up) at - 1 else at + 1) ?: return items
        return items.toMutableList().apply { this[from] = items[target]; this[target] = items[from] }
    }
    val blocks = mutableListOf<MutableList<T>>()
    items.forEach { if (tree.isSubgroup(name(it))) blocks.last().add(it) else blocks.add(mutableListOf(it)) }
    val at = blocks.indexOfFirst { name(it.first()) == moving }
    val target = if (up) at - 1 else at + 1
    if (target !in blocks.indices) return items
    blocks.add(target, blocks.removeAt(at))
    return blocks.flatten()
}
