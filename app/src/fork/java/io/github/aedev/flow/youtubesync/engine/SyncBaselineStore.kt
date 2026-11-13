package io.github.aedev.flow.youtubesync.engine

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The last state Flow and YouTube agreed on (glossary: Sync Baseline). */
@Serializable
data class SyncBaseline(
    val subscriptions: Set<String> = emptySet(),
)

/**
 * Stores the Sync Baseline of the running profile on this device only (profile-private, never backed
 * up, outside the Device Sync), so a stale baseline from elsewhere can never cause removals (ADR 0002).
 */
class SyncBaselineStore(
    private val dataStore: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("baseline")

    /** The baseline, or null when this device has none yet (first link). */
    suspend fun read(): SyncBaseline? =
        dataStore.data.first()[key]?.let { runCatching { json.decodeFromString<SyncBaseline>(it) }.getOrNull() }

    suspend fun write(baseline: SyncBaseline) {
        dataStore.edit { it[key] = json.encodeToString(SyncBaseline.serializer(), baseline) }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }
}
