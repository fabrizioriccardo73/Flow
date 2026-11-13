package io.github.aedev.flow.youtubesync.engine

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
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
 * Ticket 04, through the seam: after the first link, a change made on one side since the last
 * YouTube Sync is carried to the other, and both sides converge.
 */
class ThreeWaySubscriptionsTest {
    private val dir: File = Files.createTempDirectory("youtube-sync").toFile()
    private val job = Job()
    private val baselines =
        SyncBaselineStore(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { File(dir, "baseline.preferences_pb") },
        )
    private val flow = FakeLocalSubscriptions("A", "B", "C")
    private val youTube = FakeYouTubeAccount("A", "B", "C")
    private val sync = YouTubeSync(flow, youTube, baselines)

    /** Every test starts after a first link where both sides hold A, B and C. */
    @Before
    fun firstLink() =
        runBlocking<Unit> {
            sync.apply(sync.plan())
        }

    @After
    fun tearDown() {
        runBlocking { job.cancelAndJoin() }
        dir.deleteRecursively()
    }

    private suspend fun syncNow() = sync.apply(sync.plan())

    private fun assertConverged(vararg channels: String) =
        runBlocking<Unit> {
            assertThat(flow.channelIds).containsExactlyElementsIn(channels.toList())
            assertThat(youTube.channelIds).containsExactlyElementsIn(channels.toList())
            assertThat(baselines.read()?.subscriptions).containsExactlyElementsIn(channels.toList())
        }

    @Test
    fun `a subscription added in Flow is created on YouTube`() =
        runBlocking<Unit> {
            flow.subscriptions["D"] = flow.subscriptions.getValue("A").copy(channelId = "D")

            val result = syncNow()

            assertThat(result.addedToYouTube).isEqualTo(1)
            assertConverged("A", "B", "C", "D")
        }

    @Test
    fun `a subscription added on YouTube arrives in Flow`() =
        runBlocking<Unit> {
            youTube.subscribed["D"] = "sub-D"

            val result = syncNow()

            assertThat(result.addedToFlow).isEqualTo(1)
            assertConverged("A", "B", "C", "D")
        }

    @Test
    fun `unsubscribing in Flow unsubscribes on YouTube`() =
        runBlocking<Unit> {
            flow.subscriptions.remove("B")

            val plan = sync.plan()
            val result = sync.apply(plan)

            assertThat(plan.removeFromYouTube.map { it.channelId }).containsExactly("B")
            assertThat(plan.addToFlow).isEmpty()
            assertThat(result.removedFromYouTube).isEqualTo(1)
            assertConverged("A", "C")
        }

    @Test
    fun `unsubscribing on YouTube unsubscribes in Flow`() =
        runBlocking<Unit> {
            youTube.subscribed.remove("C")

            val plan = sync.plan()
            val result = sync.apply(plan)

            assertThat(plan.removeFromFlow.map { it.channelId }).containsExactly("C")
            assertThat(plan.addToYouTube).isEmpty()
            assertThat(result.removedFromFlow).isEqualTo(1)
            assertConverged("A", "B")
        }

    @Test
    fun `a channel added on both sides is kept once`() =
        runBlocking<Unit> {
            flow.subscriptions["D"] = flow.subscriptions.getValue("A").copy(channelId = "D")
            youTube.subscribed["D"] = "sub-D"

            val plan = sync.plan()
            syncNow()

            assertThat(plan.isEmpty).isTrue()
            assertThat(youTube.subscribeCalls).isEqualTo(0)
            assertConverged("A", "B", "C", "D")
        }

    @Test
    fun `a channel removed on both sides is simply forgotten`() =
        runBlocking<Unit> {
            flow.subscriptions.remove("A")
            youTube.subscribed.remove("A")

            assertThat(sync.plan().isEmpty).isTrue()
            syncNow()

            assertConverged("B", "C")
        }

    @Test
    fun `changes on both sides at once all go through`() =
        runBlocking<Unit> {
            flow.subscriptions.remove("A")
            flow.subscriptions["D"] = flow.subscriptions.getValue("B").copy(channelId = "D")
            youTube.subscribed.remove("C")
            youTube.subscribed["E"] = "sub-E"

            syncNow()

            assertConverged("B", "D", "E")
        }

    @Test
    fun `two syncs in a row with no changes do nothing the second time`() =
        runBlocking<Unit> {
            flow.subscriptions.remove("A")
            youTube.subscribed["E"] = "sub-E"
            syncNow()
            val callsAfterFirst = youTube.subscribeCalls

            val again = sync.plan()
            syncNow()

            assertThat(again.isEmpty).isTrue()
            assertThat(youTube.subscribeCalls).isEqualTo(callsAfterFirst)
            assertConverged("B", "C", "E")
        }

    @Test
    fun `a removal YouTube could not apply stays in the baseline and is retried`() =
        runBlocking<Unit> {
            flow.subscriptions.remove("B")
            youTube.failOn["B"] = YouTubeAccountException.Transient()
            val failing =
                object : io.github.aedev.flow.youtubesync.remote.YouTubeAccount by youTube {
                    override suspend fun unsubscribe(subscriptionId: String): Unit = throw YouTubeAccountException.Transient()
                }

            val result = YouTubeSync(flow, failing, baselines).apply(sync.plan())

            assertThat(result.stoppedBy).isInstanceOf(YouTubeAccountException.Transient::class.java)
            assertThat(baselines.read()?.subscriptions).contains("B")
            assertThat(sync.plan().removeFromYouTube.map { it.channelId }).containsExactly("B")
            syncNow()
            assertConverged("A", "C")
        }

    @Test
    fun `an add YouTube could not apply stays out of the baseline and is retried`() =
        runBlocking<Unit> {
            flow.subscriptions["D"] = flow.subscriptions.getValue("A").copy(channelId = "D")
            youTube.failOn["D"] = YouTubeAccountException.QuotaExceeded()

            val result = syncNow()

            assertThat(result.stoppedBy).isInstanceOf(YouTubeAccountException.QuotaExceeded::class.java)
            assertThat(baselines.read()?.subscriptions).doesNotContain("D")
            assertThat(sync.plan().addToYouTube.map { it.channelId }).containsExactly("D")
            assertThat(sync.plan().removeFromFlow).isEmpty()
            youTube.failOn.clear()
            syncNow()
            assertConverged("A", "B", "C", "D")
        }
}
