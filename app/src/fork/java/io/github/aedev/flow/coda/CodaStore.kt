package io.github.aedev.flow.coda

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aedev.flow.data.local.safePreferencesDataStore
import io.github.aedev.flow.data.profile.StoreScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// Profile-private and outside the Device Sync: the Coda is Device-local.
private val Context.codaStore by safePreferencesDataStore(name = "video_coda", scope = StoreScope.PROFILE_PRIVATE)
private val CodaKey = stringPreferencesKey("coda_json")

/** Keeps the Coda across process death and reboots. */
@Singleton
class CodaStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        suspend fun load(): Coda =
            context.codaStore.data
                .first()[CodaKey]
                ?.let(CodaCodec::decode) ?: Coda()

        suspend fun save(coda: Coda) {
            context.codaStore.edit { prefs ->
                if (coda.isEmpty) prefs.remove(CodaKey) else prefs[CodaKey] = CodaCodec.encode(coda)
            }
        }
    }
