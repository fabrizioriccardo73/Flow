package io.github.aedev.flow.subsfilters.sync

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.aedev.flow.subgroups.SubgroupLinkWire
import io.github.aedev.flow.subgroups.SubgroupLinks
import io.github.aedev.flow.subsfilters.language.ChannelLanguages
import io.github.aedev.flow.sync.protocol.ApplyStats
import io.github.aedev.flow.sync.protocol.CollectionWire

/**
 * The fork's collections in the Device Sync (Language of the channel, Subgroup links), reached from
 * the original sync code through `// fork` lines. Looked up through an entry point so the original
 * classes keep their constructors.
 */
class ForkSync(
    private val channelLanguages: ChannelLanguages,
    private val subgroupLinks: SubgroupLinks,
) {
    /** What this device sends for [collection], one of [COLLECTIONS]. */
    suspend fun export(collection: String): CollectionWire? =
        when (collection) {
            ChannelLanguageRecords.COLLECTION -> ChannelLanguageRecords.encode(channelLanguages.records())
            SubgroupLinkWire.COLLECTION -> SubgroupLinkWire.encode(subgroupLinks.records())
            else -> null
        }

    /** Merges what another device sent for [collection], one of [COLLECTIONS]. */
    suspend fun apply(
        collection: String,
        lines: List<String>,
    ): ApplyStats =
        when (collection) {
            ChannelLanguageRecords.COLLECTION -> ApplyStats(updated = channelLanguages.applyRemote(ChannelLanguageRecords.decode(lines)))
            SubgroupLinkWire.COLLECTION -> ApplyStats(updated = subgroupLinks.applyRemote(SubgroupLinkWire.decode(lines)))
            else -> ApplyStats()
        }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Access {
        fun channelLanguages(): ChannelLanguages

        fun subgroupLinks(): SubgroupLinks
    }

    companion object {
        /** The fork's collections, added to the ones Android exchanges. */
        val COLLECTIONS: List<String> = listOf(ChannelLanguageRecords.COLLECTION, SubgroupLinkWire.COLLECTION)

        /** The field that keys one record of [collection], for the change batches. */
        fun keyField(collection: String): String? =
            when (collection) {
                ChannelLanguageRecords.COLLECTION -> "channelId"
                SubgroupLinkWire.COLLECTION -> SubgroupLinkWire.KEY_FIELD
                else -> null
            }

        fun of(context: Context): ForkSync {
            val access = EntryPointAccessors.fromApplication(context.applicationContext, Access::class.java)
            return ForkSync(access.channelLanguages(), access.subgroupLinks())
        }
    }
}
