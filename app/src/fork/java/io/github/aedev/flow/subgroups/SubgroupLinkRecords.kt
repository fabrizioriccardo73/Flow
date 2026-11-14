package io.github.aedev.flow.subgroups

import io.github.aedev.flow.sync.merge.Crdt
import io.github.aedev.flow.sync.protocol.CollectionWire
import io.github.aedev.flow.sync.protocol.SyncSerialization
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * One Subgroup's link to its parent, kept apart from the Groups themselves so a device with the
 * original Flow cannot drop it (ADR 0004). Removing the link leaves a tombstone, so the removal
 * can reach the other devices.
 */
@Serializable
data class SubgroupLinkRecord(
    val group: String,
    val parent: String = "",
    val hlc: String = "",
    val deleted: Boolean = false,
)

object SubgroupLinkRecords {
    private val serializer = ListSerializer(SubgroupLinkRecord.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    /** Unreadable or missing storage reads as no links. */
    fun decode(value: String?): List<SubgroupLinkRecord> = value?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }.orEmpty()

    fun encode(records: List<SubgroupLinkRecord>): String = json.encodeToString(serializer, records.sortedBy { it.group })

    /** Subgroup name to parent name, without tombstones. */
    fun live(records: List<SubgroupLinkRecord>): Map<String, String> =
        records.filter { !it.deleted && it.parent.isNotBlank() }.associate { it.group to it.parent }

    /** [records] with [group]'s record replaced by [record]. */
    fun replace(
        records: List<SubgroupLinkRecord>,
        group: String,
        record: SubgroupLinkRecord,
    ): List<SubgroupLinkRecord> = records.filter { it.group != group } + record

    /**
     * [records] after the Group [oldName] (null for a new Group) was saved as [newName] inside
     * [parent] (null for none). A rename moves the link to the new name and keeps the Group's
     * Subgroups pointing at it; only what changed gets a new [stamp].
     */
    fun afterSave(
        records: List<SubgroupLinkRecord>,
        oldName: String?,
        newName: String,
        parent: String?,
        stamp: String,
    ): List<SubgroupLinkRecord> {
        var out = records
        if (oldName != null && oldName != newName) {
            if (out.any { it.group == oldName }) out = replace(out, oldName, tombstone(oldName, stamp))
            live(out).filterValues { it == oldName }.keys.forEach { child ->
                out = replace(out, child, SubgroupLinkRecord(child, newName, stamp))
            }
        }
        if (parent != live(out)[newName]) {
            out = replace(out, newName, if (parent == null) tombstone(newName, stamp) else SubgroupLinkRecord(newName, parent, stamp))
        }
        return out
    }

    /** [records] after the Group [name] was deleted: its own link and its Subgroups' links end, the Subgroups stay. */
    fun afterDelete(
        records: List<SubgroupLinkRecord>,
        name: String,
        stamp: String,
    ): List<SubgroupLinkRecord> {
        var out = records
        val ended = live(out).filterValues { it == name }.keys + listOfNotNull(name.takeIf { live(out).containsKey(it) })
        ended.forEach { group -> out = replace(out, group, tombstone(group, stamp)) }
        return out
    }

    private fun tombstone(
        group: String,
        stamp: String,
    ) = SubgroupLinkRecord(group = group, hlc = stamp, deleted = true)
}

/**
 * The links as they travel in the fork's own Device Sync collection (ADR 0004). Flow without the
 * fork never declares or reads it, so there the Subgroups simply show as plain Groups.
 */
object SubgroupLinkWire {
    const val COLLECTION = "fork_group_parents"

    /** The field that keys a record, one per Subgroup. */
    const val KEY_FIELD = "group"

    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    fun encode(records: Collection<SubgroupLinkRecord>): CollectionWire {
        val lines =
            records.sortedBy { it.group }.map {
                SyncSerialization.canonicalJson(json.encodeToJsonElement(SubgroupLinkRecord.serializer(), it))
            }
        return CollectionWire(lines, lines.size, SyncSerialization.sha256Hex(lines.joinToString("\n")))
    }

    fun decode(lines: List<String>): List<SubgroupLinkRecord> =
        lines.filter { it.isNotBlank() }.map { json.decodeFromString(SubgroupLinkRecord.serializer(), it) }

    /** Last writer wins per Subgroup, tombstones included; commutative, associative and idempotent. */
    fun merge(
        local: Collection<SubgroupLinkRecord>,
        remote: Collection<SubgroupLinkRecord>,
    ): List<SubgroupLinkRecord> =
        (local + remote)
            .groupBy { it.group }
            .map { (_, records) -> records.reduce(::prefer) }
            .sortedBy { it.group }

    private fun prefer(
        x: SubgroupLinkRecord,
        y: SubgroupLinkRecord,
    ): SubgroupLinkRecord = Crdt.preferByHlc(x, x.hlc, y, y.hlc) { "${it.deleted}|${it.parent}" }
}
