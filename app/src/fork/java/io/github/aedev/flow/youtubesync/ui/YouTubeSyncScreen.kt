package io.github.aedev.flow.youtubesync.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aedev.flow.R
import io.github.aedev.flow.ui.components.settings.SettingsPage
import io.github.aedev.flow.ui.components.settings.info
import io.github.aedev.flow.ui.components.settings.nav
import io.github.aedev.flow.ui.components.shared.FlowAlertDialog
import io.github.aedev.flow.youtubesync.engine.MassRemoval
import io.github.aedev.flow.youtubesync.engine.SyncCollection
import io.github.aedev.flow.youtubesync.engine.SyncPlan
import io.github.aedev.flow.youtubesync.engine.SyncSide
import io.github.aedev.flow.youtubesync.local.LocalPlaylist
import io.github.aedev.flow.youtubesync.remote.YouTubeAccountException
import io.github.aedev.flow.youtubesync.runtime.SyncProblem
import io.github.aedev.flow.youtubesync.runtime.SyncStatus
import java.text.DateFormat
import java.util.Date

/** The YouTube Sync page: the Linked Account, the status, "Sincronizza ora" and linking a playlist. */
@Composable
fun YouTubeSyncScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    viewModel: YouTubeSyncViewModel = hiltViewModel(),
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val working by viewModel.working.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val running by viewModel.running.collectAsStateWithLifecycle()
    val flowOnly by viewModel.flowOnly.collectAsStateWithLifecycle()
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

    // Opened from the "needs your OK" notification: show the preview with the removals straight away.
    LaunchedEffect(status.problem, account) {
        if (account != null && status.problem == SyncProblem.NEEDS_CONFIRMATION) viewModel.syncNow()
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
                    YouTubeSyncIndex.status,
                    icon = Icons.Outlined.CloudSync,
                    enabled = !working,
                    showChevron = false,
                    value = statusText(context, status, running),
                    onClick = {
                        when (status.problem) {
                            SyncProblem.NEEDS_RELINK -> viewModel.authorizationIntent()?.let(launcher::launch)
                            else -> viewModel.syncNow()
                        }
                    },
                )
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
                nav(
                    YouTubeSyncIndex.linkPlaylist,
                    icon = Icons.Outlined.PlaylistAdd,
                    enabled = !working,
                    showChevron = false,
                    onClick = viewModel::pickPlaylistToLink,
                )
                info(
                    key = "youtube_sync.sync.saved_playlists",
                    title = context.getString(R.string.youtube_sync_saved_playlists_title),
                    value = context.getString(R.string.youtube_sync_saved_playlists_body),
                )
            }
        }
    }

    flowOnly?.let { candidates ->
        LinkPlaylistDialog(
            playlists = candidates,
            onLink = viewModel::linkPlaylist,
            onDismiss = viewModel::closePlaylistPicker,
        )
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
    val warning = plan.massRemovals.isNotEmpty()
    FlowAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    when {
                        warning -> R.string.youtube_sync_mass_removal_title
                        plan.isFirstLink -> R.string.youtube_sync_preview_first_title
                        else -> R.string.youtube_sync_preview_title
                    },
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (warning) {
                    plan.massRemovals.forEach { removal -> MassRemovalWarning(removal) }
                }
                PlaylistPreview(plan)
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
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                if (warning) {
                    Text(stringResource(R.string.youtube_sync_mass_removal_confirm), color = MaterialTheme.colorScheme.error)
                } else {
                    Text(stringResource(R.string.youtube_sync_preview_confirm))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } },
    )
}

/** Which side loses how many items, with the first few names, so a lost-data accident is recognisable. */
@Composable
private fun MassRemovalWarning(removal: MassRemoval) {
    val side =
        stringResource(
            when (removal.side) {
                SyncSide.FLOW -> R.string.youtube_sync_side_flow
                SyncSide.YOUTUBE -> R.string.youtube_sync_side_youtube
            },
        )
    val shown = removal.items.take(MAX_NAMES_SHOWN)
    val more = removal.count - shown.size
    Text(
        when (removal.collection) {
            SyncCollection.SUBSCRIPTIONS -> {
                stringResource(R.string.youtube_sync_mass_removal_body, removal.count, removal.total, side)
            }

            SyncCollection.PLAYLISTS -> {
                stringResource(R.string.youtube_sync_mass_removal_playlists_body, removal.count, removal.total, side)
            }

            SyncCollection.PLAYLIST_VIDEOS -> {
                stringResource(
                    R.string.youtube_sync_mass_removal_videos_body,
                    removal.count,
                    removal.total,
                    removal.container.orEmpty(),
                    side,
                )
            }
        },
        color = MaterialTheme.colorScheme.error,
    )
    Text(
        shown.joinToString(", ") + if (more > 0) " " + stringResource(R.string.youtube_sync_mass_removal_more, more) else "",
        style = MaterialTheme.typography.bodySmall,
    )
}

