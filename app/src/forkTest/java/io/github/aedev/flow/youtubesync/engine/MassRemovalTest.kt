package io.github.aedev.flow.youtubesync.engine

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Ticket 05: a YouTube Sync that would remove too much is never applied without confirmation. */
class MassRemovalTest {
    private val dir: File = Files.createTempDirectory("youtube-sync").toFile()
    private val job = Job()
    private val baselines =
        SyncBaselineStore(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { File(dir, "baseline.preferences_pb") },
        )

    @After
    fun tearDown() {
        runBlocking { job.cancelAndJoin() }
        dir.deleteRecursively()
    }

    private fun channels(count: Int) = (1..count).map { "C$it" }.toTypedArray()

    /** Both sides hold [channels] and agree on them. */
    private fun linked(vararg channels: String): Triple<FakeLocalSubscriptions, FakeYouTubeAccount, YouTubeSync> =
        runBlocking {
            val flow = FakeLocalSubscriptions(*channels)
            val youTube = FakeYouTubeAccount(*channels)
            val sync = YouTubeSync(flow, youTube, baselines)
            sync.apply(sync.plan())
            Triple(flow, youTube, sync)
        }

    @Test
    fun `Flow losing its data does not unsubscribe everything on YouTube`() =
        runBlocking<Unit> {
            val (flow, youTube, sync) = linked(*channels(50))
            val baselineBefore = baselines.read()
            flow.subscriptions.clear()

            val plan = sync.plan()

            assertThat(plan.massRemovals).hasSize(1)
            assertThat(plan.massRemovals.single().side).isEqualTo(SyncSide.YOUTUBE)
            assertThat(plan.massRemovals.single().count).isEqualTo(50)
            assertThat(plan.massRemovals.single().total).isEqualTo(50)
            assertThrows(MassRemovalNotConfirmedException::class.java) { runBlocking { sync.apply(plan) } }
            assertThat(youTube.channelIds).hasSize(50)
            assertThat(baselines.read()).isEqualTo(baselineBefore)
        }

    @Test
    fun `YouTube losing its data does not empty Flow`() =
        runBlocking<Unit> {
            val (flow, youTube, sync) = linked(*channels(30))
            youTube.subscribed.clear()

            val plan = sync.plan()

            assertThat(plan.massRemovals.single().side).isEqualTo(SyncSide.FLOW)
            assertThrows(MassRemovalNotConfirmedException::class.java) { runBlocking { sync.apply(plan) } }
            assertThat(flow.channelIds).hasSize(30)
        }

    @Test
    fun `a confirmed mass removal is applied`() =
        runBlocking<Unit> {
            val (flow, youTube, sync) = linked(*channels(30))
            flow.subscriptions.clear()

            val result = sync.apply(sync.plan(), massRemovalConfirmed = true)

            assertThat(result.removedFromYouTube).isEqualTo(30)
            assertThat(youTube.channelIds).isEmpty()
            assertThat(baselines.read()?.subscriptions).isEmpty()
        }

    @Test
    fun `an ordinary unsubscribe needs no confirmation`() =
        runBlocking<Unit> {
            val (flow, youTube, sync) = linked(*channels(5))
            flow.subscriptions.remove("C1")

            val plan = sync.plan()
            sync.apply(plan)

            assertThat(plan.massRemovals).isEmpty()
            assertThat(youTube.channelIds).doesNotContain("C1")
        }

    @Test
    fun `the warning names the channels that would go`() =
        runBlocking<Unit> {
            val (flow, _, sync) = linked(*channels(10))
            listOf("C1", "C2", "C3").forEach { flow.subscriptions.remove(it) }

            val removal = sync.plan().massRemovals.single()

            assertThat(removal.items).containsExactly("Title C1", "Title C2", "Title C3")
        }

    @Test
    fun `the thresholds`() {
        fun alarm(
            removing: Int,
            total: Int,
        ) = RemovalGuard.check(SyncCollection.SUBSCRIPTIONS, SyncSide.YOUTUBE, List(removing) { "x$it" }, total) != null

        // More than 20 always asks, whatever the share.
        assertThat(alarm(21, 10_000)).isTrue()
        assertThat(alarm(20, 10_000)).isFalse()
        // More than 10% asks once at least 3 items go.
        assertThat(alarm(3, 20)).isTrue()
        assertThat(alarm(2, 20)).isFalse()
        assertThat(alarm(2, 5)).isFalse()
        assertThat(alarm(10, 100)).isFalse()
        assertThat(alarm(11, 100)).isTrue()
        assertThat(alarm(0, 0)).isFalse()
    }
}
