package io.github.aedev.flow.youtubesync.ui

import android.content.Intent
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aedev.flow.ui.screens.settings.SettingsViewModel
import io.github.aedev.flow.youtubesync.account.LinkedAccount
import io.github.aedev.flow.youtubesync.account.LinkedAccountException
import io.github.aedev.flow.youtubesync.account.LinkedAccountManager
import io.github.aedev.flow.youtubesync.engine.SyncPlan
import io.github.aedev.flow.youtubesync.engine.SyncResult
import io.github.aedev.flow.youtubesync.local.LocalPlaylist
import io.github.aedev.flow.youtubesync.local.LocalPlaylistKind
import io.github.aedev.flow.youtubesync.local.LocalPlaylists
import io.github.aedev.flow.youtubesync.remote.YouTubeAccountException
import io.github.aedev.flow.youtubesync.runtime.SyncStatus
import io.github.aedev.flow.youtubesync.runtime.YouTubeSyncRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What happened to the last link or unlink, for a snackbar. */
enum class LinkMessage { LINKED, UNLINKED, CANCELLED, NOT_CONFIGURED, USE_PHONE, FAILED, OFFLINE }

/** Where "Sync now" stands. */
sealed interface SyncUiState {
    data object Idle : SyncUiState

    data object Planning : SyncUiState

    /** Waiting for the viewer to confirm [plan]. */
    data class Preview(
        val plan: SyncPlan,
    ) : SyncUiState

    data object Applying : SyncUiState
}

/** How the last YouTube Sync ended, for a snackbar. */
sealed interface SyncMessage {
    data class Done(
        val result: SyncResult,
    ) : SyncMessage

    data object NothingToDo : SyncMessage

    data class PlaylistLinked(
        val title: String,
    ) : SyncMessage

    data class Failed(
        val error: YouTubeAccountException,
    ) : SyncMessage
}

@HiltViewModel
class YouTubeSyncViewModel
    @Inject
    constructor(
        private val manager: LinkedAccountManager,
        private val runner: YouTubeSyncRunner,
        private val playlists: LocalPlaylists,
    ) : SettingsViewModel() {
        val account: StateFlow<LinkedAccount?> = manager.account.asState(null)

        /** For the status indicator. */
        val status: StateFlow<SyncStatus> = runner.state.asState(SyncStatus())

        /** True while any YouTube Sync runs, the background ones included. */
        val running: StateFlow<Boolean> = runner.running

        val isConfigured: Boolean get() = manager.isConfigured

        /** False on a TV, where the screen explains to link the account on the phone. */
        val canLinkHere: Boolean get() = manager.canLinkHere

        private val _working = MutableStateFlow(false)
        val working: StateFlow<Boolean> = _working.asStateFlow()

        private val _messages = Channel<LinkMessage>(Channel.BUFFERED)
        val messages = _messages.receiveAsFlow()

        /** The browser intent to launch, or null (with a message) when the build has no OAuth client. */
        fun authorizationIntent(): Intent? =
            runCatching { manager.authorizationIntent() }
                .onFailure { _messages.trySend(it.toMessage()) }
                .getOrNull()

        fun completeLink(result: Intent?) {
            _working.value = true
            viewModelScope.launch {
                runCatching {
                    val previous = manager.account.first()
                    runner.onLinked(previous, manager.completeLink(result))
                }.onSuccess { _messages.send(LinkMessage.LINKED) }
                    .onFailure { _messages.send(it.toMessage()) }
                _working.value = false
            }
        }

        fun unlink() {
            _working.value = true
            viewModelScope.launch {
                manager.unlink()
                runner.onUnlinked()
                _messages.send(LinkMessage.UNLINKED)
                _working.value = false
            }
        }

        private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
        val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

        private val _syncMessages = Channel<SyncMessage>(Channel.BUFFERED)
        val syncMessages = _syncMessages.receiveAsFlow()

        /** "Sync now": reads both sides and shows what would change, or says there is nothing to do. */
        fun syncNow() {
            if (_syncState.value != SyncUiState.Idle) return
            _syncState.value = SyncUiState.Planning
            viewModelScope.launch {
                try {
                    val plan = runner.plan()
                    if (plan.isEmpty) {
                        runner.apply(plan, massRemovalConfirmed = false) // records the baseline even when both sides already agree
                        _syncMessages.send(SyncMessage.NothingToDo)
                        _syncState.value = SyncUiState.Idle
                    } else {
                        _syncState.value = SyncUiState.Preview(plan)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _syncMessages.send(SyncMessage.Failed(e.asAccountError()))
                    _syncState.value = SyncUiState.Idle
                }
            }
        }

        fun confirmSync() {
            val preview = _syncState.value as? SyncUiState.Preview ?: return
            _syncState.value = SyncUiState.Applying
            viewModelScope.launch {
                try {
                    // The preview showed the mass-removal warning, so confirming it confirms that too.
                    _syncMessages.send(SyncMessage.Done(runner.apply(preview.plan, massRemovalConfirmed = true)))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _syncMessages.send(SyncMessage.Failed(e.asAccountError()))
                }
                _syncState.value = SyncUiState.Idle
            }
        }

        /** Closing the preview applies nothing. */
        fun cancelSync() {
            if (_syncState.value is SyncUiState.Preview) _syncState.value = SyncUiState.Idle
        }

        private val _flowOnly = MutableStateFlow<List<LocalPlaylist>?>(null)

        /** The Flow-only playlists that can be linked, while the picker is open; null when closed. */
        val flowOnly: StateFlow<List<LocalPlaylist>?> = _flowOnly.asStateFlow()

        fun pickPlaylistToLink() {
            viewModelScope.launch {
                _flowOnly.value = playlists.all().filter { it.kind == LocalPlaylistKind.FLOW_ONLY }.sortedBy { it.title.lowercase() }
            }
        }

        fun closePlaylistPicker() {
            _flowOnly.value = null
        }

        /** "Collega a YouTube" (ticket 10). */
        fun linkPlaylist(playlist: LocalPlaylist) {
            _flowOnly.value = null
            _working.value = true
            viewModelScope.launch {
                try {
                    runner.linkPlaylist(playlist.localId)
                    _syncMessages.send(SyncMessage.PlaylistLinked(playlist.title))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _syncMessages.send(SyncMessage.Failed(e.asAccountError()))
                }
                _working.value = false
            }
        }

        private fun Exception.asAccountError(): YouTubeAccountException =
            this as? YouTubeAccountException ?: YouTubeAccountException.Transient(this)

        private fun Throwable.toMessage(): LinkMessage =
            when (this) {
                is LinkedAccountException.Cancelled -> LinkMessage.CANCELLED
                is LinkedAccountException.NotConfigured -> LinkMessage.NOT_CONFIGURED
                is LinkedAccountException.NotOnTelevision -> LinkMessage.USE_PHONE
                is LinkedAccountException.Transient -> LinkMessage.OFFLINE
                else -> LinkMessage.FAILED
            }
    }
