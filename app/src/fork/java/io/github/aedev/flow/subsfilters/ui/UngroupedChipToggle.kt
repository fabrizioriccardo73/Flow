package io.github.aedev.flow.subsfilters.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aedev.flow.R
import io.github.aedev.flow.data.local.PlayerPreferences
import io.github.aedev.flow.ui.components.shared.FlowSwitchRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The Ungrouped chip's switch, the same preference as Settings → Content → Ungrouped group. */
@HiltViewModel
class UngroupedChipToggleViewModel
    @Inject
    constructor(
        private val preferences: PlayerPreferences,
    ) : ViewModel() {
        val enabled: StateFlow<Boolean> =
            preferences.subscriptionUngroupedEnabled
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

        fun setEnabled(value: Boolean) {
            viewModelScope.launch { preferences.setSubscriptionUngroupedEnabled(value) }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

/** Shows or hides the Ungrouped chip from Manage groups, so it is reachable without Settings. */
@Composable
fun UngroupedChipToggle(
    modifier: Modifier = Modifier,
    viewModel: UngroupedChipToggleViewModel = hiltViewModel(),
) {
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    FlowSwitchRow(
        title = stringResource(R.string.content_settings_subs_ungrouped_title),
        supportingText = stringResource(R.string.content_settings_subs_ungrouped_subtitle),
        checked = enabled,
        onCheckedChange = viewModel::setEnabled,
        modifier = modifier,
    )
}
