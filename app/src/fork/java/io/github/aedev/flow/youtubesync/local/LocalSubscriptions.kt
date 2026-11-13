package io.github.aedev.flow.youtubesync.local

import io.github.aedev.flow.data.local.ChannelSubscription
import io.github.aedev.flow.data.local.SubscriptionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** A channel the profile is subscribed to in Flow. */
data class LocalSubscription(
    val channelId: String,
    val title: String,
    val thumbnailUrl: String,
)

/**
 * Flow's own subscriptions as the YouTube Sync sees them (ADR 0002): reads and writes go through
 * Flow's normal repository, so the Device Sync carries them to the other devices by itself.
 */
interface LocalSubscriptions {
    suspend fun all(): List<LocalSubscription>

    suspend fun add(subscriptions: Collection<LocalSubscription>)

    suspend fun remove(channelIds: Collection<String>)
}

class FlowLocalSubscriptions
    @Inject
    constructor(
        private val repository: SubscriptionRepository,
    ) : LocalSubscriptions {
        override suspend fun all(): List<LocalSubscription> =
            repository.getAllSubscriptions().first().map { LocalSubscription(it.channelId, it.channelName, it.channelThumbnail) }

        override suspend fun add(subscriptions: Collection<LocalSubscription>) {
            repository.subscribeAll(
                subscriptions.map {
                    ChannelSubscription(
                        channelId = it.channelId,
                        channelName = it.title,
                        channelThumbnail = it.thumbnailUrl,
                    )
                },
            )
        }

        override suspend fun remove(channelIds: Collection<String>) {
            channelIds.forEach { repository.unsubscribe(it) }
        }
    }
