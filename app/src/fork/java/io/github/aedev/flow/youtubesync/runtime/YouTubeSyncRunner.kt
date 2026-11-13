package io.github.aedev.flow.youtubesync.runtime

import io.github.aedev.flow.youtubesync.account.LinkedAccount
import io.github.aedev.flow.youtubesync.account.LinkedAccountStore
import io.github.aedev.flow.youtubesync.engine.MassRemovalNotConfirmedException
import io.github.aedev.flow.youtubesync.engine.SyncPlan
import io.github.aedev.flow.youtubesync.engine.SyncResult
import io.github.aedev.flow.youtubesync.engine.YouTubeSync
import io.github.aedev.flow.youtubesync.remote.YouTubeAccountException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** The wall clock, a seam for tests. */
fun interface Clock {
    fun now(): Long
}

/** Background work of the YouTube Sync (WorkManager in the app, a recorder in tests). */
interface SyncScheduling {
    /** The read from YouTube every 6 hours, with network. */
    fun ensurePeriodic()

    /** A run as soon as there is network (pending local changes, app opened, account linked again). */
    fun runSoon()

    /** A run at [timeMs] (after the quota reset). */
    fun runAt(timeMs: Long)

    fun cancelAll()
}

/** The notifications of the YouTube Sync. */
interface SyncNotifications {
    /** "Ricollega l'account". */
    fun needsRelink()

    /** A background run is waiting for the viewer to confirm many removals. */
    fun needsConfirmation(removals: Int)

    fun clear()
}

/** How an automatic run went, so the worker knows whether to retry. */
sealed interface AutoOutcome {
    data class Done(
        val result: SyncResult,
    ) : AutoOutcome

    /** Nothing to do here: no account, or the first link still waits for its preview. */
    data object Skipped : AutoOutcome

    /** Network or server trouble: retry with a growing wait. */
    data object RetryLater : AutoOutcome

    data class WaitForQuota(
        val untilMs: Long,
    ) : AutoOutcome

    data object NeedsRelink : AutoOutcome

    data object NeedsConfirmation : AutoOutcome
}

/**
 * Runs the YouTube Sync one at a time and keeps its status, for the viewer ("Sincronizza ora", the
 * preview) and for the background (spec: "Tempistiche", ticket 06; "Gestione degli errori", ticket 07).
 */
