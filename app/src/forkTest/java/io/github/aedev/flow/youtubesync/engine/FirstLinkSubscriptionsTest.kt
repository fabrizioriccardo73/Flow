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
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Ticket 03, through the seam: Flow's subscriptions, the Linked Account and the Sync Baseline in,
 * both sides and the new baseline out.
 */
class FirstLinkSubscriptionsTest {
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

    private fun sync(
        flow: FakeLocalSubscriptions,
        youTube: FakeYouTubeAccount,
    ) = YouTubeSync(flow, youTube, baselines, FakeLocalPlaylists())

    @Test
    fun `the first link unites both lists and removes nothing`() =
        runBlocking<Unit> {
            val flow = FakeLocalSubscriptions("A", "B", "C")
            val youTube = FakeYouTubeAccount("B", "C", "D", "E")
            val sync = sync(flow, youTube)

            val plan = sync.plan()
            val result = sync.apply(plan)

            assertThat(plan.isFirstLink).isTrue()
            assertThat(plan.addToYouTube.map { it.channelId }).containsExactly("A")
            assertThat(plan.addToFlow.map { it.channelId }).containsExactly("D", "E")
            assertThat(plan.alreadyOnBoth).containsExactly("B", "C")
            assertThat(flow.channelIds).containsExactly("A", "B", "C", "D", "E")
            assertThat(youTube.channelIds).containsExactly("A", "B", "C", "D", "E")
            assertThat(result).isEqualTo(SyncResult(addedToFlow = 2, addedToYouTube = 1, rejected = emptyList(), stoppedBy = null))
        }

    @Test
    fun `channels arriving from YouTube keep their title and thumbnail`() =
        runBlocking<Unit> {
            val flow = FakeLocalSubscriptions()
            val sync = sync(flow, FakeYouTubeAccount("D"))

            sync.apply(sync.plan())

            val added = flow.subscriptions.getValue("D")
            assertThat(added.title).isEqualTo("Title D")
            assertThat(added.thumbnailUrl).isEqualTo("thumb-D")
        }

    @Test
    fun `planning writes nothing, so a cancelled preview changes nothing`() =
        runBlocking<Unit> {
            val flow = FakeLocalSubscriptions("A")
            val youTube = FakeYouTubeAccount("D")

            sync(flow, youTube).plan()

            assertThat(flow.channelIds).containsExactly("A")
            assertThat(youTube.channelIds).containsExactly("D")
            assertThat(youTube.subscribeCalls).isEqualTo(0)
            assertThat(baselines.read()).isNull()
        }

    @Test
    fun `after applying, the baseline holds every channel now on both sides`() =
        runBlocking<Unit> {
            val sync = sync(FakeLocalSubscriptions("A", "B"), FakeYouTubeAccount("B", "C"))

            sync.apply(sync.plan())

            assertThat(baselines.read()?.subscriptions).containsExactly("A", "B", "C")
        }

    @Test
    fun `a second sync with nothing new plans nothing`() =
        runBlocking<Unit> {
            val flow = FakeLocalSubscriptions("A", "B")
            val youTube = FakeYouTubeAccount("B", "C")
            val sync = sync(flow, youTube)
            sync.apply(sync.plan())

            val again = sync.plan()

            assertThat(again.isFirstLink).isFalse()
            assertThat(again.isEmpty).isTrue()
        }

    @Test
    fun `a channel YouTube refuses is skipped, kept out of the baseline and planned again`() =
        runBlocking<Unit> {
            val flow = FakeLocalSubscriptions("A", "X")
            val youTube = FakeYouTubeAccount()
            youTube.failOn["X"] = YouTubeAccountException.ItemRejected("subscriptionForbidden")
            val sync = sync(flow, youTube)

            val result = sync.apply(sync.plan())

            assertThat(result.rejected).containsExactly("X")
            assertThat(result.stoppedBy).isNull()
            assertThat(youTube.channelIds).containsExactly("A")
            assertThat(baselines.read()?.subscriptions).containsExactly("A")
            assertThat(sync.plan().addToYouTube.map { it.channelId }).containsExactly("X")
        }

    @Test
    fun `running out of quota stops the run and keeps what was done`() =
        runBlocking<Unit> {
            val flow = FakeLocalSubscriptions("A", "B", "C")
            val youTube = FakeYouTubeAccount("D")
            val sync = sync(flow, youTube)
            val plan = sync.plan()
            youTube.failOn[plan.addToYouTube[1].channelId] = YouTubeAccountException.QuotaExceeded()

            val result = sync.apply(plan)

            assertThat(result.stoppedBy).isInstanceOf(YouTubeAccountException.QuotaExceeded::class.java)
            assertThat(result.addedToYouTube).isEqualTo(1)
            assertThat(youTube.subscribeCalls).isEqualTo(2)
            assertThat(flow.channelIds).contains("D")
            assertThat(baselines.read()?.subscriptions).hasSize(2) // D, and the one channel that made it
        }

    @Test
    fun `forgetting the baseline makes the next sync a first link again`() =
        runBlocking<Unit> {
            val sync = sync(FakeLocalSubscriptions("A"), FakeYouTubeAccount("A"))
            sync.apply(sync.plan())

            sync.forgetBaseline()

            assertThat(sync.plan().isFirstLink).isTrue()
        }
}
