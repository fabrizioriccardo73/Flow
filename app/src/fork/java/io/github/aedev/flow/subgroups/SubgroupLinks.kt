package io.github.aedev.flow.subgroups

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.aedev.flow.FlowApplication
import io.github.aedev.flow.data.local.safePreferencesDataStore
import io.github.aedev.flow.data.profile.StoreScope
import io.github.aedev.flow.sync.identity.DeviceIdentity
import io.github.aedev.flow.sync.identity.Hlc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Per profile, copied to a new phone like the library; travels with the Device Sync as fork_group_parents (ADR 0004).
private val Context.subgroupLinksStore by safePreferencesDataStore(name = "fork_group_parents", scope = StoreScope.PROFILE_LIBRARY)
private val RecordsKey = stringPreferencesKey("records_json")

/** Which Group each Subgroup sits in. Raw links: [SubgroupTree] decides which of them hold. */
@Singleton
class SubgroupLinks
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val identity: DeviceIdentity,
    ) {
        private val store get() = context.subgroupLinksStore

        /** Subgroup name to parent name, without tombstones. */
        val links: Flow<Map<String, String>> =
            store.data
                .map { prefs -> SubgroupLinkRecords.live(SubgroupLinkRecords.decode(prefs[RecordsKey])) }
                .distinctUntilChanged()

        suspend fun records(): List<SubgroupLinkRecord> = SubgroupLinkRecords.decode(store.data.first()[RecordsKey])

        /** Records that the Group [oldName] (null for a new one) was saved as [newName] inside [parent] (null for none). */
        suspend fun saved(
            oldName: String?,
            newName: String,
            parent: String?,
        ) {
            val stamp = stamp()
            edit { SubgroupLinkRecords.afterSave(it, oldName, newName, parent, stamp) }
        }

        /** Records that the Group [name] was deleted; its Subgroups become plain Groups. */
        suspend fun deleted(name: String) {
            val stamp = stamp()
            edit { SubgroupLinkRecords.afterDelete(it, name, stamp) }
        }

        /** Merges what another device sent; returns how many records arrived. */
        suspend fun applyRemote(remote: List<SubgroupLinkRecord>): Int {
            edit { SubgroupLinkWire.merge(it, remote) }
            remote.maxOfOrNull { Hlc.decode(it.hlc).physicalMs }?.let { identity.observe(it) }
            return remote.size
        }

        private suspend fun stamp(): String = Hlc(identity.editTime(), 0, identity.hlcNode()).encode()

        private suspend fun edit(change: (List<SubgroupLinkRecord>) -> List<SubgroupLinkRecord>) {
            store.edit { prefs ->
                val before = SubgroupLinkRecords.decode(prefs[RecordsKey])
                val after = change(before)
                if (after != before) prefs[RecordsKey] = SubgroupLinkRecords.encode(after)
            }
        }

        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface Access {
            fun subgroupLinks(): SubgroupLinks
        }

        companion object {
            /** The store itself, for the fork's own UI hooked into the original group dialogs. */
            fun from(context: Context): SubgroupLinks =
                EntryPointAccessors.fromApplication(context.applicationContext, Access::class.java).subgroupLinks()

            /**
             * The links for the original Subscriptions code, reached through a `// fork` line so its
             * ViewModel keeps its constructor. Empty where the app never started, as in JVM tests.
             */
            fun observe(): Flow<Map<String, String>> =
                runCatching {
                    EntryPointAccessors.fromApplication(FlowApplication.appContext, Access::class.java).subgroupLinks().links
                }.getOrElse { emptyFlow() }
        }
    }
