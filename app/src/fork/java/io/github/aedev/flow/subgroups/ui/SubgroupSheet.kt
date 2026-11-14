package io.github.aedev.flow.subgroups.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.model.SubscriptionGroup
import io.github.aedev.flow.subgroups.SubgroupLinks
import io.github.aedev.flow.subgroups.SubgroupSheetLayout

private val SubgroupIndent = 24.dp

/** [groups] laid out as the tree, for the channel's group sheet; the links are read from the fork's store. */
@Composable
fun rememberSubgroupSheetLayout(groups: List<SubscriptionGroup>): SubgroupSheetLayout {
    val linksFlow = remember { SubgroupLinks.observe() }
    val links by linksFlow.collectAsState(initial = emptyMap())
    return remember(groups, links) { SubgroupSheetLayout(groups, links) }
}

/** The row modifier for the sheet entry of the Group [name]: indented under its parent when it is a Subgroup. */
fun SubgroupSheetLayout.rowModifier(name: String): Modifier = if (indented(name)) Modifier.padding(start = SubgroupIndent) else Modifier
