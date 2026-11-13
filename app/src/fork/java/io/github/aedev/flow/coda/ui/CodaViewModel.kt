package io.github.aedev.flow.coda.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aedev.flow.coda.VideoCoda
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val SHARING_TIMEOUT_MS = 5_000L

/** What the app shell needs of the Coda: how many videos it holds, and whether its sheet is open. */
@HiltViewModel
class CodaViewModel
    @Inject
    constructor(
        coda: VideoCoda,
    ) : ViewModel() {
        val count: StateFlow<Int> =
            coda.state
                .map { it.videos.size }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SHARING_TIMEOUT_MS), coda.state.value.videos.size)

        private val _sheetOpen = MutableStateFlow(false)
        val sheetOpen: StateFlow<Boolean> = _sheetOpen.asStateFlow()

        fun openSheet() {
            _sheetOpen.value = true
        }

        fun closeSheet() {
            _sheetOpen.value = false
        }
    }
