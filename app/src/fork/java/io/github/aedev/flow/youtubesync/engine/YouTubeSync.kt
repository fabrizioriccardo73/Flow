package io.github.aedev.flow.youtubesync.engine

import io.github.aedev.flow.youtubesync.local.LocalSubscription
import io.github.aedev.flow.youtubesync.local.LocalSubscriptions
import io.github.aedev.flow.youtubesync.remote.RemoteSubscription
import io.github.aedev.flow.youtubesync.remote.YouTubeAccount
import io.github.aedev.flow.youtubesync.remote.YouTubeAccountException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What a YouTube Sync is about to do, computed before any write so it can be shown and confirmed.
 * Ticket 03 (first link): a union, so nothing is ever removed on either side.
 */
data class SyncPlan(
    val isFirstLink: Boolean,
    /** Subscriptions only on YouTube, to add in Flow. */
    val addToFlow: List<RemoteSubscription>,
    /** Subscriptions only in Flow, to add on YouTube. */
    val addToYouTube: List<LocalSubscription>,
    /** Channels already on both sides. */
    val alreadyOnBoth: Set<String>,
) {
    val isEmpty: Boolean get() = addToFlow.isEmpty() && addToYouTube.isEmpty()
}

/** How applying a [SyncPlan] went. */
data class SyncResult(
    val addedToFlow: Int,
    val addedToYouTube: Int,
    /** Channels YouTube refused (e.g. closed channels); retried on the next YouTube Sync. */
    val rejected: List<String>,
    /** Why the run stopped early, if it did (quota, authorization, network); null when it completed. */
    val stoppedBy: YouTubeAccountException?,
)

/**
 * The YouTube Sync of subscriptions (spec: "Motore di confronto a tre vie"). This is the seam the
 * tests drive: Flow's subscriptions, the Linked Account and the Sync Baseline in, both sides and the
 * new baseline out.
 */
@Singleton
class YouTubeSync
    @Inject
    constructor(
        private val local: LocalSubscriptions,
        private val remote: YouTubeAccount,
        private val baselines: SyncBaselineStore,
    ) {
        /**
         * Forgets the Sync Baseline, so the next run is a first link again (union, nothing removed).
         * Called on every new link and on unlink: a baseline must never outlive the account it was made with.
         */
        suspend fun forgetBaseline() = baselines.clear()

        /** Reads both sides and plans the run. Writes nothing. */
        suspend fun plan(): SyncPlan {
            val baseline = baselines.read()
            val flow = local.all().associateBy { it.channelId }
            val youTube = remote.subscriptions().associateBy { it.channelId }
            return SyncPlan(
                isFirstLink = baseline == null,
                addToFlow = youTube.values.filter { it.channelId !in flow },
                addToYouTube = flow.values.filter { it.channelId !in youTube },
                alreadyOnBoth = flow.keys intersect youTube.keys,
            )
        }

        /**
         * Applies [plan]: Flow first (local, cannot fail on quota), then YouTube one channel at a time.
         * The baseline records only channels that ended up on both sides, so anything that failed is
         * planned again next time. Quota, authorization and network errors stop the run; a channel
         * YouTube refuses is skipped.
         */
        suspend fun apply(plan: SyncPlan): SyncResult {
            val agreed = plan.alreadyOnBoth.toMutableSet()

            if (plan.addToFlow.isNotEmpty()) {
                local.add(plan.addToFlow.map { LocalSubscription(it.channelId, it.title, it.thumbnailUrl) })
                plan.addToFlow.mapTo(agreed) { it.channelId }
            }

            var added = 0
            val rejected = mutableListOf<String>()
            var stoppedBy: YouTubeAccountException? = null
            for (subscription in plan.addToYouTube) {
                try {
                    remote.subscribe(subscription.channelId)
                    agreed += subscription.channelId
                    added++
                } catch (e: YouTubeAccountException.ItemRejected) {
                    rejected += subscription.channelId
                } catch (e: YouTubeAccountException) {
                    stoppedBy = e
                    break
                }
            }

            val previous = baselines.read() ?: SyncBaseline()
            baselines.write(previous.copy(subscriptions = previous.subscriptions + agreed))
            return SyncResult(plan.addToFlow.size, added, rejected, stoppedBy)
        }
    }
