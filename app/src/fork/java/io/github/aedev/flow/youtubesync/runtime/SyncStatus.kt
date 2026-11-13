package io.github.aedev.flow.youtubesync.runtime

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId

/** Why the YouTube Sync is not running normally. */
enum class SyncProblem {
    /** The daily API quota is used up: it resumes after the reset, quietly. */
    QUOTA,

    /** The authorization was revoked or expired: the viewer must link the account again. */
    NEEDS_RELINK,

    /** A background run would remove too many items: it waits for the viewer to look at the preview. */
    NEEDS_CONFIRMATION,

    /** Network or server trouble: retried with a growing wait. */
    OFFLINE,
}

/** Where the YouTube Sync stands, for the status indicator (spec: "Tempistiche"). */
@Serializable
data class SyncStatus(
    val lastSuccessAt: Long? = null,
    val problem: SyncProblem? = null,
    /** With [SyncProblem.QUOTA]: when to try again. */
    val retryAt: Long? = null,
)

/** The [SyncStatus] of the running profile, on this device only. */
class SyncStatusStore(
    private val dataStore: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("status")

    val status: Flow<SyncStatus> =
        dataStore.data.map { prefs ->
            prefs[key]?.let { runCatching { json.decodeFromString<SyncStatus>(it) }.getOrNull() } ?: SyncStatus()
        }

    suspend fun read(): SyncStatus = status.first()

    suspend fun update(change: (SyncStatus) -> SyncStatus) {
        dataStore.edit { prefs ->
            val current = prefs[key]?.let { runCatching { json.decodeFromString<SyncStatus>(it) }.getOrNull() } ?: SyncStatus()
            prefs[key] = json.encodeToString(SyncStatus.serializer(), change(current))
        }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }
}

/** The YouTube Data API quota resets at midnight Pacific time; a few minutes after that, to be safe. */
fun nextQuotaReset(nowMs: Long): Long {
    val zone = ZoneId.of("America/Los_Angeles")
    val nextMidnight =
        Instant
            .ofEpochMilli(nowMs)
            .atZone(zone)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(zone)
    return nextMidnight.toInstant().toEpochMilli() + QUOTA_RESET_MARGIN_MS
}

private const val QUOTA_RESET_MARGIN_MS = 10 * 60 * 1000L