private const val MAX_NAMES_SHOWN = 8

/** What happens to playlists, and titles a Flow-only playlist already uses (kept apart, never merged). */
@Composable
private fun PlaylistPreview(plan: SyncPlan) {
    val playlists = plan.playlists
    if (playlists.isEmpty) return
    Text(
        stringResource(
            R.string.youtube_sync_preview_playlists,
            playlists.importToFlow.size,
            playlists.changed.size,
            playlists.deleteFromFlow.size,
            playlists.deleteFromYouTube.size,
        ),
    )
    if (playlists.sameTitles.isNotEmpty()) {
        Text(
            stringResource(R.string.youtube_sync_preview_same_title, playlists.sameTitles.joinToString(", ")),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** Picks a Flow-only playlist and confirms that a new private playlist is created on YouTube (ticket 10). */
@Composable
private fun LinkPlaylistDialog(
    playlists: List<LocalPlaylist>,
    onLink: (LocalPlaylist) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosen by remember { mutableStateOf<LocalPlaylist?>(null) }
    val selected = chosen
    FlowAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.youtube_sync_link_playlist_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.youtube_sync_link_playlist_body), style = MaterialTheme.typography.bodySmall)
                if (playlists.isEmpty()) {
                    Text(stringResource(R.string.youtube_sync_link_playlist_none))
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(playlists, key = { it.localId }) { playlist ->
                            Text(
                                stringResource(R.string.youtube_sync_link_playlist_item, playlist.title, playlist.videos.size),
                                color =
                                    if (playlist ==
                                        selected
                                    ) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { chosen = playlist }
                                        .padding(vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = selected != null, onClick = { selected?.let(onLink) }) {
                Text(stringResource(R.string.youtube_sync_link_playlist_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } },
    )
}

/** The status indicator: the last successful YouTube Sync, or why it is not running. */
private fun statusText(
    context: Context,
    status: SyncStatus,
    running: Boolean,
): String {
    val last = status.lastSuccessAt?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it)) }
    return when {
        running -> {
            context.getString(R.string.youtube_sync_status_running)
        }

        status.problem == SyncProblem.NEEDS_RELINK -> {
            context.getString(R.string.youtube_sync_status_relink)
        }

        status.problem == SyncProblem.NEEDS_CONFIRMATION -> {
            context.getString(R.string.youtube_sync_status_confirm)
        }

        status.problem == SyncProblem.QUOTA -> {
            context.getString(
                R.string.youtube_sync_status_quota,
                status.retryAt?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it)) }.orEmpty(),
            )
        }

        status.problem == SyncProblem.OFFLINE -> {
            context.getString(R.string.youtube_sync_status_offline)
        }

        last != null -> {
            context.getString(R.string.youtube_sync_status_ok, last)
        }

        else -> {
            context.getString(R.string.youtube_sync_status_never)
        }
    }
}

private fun syncMessageText(
    context: Context,
    message: SyncMessage,
): String =
    when (message) {
        SyncMessage.NothingToDo -> {
            context.getString(R.string.youtube_sync_nothing_to_do)
        }

        is SyncMessage.PlaylistLinked -> {
            context.getString(R.string.youtube_sync_playlist_linked, message.title)
        }

        is SyncMessage.Done -> {
            val result = message.result
            val playlists = result.playlists
            val base =
                context.getString(
                    R.string.youtube_sync_done_changes,
                    result.addedToYouTube,
                    result.removedFromYouTube,
                    result.addedToFlow,
                    result.removedFromFlow,
                ) +
                    if (playlists.imported + playlists.updated + playlists.deletedFromFlow + playlists.deletedFromYouTube > 0) {
                        " " +
                            context.getString(
                                R.string.youtube_sync_done_playlists,
                                playlists.updated,
                                playlists.imported,
                                playlists.deletedFromFlow + playlists.deletedFromYouTube,
                            )
                    } else {
                        ""
                    }
            when {
                result.stoppedBy != null -> {
                    base + " " + errorText(context, result.stoppedBy)
                }

                result.rejected.isNotEmpty() || result.playlists.rejected > 0 -> {
                    base + " " +
                        context.getString(R.string.youtube_sync_rejected, result.rejected.size + result.playlists.rejected)
                }

                else -> {
                    base
                }
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
