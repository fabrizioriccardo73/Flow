package io.github.aedev.flow.subsfilters

import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.subsfilters.language.VideoLanguages
import io.github.aedev.flow.subsfilters.language.languageFilterAllows
import org.junit.Test

class LanguageFilterTest {
    private fun video(
        id: String,
        channelId: String,
        title: String = "",
    ) = Video(id = id, title = title, channelName = "", channelId = channelId, thumbnailUrl = "", duration = 0, viewCount = 0, uploadDate = "")

    @Test
    fun `the channel's language wins over the detected one`() {
        val languages = VideoLanguages(detect = { _, _ -> "en" })
        assertThat(languages.of(video("v", "c"), mapOf("c" to "it"))).isEqualTo("it")
        assertThat(languages.of(video("v", "other"), mapOf("c" to "it"))).isEqualTo("en")
    }

    @Test
    fun `detection runs once per video`() {
        var calls = 0
        val languages = VideoLanguages(detect = { _, _ -> calls++; null })
        repeat(3) { languages.of(video("v", "c"), emptyMap()) }
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `nothing selected shows everything`() {
        assertThat(languageFilterAllows("en", emptySet())).isTrue()
        assertThat(languageFilterAllows(null, emptySet())).isTrue()
    }

    @Test
    fun `a selection shows its languages and the unknown ones`() {
        val selected = setOf("it", "en")
        assertThat(languageFilterAllows("it", selected)).isTrue()
        assertThat(languageFilterAllows("en", selected)).isTrue()
        assertThat(languageFilterAllows(null, selected)).isTrue()
        assertThat(languageFilterAllows("es", selected)).isFalse()
    }
}
