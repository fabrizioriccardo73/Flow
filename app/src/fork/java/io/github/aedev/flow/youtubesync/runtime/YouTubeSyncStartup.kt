package io.github.aedev.flow.youtubesync.runtime

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.aedev.flow.data.local.AppDatabase
import io.github.aedev.flow.data.local.SubscriptionRepository
import io.github.aedev.flow.youtubesync.account.LinkedAccountStore
import io.github.aedev.flow.youtubesync.engine.YouTubeSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Starts the automatic YouTube Sync of the running profile (ticket 06), from the app's start:
 * - a run when the app comes to the foreground (at most every few minutes);
 * - a run soon after Flow's subscriptions or playlists change, whoever changed them (the viewer, an
 *   import, the Device Sync), when they now differ from the Sync Baseline;
 * - the 6-hour background read, while an account is linked.
 */
object YouTubeSyncStartup {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun runner(): YouTubeSyncRunner

        fun sync(): YouTubeSync

        fun scheduling(): SyncScheduling

        fun accounts(): LinkedAccountStore

        fun subscriptions(): SubscriptionRepository
    }

    private const val SETTLE_MS = 2_000L
    private const val FOREGROUND_EVERY_MS = 10 * 60 * 1000L

    @OptIn(FlowPreview::class)
    suspend fun start(
        context: Context,
        scope: CoroutineScope,
    ) {
        val deps = EntryPointAccessors.fromApplication(context, Dependencies::class.java)
        val linked = suspend { deps.accounts().account.first() != null }
        if (linked() && deps.sync().hasBaseline()) deps.scheduling().ensurePeriodic()

        var lastForegroundRun = 0L
        withContext(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        val now = System.currentTimeMillis()
                        if (now - lastForegroundRun < FOREGROUND_EVERY_MS) return
                        lastForegroundRun = now
                        scope.launch { if (linked()) deps.scheduling().runSoon() }
                    }
                },
            )
        }

        val database = AppDatabase.profile(context)
        merge(
            deps
                .subscriptions()
                .getAllSubscriptions()
                .drop(1)
                .map { },
            database.invalidationTracker.createFlow(PLAYLISTS_TABLE, PLAYLIST_VIDEOS_TABLE, emitInitialState = false).map { },
        ).debounce(SETTLE_MS)
            .collect {
                // Writes of a run itself are not changes to send; whatever is left is seen after it.
                if (deps.runner().running.value) return@collect
                if (linked() && deps.sync().hasLocalChanges()) deps.scheduling().runSoon()
            }
    }

    private const val PLAYLISTS_TABLE = "playlists"
    private const val PLAYLIST_VIDEOS_TABLE = "playlist_video_cross_ref"
}
