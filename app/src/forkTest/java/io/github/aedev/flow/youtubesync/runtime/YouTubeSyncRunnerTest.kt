package io.github.aedev.flow.youtubesync.runtime

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.youtubesync.account.LinkedAccount
import io.github.aedev.flow.youtubesync.account.LinkedAccountStore
import io.github.aedev.flow.youtubesync.account.SecretSealer
import io.github.aedev.flow.youtubesync.engine.SyncFixture
import io.github.aedev.flow.youtubesync.remote.YouTubeAccountException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Tickets 06 and 07, through the seam: automatic runs, the pending local changes, and what each kind
 * of failure leads to (status, retries, notifications).
 */
class YouTubeSyncRunnerTest : SyncFixture() {
    private val dir: File = Files.createTempDirectory("youtube-sync-runner").toFile()
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val statusStore = SyncStatusStore(PreferenceDataStoreFactory.create(scope = scope) { File(dir, "status.preferences_pb") })
    private val accounts =
        LinkedAccountStore(
            PreferenceDataStoreFactory.create(scope = scope) { File(dir, "account.preferences_pb") },
            object : SecretSealer {
                override fun seal(plain: String) = plain

                override fun open(sealed: String?) = sealed.orEmpty()
            },
        )

    private val scheduled = mutableListOf<String>()
    private val notified = mutableListOf<String>()
    private var now = 1_700_000_000_000L

    private val scheduling =
        object : SyncScheduling {
            override fun ensurePeriodic() {
                scheduled += "periodic"
            }

            override fun runSoon() {
                scheduled += "soon"
            }

            override fun runAt(timeMs: Long) {
                scheduled += "at:$timeMs"
            }

            override fun cancelAll() {
                scheduled += "cancel"
            }
        }

    private val notifications =
        object : SyncNotifications {
            override fun needsRelink() {
                notified += "relink"
            }

            override fun needsConfirmation(removals: Int) {
                notified += "confirm:$removals"
            }

            override fun clear() {
                notified += "clear"
            }
        }

    private val runner = YouTubeSyncRunner(sync, statusStore, accounts, scheduling, notifications) { now }
    private val me = LinkedAccount("UCme", "Me")

    @Before
    fun linkAndFirstSync() =
        runBlocking<Unit> {
            accounts.save(me, "{}")
            youTube.subscribed["A"] = "sub-A"
            runner.apply(runner.plan(), massRemovalConfirmed = false)
            scheduled.clear()
            notified.clear()
        }

    @After
    fun tearDown() {
        runBlocking { job.cancelAndJoin() }
        dir.deleteRecursively()
        close()
    }

    private fun subscribeInFlow(channel: String) {
        subscriptions.subscriptions[channel] = subscriptions.subscriptions.getValue("A").copy(channelId = channel)
    }

    @Test
    fun `a change in Flow is pending until a run sends it, and then the status shows the time`() =
        runBlocking<Unit> {
            subscribeInFlow("B")
            assertThat(sync.hasLocalChanges()).isTrue()

            val outcome = runner.runAutomatically()

            assertThat(outcome).isInstanceOf(AutoOutcome.Done::class.java)
            assertThat(youTube.channelIds).contains("B")
            assertThat(sync.hasLocalChanges()).isFalse()
            assertThat(statusStore.read()).isEqualTo(SyncStatus(lastSuccessAt = now))
        }

    @Test
    fun `a subscribe followed by an unsubscribe leaves nothing to send`() =
        runBlocking<Unit> {
            subscribeInFlow("B")
            subscriptions.subscriptions.remove("B")

            assertThat(sync.hasLocalChanges()).isFalse()
        }

    @Test
    fun `before the first link preview is confirmed nothing runs automatically`() =
        runBlocking<Unit> {
            sync.forgetBaseline()
            assertThat(runner.runAutomatically()).isEqualTo(AutoOutcome.Skipped)
            assertThat(youTube.writes).isEmpty()
        }

    @Test
    fun `without a Linked Account nothing runs`() =
        runBlocking<Unit> {
            accounts.clear()
            assertThat(runner.runAutomatically()).isEqualTo(AutoOutcome.Skipped)
        }

