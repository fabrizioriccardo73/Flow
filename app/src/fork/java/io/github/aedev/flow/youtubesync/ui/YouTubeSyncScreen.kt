package io.github.aedev.flow.youtubesync.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aedev.flow.R
import io.github.aedev.flow.ui.components.settings.SettingsPage
import io.github.aedev.flow.ui.components.settings.info
import io.github.aedev.flow.ui.components.settings.nav
import io.github.aedev.flow.ui.components.shared.FlowAlertDialog

/** The YouTube Sync page: link or unlink the profile's Linked Account (ticket 02). */
@Composable
fun YouTubeSyncScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    viewModel: YouTubeSyncViewModel = hiltViewModel(),
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val working by viewModel.working.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmUnlink by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.completeLink(result.data)
        }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text =
                when (message) {
                    LinkMessage.LINKED -> R.string.youtube_sync_linked
                    LinkMessage.UNLINKED -> R.string.youtube_sync_unlinked
                    LinkMessage.CANCELLED -> R.string.youtube_sync_link_cancelled
                    LinkMessage.NOT_CONFIGURED -> R.string.youtube_sync_not_configured
                    LinkMessage.OFFLINE -> R.string.youtube_sync_link_offline
                    LinkMessage.FAILED -> R.string.youtube_sync_link_failed
                }
            snackbarHostState.showSnackbar(context.getString(text))
        }
    }

    SettingsPage(
        title = stringResource(R.string.youtube_sync_title),
        onBack = onBack,
        highlight = highlight,
        snackbarHostState = snackbarHostState,
    ) {
        group(key = "youtube_sync.account.group", header = R.string.youtube_sync_account_section) {
            val linked = account
            if (linked == null) {
                nav(
                    YouTubeSyncIndex.link,
                    icon = Icons.Outlined.Link,
                    enabled = !working && viewModel.isConfigured,
                    showChevron = false,
                    value = if (viewModel.isConfigured) null else context.getString(R.string.youtube_sync_not_configured),
                    onClick = { viewModel.authorizationIntent()?.let(launcher::launch) },
                )
            } else {
                nav(
                    YouTubeSyncIndex.account,
                    icon = Icons.Outlined.AccountCircle,
                    enabled = !working,
                    showChevron = false,
                    value = context.getString(R.string.youtube_sync_linked_as, linked.channelTitle),
                    onClick = { confirmUnlink = true },
                )
            }
            info(
                key = "youtube_sync.account.privacy",
                title = context.getString(R.string.youtube_sync_privacy_title),
                value = context.getString(R.string.youtube_sync_privacy_body),
            )
        }
    }

    if (confirmUnlink) {
        FlowAlertDialog(
            onDismissRequest = { confirmUnlink = false },
            title = { Text(stringResource(R.string.youtube_sync_unlink_title)) },
            text = { Text(stringResource(R.string.youtube_sync_unlink_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnlink = false
                    viewModel.unlink()
                }) { Text(stringResource(R.string.youtube_sync_unlink)) }
            },
            dismissButton = { TextButton(onClick = { confirmUnlink = false }) { Text(stringResource(R.string.btn_cancel)) } },
        )
    }
}
