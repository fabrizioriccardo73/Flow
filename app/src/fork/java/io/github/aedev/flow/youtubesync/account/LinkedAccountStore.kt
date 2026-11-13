package io.github.aedev.flow.youtubesync.account

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * The Linked Account of the running profile. Its tokens (AppAuth's serialized auth state) are sealed
 * before they are written; the channel shown in settings is stored in clear.
 * Lives in a profile-private, never-backed-up DataStore, so it does not travel with backups or Device Sync.
 */
class LinkedAccountStore(
    private val dataStore: DataStore<Preferences>,
    private val sealer: SecretSealer,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val accountKey = stringPreferencesKey("account")
    private val authStateKey = stringPreferencesKey("auth_state")

    val account: Flow<LinkedAccount?> =
        dataStore.data
            .map { prefs -> prefs[accountKey]?.let { runCatching { json.decodeFromString<LinkedAccount>(it) }.getOrNull() } }
            .distinctUntilChanged()

    /** The serialized auth state, or null when nothing is linked or it no longer opens. */
    suspend fun authState(): String? = sealer.open(dataStore.data.first()[authStateKey]).ifEmpty { null }

    suspend fun save(
        account: LinkedAccount,
        authState: String,
    ) {
        dataStore.edit {
            it[accountKey] = json.encodeToString(LinkedAccount.serializer(), account)
            it[authStateKey] = sealer.seal(authState)
        }
    }

    suspend fun updateAuthState(authState: String) {
        dataStore.edit { if (accountKey in it) it[authStateKey] = sealer.seal(authState) }
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(accountKey)
            it.remove(authStateKey)
        }
    }
}
