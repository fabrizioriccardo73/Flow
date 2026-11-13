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
 *
 * On the first link (no Sync Baseline) it is a union and removes nothing. Afterwards it is a three-way
 * comparison of Flow, YouTube and the Sync Baseline: a change made on one side only is carried to the
 * other. For subscriptions there are no real conflicts: a channel added on both sides is one channel,
 * and a channel removed on one side and left alone on the other is removed.
 */
data class SyncPlan(
    val isFirstLink: Boolean,
    /** Subscriptions only on YouTube, to add in Flow. */
    val addToFlow: List<RemoteSubscription>,
    /** Subscriptions only in Flow, to add on YouTube. */
    val addToYouTube: List<LocalSubscription>,
    /** Channels already on both sides. */
    val alreadyOnBoth: Set<String>,
    /** Channels unsubscribed on YouTube since the last YouTube Sync, to remove from Flow. */
    val removeFromFlow: List<LocalSubscription> = emptyList(),
    /** Channels unsubscribed in Flow since the last YouTube Sync, to remove from YouTube. */
    val removeFromYouTube: List<RemoteSubscription> = emptyList(),
    /** Removals too large to apply without confirmation (see [RemovalGuard]); empty when none. */
    val massRemovals: List<MassRemoval> = emptyList(),
) {
    val isEmpty: Boolean
        get() = addToFlow.isEmpty() && addToYouTube.isEmpty() && removeFromFlow.isEmpty() && removeFromYouTube.isEmpty()
}

/** How applying a [SyncPlan] went. */
data class SyncResult(
    val addedToFlow: Int,
    val addedToYouTube: Int,
    /** Channels YouTube refused (e.g. closed channels); retried on the next YouTube Sync. */
    val rejected: List<String>,
    /** Why the run stopped early, if it did (quota, authorization, network); null when it completed. */
    val stoppedBy: YouTubeAccountException?,
    val removedFromFlow: Int = 0,
    val removedFromYouTube: Int = 0,
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
            val baseline = baselines.read()?.subscriptions
            val flow = local.all().associateBy { it.channelId }
            val youTube = remote.subscriptions().associateBy { it.channelId }
            val onlyInFlow = flow.values.filter { it.channelId !in youTube }
            val onlyOnYouTube = youTube.values.filter { it.channelId !in flow }
            if (baseline == null) {
                return SyncPlan(
                    isFirstLink = true,
                    addToFlow = onlyOnYouTube,
                    addToYouTube = onlyInFlow,
                    alreadyOnBoth = flow.keys intersect youTube.keys,
                )
            }
            // A channel on one side only was either added there (not in the baseline) or removed from
            // the other side (in the baseline): carry that change across.
            val removeFromFlow = onlyInFlow.filter { it.channelId in baseline }
            val removeFromYouTube = onlyOnYouTube.filter { it.channelId in baseline }
            return SyncPlan(
                isFirstLink = false,
                addToFlow = onlyOnYouTube.filter { it.channelId !in baseline },
                addToYouTube = onlyInFlow.filter { it.channelId !in baseline },
                alreadyOnBoth = flow.keys intersect youTube.keys,
                removeFromFlow = removeFromFlow,
                removeFromYouTube = removeFromYouTube,
                massRemovals =
                    listOfNotNull(
                        RemovalGuard.check(SyncCollection.SUBSCRIPTIONS, SyncSide.FLOW, removeFromFlow.map { it.title }, flow.size),
                        RemovalGuard.check(
                            SyncCollection.SUBSCRIPTIONS,
                            SyncSide.YOUTUBE,
                            removeFromYouTube.map { it.title },
                            youTube.size,
                        ),
                    ),
            )
        }

        /**
         * Applies [plan]: Flow first (local, cannot run out of quota), then YouTube one channel at a time.
         * The new baseline is what both sides now agree on, so anything that failed is planned again
         * next time: a failed add stays out of it, a failed removal stays in it. Quota, authorization
         * and network errors stop the run; a channel YouTube refuses is skipped.
         *
         * A plan with [SyncPlan.massRemovals] is refused with [MassRemovalNotConfirmedException], writing
         * nothing, unless [massRemovalConfirmed]: no caller, background ones included, can skip the guard.
         */
        suspend fun apply(
            plan: SyncPlan,
            massRemovalConfirmed: Boolean = false,
        ): SyncResult {
            if (plan.massRemovals.isNotEmpty() && !massRemovalConfirmed) throw MassRemovalNotConfirmedException(plan.massRemovals)
            val agreed = plan.alreadyOnBoth.toMutableSet()

            if (plan.addToFlow.isNotEmpty()) {
                local.add(plan.addToFlow.map { LocalSubscription(it.channelId, it.title, it.thumbnailUrl) })
                plan.addToFlow.mapTo(agreed) { it.channelId }
            }
            if (plan.removeFromFlow.isNotEmpty()) {
                local.remove(plan.removeFromFlow.map { it.channelId })
            }

            var added = 0
            var removed = 0
            val rejected = mutableListOf<String>()
            var stoppedBy: YouTubeAccountException? = null
            // Removals not (yet) done on YouTube: still in the baseline, so they are retried.
            val pendingRemovals = plan.removeFromYouTube.mapTo(mutableSetOf()) { it.channelId }

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
            if (stoppedBy == null) {
                for (subscription in plan.removeFromYouTube) {
                    try {
                        remote.unsubscribe(subscription.subscriptionId)
                        pendingRemovals -= subscription.channelId
                        removed++
                    } catch (e: YouTubeAccountException.ItemRejected) {
                        rejected += subscription.channelId
                    } catch (e: YouTubeAccountException) {
                        stoppedBy = e
                        break
                    }
                }
            }

            baselines.write(SyncBaseline(subscriptions = agreed + pendingRemovals))
            return SyncResult(
                addedToFlow = plan.addToFlow.size,
                addedToYouTube = added,
                rejected = rejected,
                stoppedBy = stoppedBy,
                removedFromFlow = plan.removeFromFlow.size,
                removedFromYouTube = removed,
            )
        }
    }