@Singleton
class YouTubeSyncRunner
    @Inject
    constructor(
        private val sync: YouTubeSync,
        private val status: SyncStatusStore,
        private val accounts: LinkedAccountStore,
        private val scheduling: SyncScheduling,
        private val notifications: SyncNotifications,
        private val clock: Clock,
    ) {
        private val mutex = Mutex()
        private val _running = MutableStateFlow(false)

        /** True while a run reads or writes: the local change watcher ignores what it sees meanwhile. */
        val running: StateFlow<Boolean> = _running.asStateFlow()

        val state = status.status

        /** Reads both sides for the preview. Failures are recorded and rethrown. */
        suspend fun plan(): SyncPlan = exclusive { recordingFailures { sync.plan() } }

        /** Applies a plan the viewer confirmed in the preview. */
        suspend fun apply(
            plan: SyncPlan,
            massRemovalConfirmed: Boolean,
        ): SyncResult =
            exclusive {
                recordingFailures {
                    val result = sync.apply(plan, massRemovalConfirmed)
                    record(result)
                    // From the first link on, the background keeps both sides in step.
                    scheduling.ensurePeriodic()
                    result
                }
            }

        /** Links a Flow-only playlist to YouTube (ticket 10), then sends its videos right away. */
        suspend fun linkPlaylist(localId: String): String =
            exclusive { recordingFailures { sync.linkPlaylist(localId) } }.also { scheduling.runSoon() }

        /** A run without anyone watching: from the worker, after a local change, on app open, every 6 hours. */
        suspend fun runAutomatically(): AutoOutcome {
            if (accounts.account.first() == null) return AutoOutcome.Skipped
            val current = status.read()
            if (current.problem == SyncProblem.NEEDS_RELINK) return AutoOutcome.NeedsRelink
            val retryAt = current.retryAt
            if (current.problem == SyncProblem.QUOTA && retryAt != null && clock.now() < retryAt) {
                return AutoOutcome.WaitForQuota(retryAt)
            }
            if (!sync.hasBaseline()) return AutoOutcome.Skipped
            return exclusive {
                try {
                    val plan = sync.plan()
                    if (plan.massRemovals.isNotEmpty()) throw MassRemovalNotConfirmedException(plan.massRemovals)
                    val result = sync.apply(plan)
                    result.stoppedBy?.let { return@exclusive failure(it) }
                    record(result)
                    AutoOutcome.Done(result)
                } catch (e: MassRemovalNotConfirmedException) {
                    status.update { it.copy(problem = SyncProblem.NEEDS_CONFIRMATION, retryAt = null) }
                    notifications.needsConfirmation(e.massRemovals.sumOf { it.count })
                    AutoOutcome.NeedsConfirmation
                } catch (e: YouTubeAccountException) {
                    failure(e)
                }
            }
        }

        /**
         * After a link. The same account again (after "Ricollega l'account") picks up where it stopped:
         * the Sync Baseline and pending changes stay. Another account starts over as a first link.
         */
        suspend fun onLinked(
            previous: LinkedAccount?,
            now: LinkedAccount,
        ) {
            notifications.clear()
            if (previous?.channelId == now.channelId && sync.hasBaseline()) {
                status.update { it.copy(problem = null, retryAt = null) }
                scheduling.ensurePeriodic()
                scheduling.runSoon()
            } else {
                sync.forgetBaseline()
                status.clear()
            }
        }

        /**
         * After an unlink (ticket 11): Owned and Saved Playlists become Flow-only, the Sync Baseline and the
         * pending changes are dropped, background work stops. Nothing is sent to YouTube.
         */
        suspend fun onUnlinked() {
            scheduling.cancelAll()
            exclusive { sync.releaseAll() }
            status.clear()
            notifications.clear()
        }

        private suspend fun record(result: SyncResult) {
            val stoppedBy = result.stoppedBy
            if (stoppedBy != null) {
                failure(stoppedBy)
            } else {
                status.update { SyncStatus(lastSuccessAt = clock.now()) }
                notifications.clear()
            }
        }

        /** Records why a run failed and plans what comes next. */
        private suspend fun failure(e: YouTubeAccountException): AutoOutcome =
            when (e) {
                is YouTubeAccountException.QuotaExceeded -> {
                    val until = nextQuotaReset(clock.now())
                    status.update { it.copy(problem = SyncProblem.QUOTA, retryAt = until) }
                    scheduling.runAt(until)
                    AutoOutcome.WaitForQuota(until)
                }

                is YouTubeAccountException.AuthorizationInvalid -> {
                    status.update { it.copy(problem = SyncProblem.NEEDS_RELINK, retryAt = null) }
                    notifications.needsRelink()
                    AutoOutcome.NeedsRelink
                }

                is YouTubeAccountException.NotLinked -> {
                    AutoOutcome.Skipped
                }

                is YouTubeAccountException.Transient, is YouTubeAccountException.ItemRejected -> {
                    status.update { it.copy(problem = SyncProblem.OFFLINE, retryAt = null) }
                    AutoOutcome.RetryLater
                }
            }

        private suspend fun <T> recordingFailures(block: suspend () -> T): T =
            try {
                block()
            } catch (e: YouTubeAccountException) {
                failure(e)
                throw e
            }

        private suspend fun <T> exclusive(block: suspend () -> T): T =
            mutex.withLock {
                _running.value = true
                try {
                    block()
                } finally {
                    _running.value = false
                }
            }
    }
