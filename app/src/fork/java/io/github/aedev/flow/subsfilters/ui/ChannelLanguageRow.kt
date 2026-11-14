package io.github.aedev.flow.subsfilters.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aedev.flow.R
import io.github.aedev.flow.subsfilters.language.ChannelLanguages
import io.github.aedev.flow.subsfilters.language.Languages
import io.github.aedev.flow.ui.components.shared.FlowAlertDialog
import io.github.aedev.flow.ui.components.shared.FlowNavRow
import io.github.aedev.flow.ui.components.shared.FlowSelectionRow
import io.github.aedev.flow.youtubesync.account.ThisDevice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val DialogListMaxHeight = 360.dp

@HiltViewModel
class ChannelLanguageViewModel
    @Inject
    constructor(
        private val channelLanguages: ChannelLanguages,
    ) : ViewModel() {
        val languages: StateFlow<Map<String, String>> =
            channelLanguages.languages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyMap())

        fun assign(
            channelId: String,
            language: String?,
        ) {
            viewModelScope.launch { channelLanguages.assign(channelId, language) }
        }
    }

/**
 * The "Language" row at the top of a channel's group sheet. Phone only: on a TV the languages arrive
 * through the Device Sync and are not assigned there.
 */
@Composable
fun ChannelLanguageRow(
    channelId: String,
    viewModel: ChannelLanguageViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val isTelevision = remember(context) { ThisDevice.from(context).isTelevision }
    if (isTelevision || channelId.isBlank()) return
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    val current = languages[channelId]
    var picking by remember { mutableStateOf(false) }
    FlowNavRow(
        title = stringResource(R.string.subsfilters_channel_language),
        supportingText = current?.let(Languages::displayName) ?: stringResource(R.string.subsfilters_channel_language_detect),
        leadingIcon = Icons.Outlined.Language,
        onClick = { picking = true },
    )
    HorizontalDivider()
    if (picking) {
        FlowAlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(stringResource(R.string.subsfilters_channel_language)) },
            text = {
                Column(modifier = Modifier.heightIn(max = DialogListMaxHeight).verticalScroll(rememberScrollState())) {
                    FlowSelectionRow(
                        title = stringResource(R.string.subsfilters_channel_language_detect),
                        supportingText = stringResource(R.string.subsfilters_channel_language_detect_body),
                        selected = current == null,
                        onClick = {
                            viewModel.assign(channelId, null)
                            picking = false
                        },
                    )
                    Languages.ASSIGNABLE.forEach { code ->
                        FlowSelectionRow(
                            title = Languages.displayName(code),
                            selected = current == code,
                            onClick = {
                                viewModel.assign(channelId, code)
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
