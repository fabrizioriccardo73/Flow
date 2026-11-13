package io.github.aedev.flow.youtubesync.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Sync
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
import io.github.aedev.flow.youtubesync.engine.SyncPlan
import io.github.aedev.flow.youtubesync.remote.YouTubeAccountException

/** The YouTube Sync page: link or unlink the profile's Linked Account (ticket 02). */
@Composable
fun YouTubeSyncScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    viewModel: YouTubeSyncViewModel = hiltViewModel(),
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val working by viewModel.working.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
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

    LaunchedEffect(viewModel) {
        viewModel.syncMessages.collect { message ->
            snackbarHostState.showSnackbar(syncMessageText(context, message))
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
        if (account != null) {
            group(key = "youtube_sync.sync.group", header = R.string.youtube_sync_sync_section) {
                nav(
                    YouTubeSyncIndex.syncNow,
                    icon = Icons.Outlined.Sync,
                    enabled = syncState == SyncUiState.Idle && !working,
                    showChevron = false,
                    value =
                        when (syncState) {
                            SyncUiState.Planning -> context.getString(R.string.youtube_sync_reading)
                            SyncUiState.Applying -> context.getString(R.string.youtube_sync_applying)
                            else -> null
                        },
                    onClick = viewModel::syncNow,
                )
            }
        }
    }

    (syncState as? SyncUiState.Preview)?.let { preview ->
        SyncPreviewDialog(
            plan = preview.plan,
            onConfirm = viewModel::confirmSync,
            onDismiss = viewModel::cancelSync,
        )
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

@Composable
private fun SyncPreviewDialog(
    plan: SyncPlan,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    FlowAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (plan.isFirstLink) R.string.youtube_sync_preview_first_title else R.string.youtube_sync_preview_title))
        },
        text = {
            Text(
                if (plan.isFirstLink) {
                    stringResource(
                        R.string.youtube_sync_preview_body,
                        plan.addToYouTube.size,
                        plan.addToFlow.size,
                        plan.alreadyOnBoth.size,
                    )
                } else {
                    stringResource(
                        R.string.youtube_sync_preview_changes_body,
                        plan.addToYouTube.size,
                        plan.removeFromYouTube.size,
                        plan.addToFlow.size,
                        plan.removeFromFlow.size,
                    )
                },
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.youtube_sync_preview_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } },
    )
}

private fun syncMessageText(
    context: Context,
    message: SyncMessage,
): String =
    when (message) {
        SyncMessage.NothingToDo -> {
            context.getString(R.string.youtube_sync_nothing_to_do)
        }

        is SyncMessage.Done -> {
            val result = message.result
            val base =
                context.getString(
                    R.string.youtube_sync_done_changes,
                    result.addedToYouTube,
                    result.removedFromYouTube,
                    result.addedToFlow,
                    result.removedFromFlow,
                )
            when {
                result.stoppedBy != null -> base + " " + errorText(context, result.stoppedBy)
                result.rejected.isNotEmpty() -> base + " " + context.getString(R.string.youtube_sync_rejected, result.rejected.size)
                else -> base
            }
        }

        is SyncMessage.Failed -> {
            errorText(context, message.error)
        }
    }

private fun errorText(
    context: Context,
    error: YouTubeAccountException,
): String =
    context.getString(
        when (error) {
            is YouTubeAccountException.QuotaExceeded -> R.string.youtube_sync_error_quota
            is YouTubeAccountException.AuthorizationInvalid, is YouTubeAccountException.NotLinked -> R.string.youtube_sync_error_auth
            is YouTubeAccountException.Transient -> R.string.youtube_sync_error_offline
            is YouTubeAccountException.ItemRejected -> R.string.youtube_sync_error_generic
        },
    )
