package io.github.aedev.flow.subgroups.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aedev.flow.R
import io.github.aedev.flow.data.model.SubscriptionGroup
import io.github.aedev.flow.subgroups.SubgroupLinks
import io.github.aedev.flow.subgroups.SubgroupTree
import io.github.aedev.flow.subgroups.treeMove
import io.github.aedev.flow.subgroups.treeOrder
import io.github.aedev.flow.ui.components.shared.FlowAlertDialog
import io.github.aedev.flow.ui.components.shared.FlowNavRow
import io.github.aedev.flow.ui.components.shared.FlowSelectionRow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val DialogListMaxHeight = 360.dp
private val SubgroupIndent = 24.dp

/** Saves the Subgroup links that follow an edit in Manage groups; outlives the dialogs that call it. */
@HiltViewModel
class SubgroupEditsViewModel
    @Inject
    constructor(
        private val links: SubgroupLinks,
    ) : ViewModel() {
        fun saved(
            oldName: String?,
            newName: String,
            parent: String?,
        ) {
            viewModelScope.launch { links.saved(oldName, newName, parent) }
        }

        fun deleted(name: String) {
            viewModelScope.launch { links.deleted(name) }
        }
    }

/** How Manage groups lays out and moves its rows as a tree. Rows are named by [SubscriptionGroup.name]. */
class SubgroupArrangement(
    private val links: Map<String, String>,
) {
    fun arrange(rows: List<SubscriptionGroup>): List<SubscriptionGroup> = treeOrder(rows, links) { it.name }

    fun arrangeNames(names: List<String>): List<String> = treeOrder(names, links) { it }

    fun move(
        rows: List<SubscriptionGroup>,
        from: Int,
        up: Boolean,
    ): List<SubscriptionGroup> = treeMove(rows, links, from, up) { it.name }

    fun isSubgroup(
        rows: List<SubscriptionGroup>,
        name: String,
    ): Boolean = SubgroupTree.of(rows, links).isSubgroup(name)
}

/** Shifts a Subgroup's row in Manage groups to the right of its parent. */
fun Modifier.subgroupIndent(isSubgroup: Boolean): Modifier = if (isSubgroup) padding(start = SubgroupIndent) else this

/** The parent chosen while creating or editing [editing] (null for a new Group), starting from its current one. */
@Composable
fun rememberSubgroupParent(
    editing: SubscriptionGroup?,
    groups: List<SubscriptionGroup>,
    links: Map<String, String>,
): MutableState<String?> =
    remember(editing?.name) {
        mutableStateOf(editing?.let { SubgroupTree.of(groups, links).parentOf[it.name] })
    }

/** The "Inside" row of the create or edit dialog: which Group, if any, [editing] sits in. */
@Composable
fun SubgroupParentRow(
    editing: String?,
    groups: List<SubscriptionGroup>,
    links: Map<String, String>,
    parent: MutableState<String?>,
) {
    val tree = remember(groups, links) { SubgroupTree.of(groups, links) }
    val hasSubgroups = editing != null && tree.hasSubgroups(editing)
    val options = remember(tree, editing) { tree.parentOptions(editing) }
    var picking by remember { mutableStateOf(false) }
    val none = stringResource(R.string.subgroups_parent_none)
    FlowNavRow(
        title = stringResource(R.string.subgroups_parent),
        supportingText = if (hasSubgroups) stringResource(R.string.subgroups_parent_has_subgroups) else parent.value ?: none,
        leadingIcon = Icons.Outlined.AccountTree,
        enabled = !hasSubgroups && options.isNotEmpty(),
        onClick = { picking = true },
    )
    if (picking) {
        FlowAlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(stringResource(R.string.subgroups_parent)) },
            text = {
                Column(modifier = Modifier.heightIn(max = DialogListMaxHeight).verticalScroll(rememberScrollState())) {
                    FlowSelectionRow(
                        title = none,
                        selected = parent.value == null,
                        onClick = {
                            parent.value = null
                            picking = false
                        },
                    )
                    options.forEach { group ->
                        FlowSelectionRow(
                            title = group.name,
                            selected = parent.value == group.name,
                            onClick = {
                                parent.value = group.name
                                picking = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { picking = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }
}

/**
 * Asks before deleting a Group that has Subgroups, saying they stay; any other Group is deleted at
 * once, as before. Call [request] with the Group to delete.
 */
class SubgroupDeleteGuard(
    private val pending: MutableState<SubscriptionGroup?>,
    private val subgroupCount: (SubscriptionGroup) -> Int,
    private val delete: (SubscriptionGroup) -> Unit,
) {
    fun request(group: SubscriptionGroup) {
        if (subgroupCount(group) > 0) pending.value = group else delete(group)
    }

    @Composable
    fun Dialog() {
        val group = pending.value ?: return
        val count = subgroupCount(group)
        FlowAlertDialog(
            onDismissRequest = { pending.value = null },
            title = { Text(stringResource(R.string.subgroups_delete_title, group.name)) },
            text = {
                Text(
                    text = pluralStringResource(R.plurals.subgroups_delete_body, count, count),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending.value = null
                    delete(group)
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pending.value = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
fun rememberSubgroupDeleteGuard(
    groups: List<SubscriptionGroup>,
    links: Map<String, String>,
    delete: (SubscriptionGroup) -> Unit,
): SubgroupDeleteGuard {
    val pending = remember { mutableStateOf<SubscriptionGroup?>(null) }
    val tree = remember(groups, links) { SubgroupTree.of(groups, links) }
    return SubgroupDeleteGuard(pending, { tree.subgroupsOf(it.name).size }, delete)
}
