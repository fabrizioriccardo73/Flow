package io.github.aedev.flow.subsfilters.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aedev.flow.R
import io.github.aedev.flow.subsfilters.filter.LanguageFilter
import io.github.aedev.flow.subsfilters.language.Languages
import io.github.aedev.flow.ui.components.shared.FlowAlertDialog
import io.github.aedev.flow.ui.components.shared.FlowSwitchRow
import io.github.aedev.flow.ui.screens.subscriptions.SubscriptionsUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L
private val ChipIconSize = 18.dp
private val DialogListMaxHeight = 360.dp

@HiltViewModel
class LanguageFilterViewModel
    @Inject
    constructor(
        val filter: LanguageFilter,
    ) : ViewModel() {
        val selected: StateFlow<Set<String>?> =
            filter.selected.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

        val channelLanguages: StateFlow<Map<String, String>> =
            filter.channelLanguages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyMap())

        fun select(languages: Set<String>) {
            viewModelScope.launch { filter.select(languages) }
        }
    }

/**
 * [state] with its videos and Shorts narrowed by the Language filter. Until the saved selection has
 * loaded nothing is hidden, so the feed never flashes empty.
 */
@Composable
fun rememberLanguageFiltered(
    state: SubscriptionsUiState,
    viewModel: LanguageFilterViewModel = hiltViewModel(),
): SubscriptionsUiState {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val channelLanguages by viewModel.channelLanguages.collectAsStateWithLifecycle()
    val result =
        remember(state, selected, channelLanguages) {
            val chosen = selected.orEmpty()
            val (videos, videoLanguages) = viewModel.filter.apply(state.recentVideos, chosen, channelLanguages)
            val (shorts, shortLanguages) = viewModel.filter.apply(state.shorts, chosen, channelLanguages)
            state.copy(recentVideos = videos, shorts = shorts) to (videoLanguages + shortLanguages + chosen)
        }
    SideEffect { viewModel.filter.reportAvailable(result.second) }
    return result.first
}

/** The globe chip leading the group chips: the active languages, and a tap to change them. */
@Composable
fun LanguageFilterChip(viewModel: LanguageFilterViewModel = hiltViewModel()) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val available by viewModel.filter.available.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    val chosen = selected.orEmpty()
    FilterChip(
        selected = chosen.isNotEmpty(),
        onClick = { open = true },
        label = {
            Text(
                text = if (chosen.isEmpty()) stringResource(R.string.subsfilters_language_chip) else Languages.shortLabel(chosen),
                maxLines = 1,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                modifier = Modifier.size(ChipIconSize),
            )
        },
        colors = FilterChipDefaults.filterChipColors(),
    )
    if (open) {
        LanguageFilterDialog(
            available = Languages.sorted(available + chosen),
            initial = chosen,
            onConfirm = {
                viewModel.select(it)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

@Composable
private fun LanguageFilterDialog(
    available: List<String>,
    initial: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by remember(initial) { mutableStateOf(initial) }
    FlowAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.subsfilters_language_dialog_title)) },
        text = {
            Column(modifier = Modifier.heightIn(max = DialogListMaxHeight).verticalScroll(rememberScrollState())) {
                if (available.isEmpty()) {
                    Text(
                        text = stringResource(R.string.subsfilters_language_none_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                available.forEach { code ->
                    FlowSwitchRow(
                        title = Languages.displayName(code),
                        checked = code in picked,
                        onCheckedChange = { on -> picked = if (on) picked + code else picked - code },
                    )
                }
                Text(
                    text = stringResource(R.string.subsfilters_language_unknown_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(picked) }) { Text(stringResource(R.string.subsfilters_language_apply)) }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(emptySet()) }) { Text(stringResource(R.string.subsfilters_language_show_all)) }
        },
    )
}
