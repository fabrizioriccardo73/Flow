package io.github.aedev.flow.subsfilters.language

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aedev.flow.data.local.safePreferencesDataStore
import io.github.aedev.flow.data.profile.StoreScope
import io.github.aedev.flow.subsfilters.sync.ChannelLanguageRecord
import io.github.aedev.flow.subsfilters.sync.ChannelLanguageRecords
import io.github.aedev.flow.sync.identity.DeviceIdentity
import io.github.aedev.flow.sync.identity.Hlc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// Per profile, copied to a new phone like the library; it travels with the Device Sync (ADR 0004).
private val Context.channelLanguagesStore by safePreferencesDataStore(name = "fork_channel_languages", scope = StoreScope.PROFILE_LIBRARY)
private val RecordsKey = stringPreferencesKey("records_json")
private val recordsSerializer = ListSerializer(ChannelLanguageRecord.serializer())
private val json = Json { ignoreUnknownKeys = true }

/**
 * The Language of each channel the user assigned one to. Entries outlive an unsubscribe, so a channel
 * followed again gets its language back; a removed language stays as a tombstone for the Device Sync.
 */
@Singleton
class ChannelLanguages
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val identity: DeviceIdentity,
    ) {
        private val store get() = context.channelLanguagesStore

        /** Channel id to language code, without tombstones. */
        val languages: Flow<Map<String, String>> =
            store.data
                .map { prefs -> live(decode(prefs[RecordsKey])) }
                .distinctUntilChanged()

        suspend fun records(): List<ChannelLanguageRecord> = decode(store.data.first()[RecordsKey])

        /** Gives [channelId] [language], or removes its language with null. */
        suspend fun assign(
            channelId: String,
            language: String?,
        ) {
            val stamp = Hlc(identity.editTime(), 0, identity.hlcNode()).encode()
            val record =
                if (language == null) {
                    ChannelLanguageRecord(channelId = channelId, hlc = stamp, deleted = true)
                } else {
                    ChannelLanguageRecord(channelId = channelId, language = language, hlc = stamp)
                }
            store.edit { prefs ->
                val others = decode(prefs[RecordsKey]).filter { it.channelId != channelId }
                prefs[RecordsKey] = encode(others + record)
            }
        }

        /** Merges what another device sent; returns how many records arrived. */
        suspend fun applyRemote(remote: List<ChannelLanguageRecord>): Int {
            store.edit { prefs ->
                prefs[RecordsKey] = encode(ChannelLanguageRecords.merge(decode(prefs[RecordsKey]), remote))
            }
            remote.maxOfOrNull { Hlc.decode(it.hlc).physicalMs }?.let { identity.observe(it) }
            return remote.size
        }

        private fun decode(value: String?): List<ChannelLanguageRecord> =
            value?.let { runCatching { json.decodeFromString(recordsSerializer, it) }.getOrNull() }.orEmpty()

        private fun encode(records: List<ChannelLanguageRecord>): String = json.encodeToString(recordsSerializer, records.sortedBy { it.channelId })

        private fun live(records: List<ChannelLanguageRecord>): Map<String, String> =
            records.filter { !it.deleted && it.language.isNotBlank() }.associate { it.channelId to it.language }
    }
