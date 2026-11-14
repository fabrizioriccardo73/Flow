package io.github.aedev.flow.subgroups.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.subgroups.SubgroupChips
import io.github.aedev.flow.ui.components.shared.FlowFilterChip

private val RowHorizontalPadding = 12.dp
private val RowBottomPadding = 8.dp
private val ChipSpacing = 8.dp

/**
 * The second row of group chips, under the main one: the selected parent's Subgroups. Nothing when
 * the selected chip has none. A tap narrows the feed to that Subgroup, a second tap widens it back.
 *
 * With the D-pad (TV), moving down from the main row lands on the chip focused last in this row, or
 * on the selected Subgroup (the first one when none is selected), rather than on whichever chip
 * happens to sit under the focused one.
 */
@Composable
fun SubgroupChipRow(
    chips: SubgroupChips,
    onGroupSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chips.subgroupRow.isEmpty()) return
    val entry = remember { FocusRequester() }
    val entryName = chips.selectedSubgroup ?: chips.subgroupRow.first().name
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .focusRestorer(entry)
                .focusGroup()
                .horizontalScroll(rememberScrollState())
                .padding(start = RowHorizontalPadding, end = RowHorizontalPadding, bottom = RowBottomPadding),
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.subgroupRow.forEach { group ->
            FlowFilterChip(
                label = group.name,
                selected = chips.selectedSubgroup == group.name,
                onClick = { onGroupSelected(chips.afterTap(group.name)) },
                modifier = if (group.name == entryName) Modifier.focusRequester(entry) else Modifier,
            )
        }
    }
}
