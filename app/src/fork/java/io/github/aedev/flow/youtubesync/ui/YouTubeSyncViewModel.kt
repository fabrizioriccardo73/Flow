package io.github.aedev.flow.youtubesync.ui

import android.content.Intent
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aedev.flow.ui.screens.settings.SettingsViewModel
import io.github.aedev.flow.youtubesync.account.LinkedAccount
import io.github.aedev.flow.youtubesync.account.LinkedAccountException
import io.github.aedev.flow.youtubesync.account.LinkedAccountManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What happened to the last link or unlink, for a snackbar. */
enum class LinkMessage { LINKED, UNLINKED, CANCELLED, NOT_CONFIGURED, FAILED, OFFLINE }

@HiltViewModel
class YouTubeSyncViewModel
    @Inject
    constructor(
        private val manager: LinkedAccountManager,
    ) : SettingsViewModel() {
        val account: StateFlow<LinkedAccount?> = manager.account.asState(null)

        val isConfigured: Boolean get() = manager.isConfigured

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
                runCatching { manager.completeLink(result) }
                    .onSuccess { _messages.send(LinkMessage.LINKED) }
                    .onFailure { _messages.send(it.toMessage()) }
                _working.value = false
            }
        }

        fun unlink() {
            _working.value = true
            viewModelScope.launch {
                manager.unlink()
                _messages.send(LinkMessage.UNLINKED)
                _working.value = false
            }
        }

        private fun Throwable.toMessage(): LinkMessage =
            when (this) {
                is LinkedAccountException.Cancelled -> LinkMessage.CANCELLED
                is LinkedAccountException.NotConfigured -> LinkMessage.NOT_CONFIGURED
                is LinkedAccountException.Transient -> LinkMessage.OFFLINE
                else -> LinkMessage.FAILED
            }
    }
