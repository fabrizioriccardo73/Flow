package io.github.aedev.flow.youtubesync.engine

import io.github.aedev.flow.youtubesync.local.LocalSubscription
import io.github.aedev.flow.youtubesync.local.LocalSubscriptions
import io.github.aedev.flow.youtubesync.remote.RemoteSubscription
import io.github.aedev.flow.youtubesync.remote.YouTubeAccount
import io.github.aedev.flow.youtubesync.remote.YouTubeAccountException

/** An in-memory Linked Account. [failOn] makes subscribing to a channel throw. */
class FakeYouTubeAccount(
    vararg channels: String,
) : YouTubeAccount {
    val subscribed = channels.associateWith { "sub-$it" }.toMutableMap()
    val failOn = mutableMapOf<String, YouTubeAccountException>()
    var subscribeCalls = 0

    val channelIds: Set<String> get() = subscribed.keys

    override suspend fun subscriptions() =
        subscribed.map { (channel, id) -> RemoteSubscription(id, channel, "Title $channel", "thumb-$channel") }

    override suspend fun subscribe(channelId: String) {
        subscribeCalls++
        failOn[channelId]?.let { throw it }
        subscribed.getOrPut(channelId) { "sub-$channelId" }
    }

    override suspend fun unsubscribe(subscriptionId: String) {
        subscribed.entries.removeAll { it.value == subscriptionId }
    }
}

/** Flow's subscriptions, in memory. */
class FakeLocalSubscriptions(
    vararg channels: String,
) : LocalSubscriptions {
    val subscriptions = channels.associateWith { LocalSubscription(it, "Local $it", "local-thumb-$it") }.toMutableMap()

    val channelIds: Set<String> get() = subscriptions.keys

    override suspend fun all() = subscriptions.values.toList()

    override suspend fun add(subscriptions: Collection<LocalSubscription>) {
        subscriptions.forEach { this.subscriptions[it.channelId] = it }
    }

    override suspend fun remove(channelIds: Collection<String>) {
        channelIds.forEach { subscriptions.remove(it) }
    }
}
