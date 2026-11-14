package io.github.aedev.flow.subsfilters.sync

import io.github.aedev.flow.sync.merge.Crdt
import io.github.aedev.flow.sync.protocol.CollectionWire
import io.github.aedev.flow.sync.protocol.SyncSerialization
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One channel's Language of the channel as it travels in the fork's own Device Sync collection
 * (ADR 0004). A removed language is a tombstone, so the removal reaches the other devices.
 */
@Serializable
data class ChannelLanguageRecord(
    val channelId: String,
    val language: String = "",
    val hlc: String = "",
    val deleted: Boolean = false,
)

object ChannelLanguageRecords {
    /** The collection's name on the wire; Flow without the fork never declares or reads it. */
    const val COLLECTION = "fork_channel_languages"

    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    fun encode(records: Collection<ChannelLanguageRecord>): CollectionWire {
        val lines =
            records.sortedBy { it.channelId }.map {
                SyncSerialization.canonicalJson(json.encodeToJsonElement(ChannelLanguageRecord.serializer(), it))
            }
        return CollectionWire(lines, lines.size, SyncSerialization.sha256Hex(lines.joinToString("\n")))
    }

    fun decode(lines: List<String>): List<ChannelLanguageRecord> =
        lines.filter { it.isNotBlank() }.map { json.decodeFromString(ChannelLanguageRecord.serializer(), it) }

    /** Last writer wins per channel; commutative, associative and idempotent like the other mergers. */
    fun merge(
        local: Collection<ChannelLanguageRecord>,
        remote: Collection<ChannelLanguageRecord>,
    ): List<ChannelLanguageRecord> =
        (local + remote)
            .groupBy { it.channelId }
            .map { (_, records) -> records.reduce(::prefer) }
            .sortedBy { it.channelId }

    private fun prefer(
        x: ChannelLanguageRecord,
        y: ChannelLanguageRecord,
    ): ChannelLanguageRecord = Crdt.preferByHlc(x, x.hlc, y, y.hlc) { "${it.deleted}|${it.language}" }
}
