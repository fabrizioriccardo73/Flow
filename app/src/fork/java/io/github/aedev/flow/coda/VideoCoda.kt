package io.github.aedev.flow.coda

import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.di.ApplicationScope
import io.github.aedev.flow.player.GlobalPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The profile's Coda: the one place it changes, saved after every change.
 *
 * Changes are atomic, so callers on any thread can make them. A video that starts playing while
 * it is in the Coda becomes the one in riproduzione, whichever path started it.
 */
@Singleton
class VideoCoda
    @Inject
    constructor(
        private val store: CodaStore,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) {
        private val _state = MutableStateFlow(Coda())
        val state: StateFlow<Coda> = _state.asStateFlow()

        init {
            scope.launch {
                // Additions made before the saved Coda arrived go after it rather than replacing it.
                val saved = store.load()
                _state.update { early -> saved.mergedWith(early) }
                // A failed write must not stop the next ones; the following change saves the whole Coda again.
                state.collect { coda -> runCatching { store.save(coda) } }
            }
            scope.launch {
                GlobalPlayerState.currentVideo
                    .filterNotNull()
                    .map { video -> video.id }
                    .distinctUntilChanged()
                    .collect { id -> _state.update { coda -> coda.markPlaying(id) } }
            }
        }

        operator fun contains(videoId: String): Boolean = videoId in state.value

        /** @return false when [video] was already in the Coda. */
        fun append(video: Video): Boolean {
            var added = false
            _state.update { coda ->
                added = video.id !in coda
                coda.append(video)
            }
            return added
        }

        fun putFirst(video: Video) = _state.update { it.putFirst(video) }

        fun finish(videoId: String) = _state.update { it.finish(videoId) }

        fun move(
            from: Int,
            to: Int,
        ) = _state.update { it.move(from, to) }

        /** @return what an undo needs, or null when [videoId] was not in the Coda. */
        fun remove(videoId: String): RemovedFromCoda? {
            var removed: RemovedFromCoda? = null
            _state.update { coda ->
                val result = coda.remove(videoId)
                removed = result?.second
                result?.first ?: coda
            }
            return removed
        }

        fun restore(removed: RemovedFromCoda) = _state.update { it.restore(removed) }

        /** Empties the Coda. @return the Coda as it was, for an undo; null when it was already empty. */
        fun clear(): Coda? {
            val before = _state.getAndUpdate { Coda() }
            return before.takeUnless { it.isEmpty }
        }

        /** Puts back a Coda emptied by [clear], ahead of anything added since. */
        fun restoreAll(before: Coda) = _state.update { added -> before.mergedWith(added) }
    }
