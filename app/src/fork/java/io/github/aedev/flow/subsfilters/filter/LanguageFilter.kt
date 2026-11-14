package io.github.aedev.flow.subsfilters.filter

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aedev.flow.data.local.safePreferencesDataStore
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.profile.StoreScope
import io.github.aedev.flow.subsfilters.language.ChannelLanguages
import io.github.aedev.flow.subsfilters.language.VideoLanguages
import io.github.aedev.flow.subsfilters.language.languageFilterAllows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Profile-private and outside the Device Sync: the Language filter is Device-local.
private val Context.languageFilterStore by safePreferencesDataStore(name = "fork_language_filter", scope = StoreScope.PROFILE_PRIVATE)
private val SelectedKey = stringSetPreferencesKey("selected_languages")

/** The Language filter of the Subscriptions section, shared by the phone and the TV screens. */
@Singleton
class LanguageFilter
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        channelLanguages: ChannelLanguages,
    ) {
        private val videoLanguages = VideoLanguages()

        val selected: Flow<Set<String>> = context.languageFilterStore.data.map { it[SelectedKey].orEmpty() }

        val channelLanguages: Flow<Map<String, String>> = channelLanguages.languages

        private val _available = MutableStateFlow<Set<String>>(emptySet())

        /** The languages of what the section currently holds, offered by the filter chip. */
        val available: StateFlow<Set<String>> = _available.asStateFlow()

        suspend fun select(languages: Set<String>) {
            context.languageFilterStore.edit { it[SelectedKey] = languages }
        }

        fun reportAvailable(languages: Set<String>) {
            _available.value = languages
        }

        fun languageOf(
            video: Video,
            channelLanguages: Map<String, String>,
        ): String? = videoLanguages.of(video, channelLanguages)

        /** [videos] the filter lets through, and the languages found among all of them. */
        fun apply(
            videos: List<Video>,
            selected: Set<String>,
            channelLanguages: Map<String, String>,
        ): Pair<List<Video>, Set<String>> {
            val found = HashSet<String>()
            val shown =
                videos.filter { video ->
                    val language = languageOf(video, channelLanguages)
                    language?.let(found::add)
                    languageFilterAllows(language, selected)
                }
            return shown to found
        }
    }