    @Test
    fun `quota exhausted - changes stay pending, a run is planned after the reset, no notification`() =
        runBlocking<Unit> {
            subscribeInFlow("B")
            youTube.failOn["B"] = YouTubeAccountException.QuotaExceeded()

            val outcome = runner.runAutomatically() as AutoOutcome.WaitForQuota

            assertThat(outcome.untilMs).isEqualTo(nextQuotaReset(now))
            assertThat(scheduled).containsExactly("at:${outcome.untilMs}")
            assertThat(notified).isEmpty()
            assertThat(statusStore.read().problem).isEqualTo(SyncProblem.QUOTA)
            assertThat(sync.hasLocalChanges()).isTrue()

            // Before the reset nothing is tried; after it, the change goes through.
            youTube.failOn.clear()
            assertThat(runner.runAutomatically()).isInstanceOf(AutoOutcome.WaitForQuota::class.java)
            now = outcome.untilMs
            assertThat(runner.runAutomatically()).isInstanceOf(AutoOutcome.Done::class.java)
            assertThat(youTube.channelIds).contains("B")
        }

    @Test
    fun `authorization revoked - the sync stops, notifies, and the same account picks up where it stopped`() =
        runBlocking<Unit> {
            subscribeInFlow("B")
            youTube.failOn["B"] = YouTubeAccountException.AuthorizationInvalid()

            assertThat(runner.runAutomatically()).isEqualTo(AutoOutcome.NeedsRelink)
            assertThat(notified).containsExactly("relink")
            assertThat(statusStore.read().problem).isEqualTo(SyncProblem.NEEDS_RELINK)
            assertThat(runner.runAutomatically()).isEqualTo(AutoOutcome.NeedsRelink)

            youTube.failOn.clear()
            runner.onLinked(previous = me, now = me)
            assertThat(statusStore.read().problem).isNull()
            assertThat(scheduled).contains("soon")
            assertThat(runner.runAutomatically()).isInstanceOf(AutoOutcome.Done::class.java)
            assertThat(youTube.channelIds).contains("B")
        }

    @Test
    fun `linking another account starts over as a first link`() =
        runBlocking<Unit> {
            runner.onLinked(previous = me, now = LinkedAccount("UCother", "Other"))
            assertThat(sync.hasBaseline()).isFalse()
        }

    @Test
    fun `a transient error asks for a retry`() =
        runBlocking<Unit> {
            subscribeInFlow("B")
            youTube.failOn["B"] = YouTubeAccountException.Transient()

            assertThat(runner.runAutomatically()).isEqualTo(AutoOutcome.RetryLater)
            assertThat(statusStore.read().problem).isEqualTo(SyncProblem.OFFLINE)
            assertThat(sync.hasLocalChanges()).isTrue()
        }

    @Test
    fun `one rejected item does not stop the others`() =
        runBlocking<Unit> {
            subscribeInFlow("B")
            subscribeInFlow("C")
            youTube.failOn["B"] = YouTubeAccountException.ItemRejected("subscriptionForbidden")

            val outcome = runner.runAutomatically() as AutoOutcome.Done

            assertThat(outcome.result.rejected).containsExactly("B")
            assertThat(youTube.channelIds).contains("C")
        }

    @Test
    fun `a background run with mass removals waits and notifies instead of applying`() =
        runBlocking<Unit> {
            (1..30).forEach { youTube.subscribed["X$it"] = "sub-X$it" }
            runner.apply(runner.plan(), massRemovalConfirmed = false)
            notified.clear()
            subscriptions.subscriptions.clear()

            assertThat(runner.runAutomatically()).isEqualTo(AutoOutcome.NeedsConfirmation)

            assertThat(notified).containsExactly("confirm:31")
            assertThat(statusStore.read().problem).isEqualTo(SyncProblem.NEEDS_CONFIRMATION)
            assertThat(youTube.channelIds).hasSize(31)
        }

    @Test
    fun `unlinking releases the playlists and stops the background without writing to YouTube`() =
        runBlocking<Unit> {
            youTube.playlist("PLowned0000001", "Mine", "a")
            runner.runAutomatically()
            youTube.writes.clear()

            runner.onUnlinked()

            assertThat(scheduled).containsExactly("cancel")
            assertThat(youTube.writes).isEmpty()
            assertThat(playlists.byYouTubeId("PLowned0000001")).isNull()
            assertThat(sync.hasBaseline()).isFalse()
            assertThat(statusStore.read()).isEqualTo(SyncStatus())
        }

    @Test
    fun `the quota resets after midnight Pacific time`() {
        // 2026-11-13 10:00 UTC is 02:00 in Los Angeles: the next reset is 2026-11-14 08:00 UTC (+10 minutes).
        val tenUtc =
            java.time.Instant
                .parse("2026-11-13T10:00:00Z")
                .toEpochMilli()
        assertThat(nextQuotaReset(tenUtc)).isEqualTo(
            java.time.Instant
                .parse("2026-11-14T08:10:00Z")
                .toEpochMilli(),
        )
    }
}
