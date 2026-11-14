package io.github.aedev.flow.subsfilters.language

import io.github.aedev.flow.data.model.Video
import java.util.concurrent.ConcurrentHashMap

/**
 * The Language of a video: its channel's language when the user gave one, otherwise the one detected
 * from the video itself, otherwise unknown (null).
 */
class VideoLanguages(
    private val detect: (title: String, description: String) -> String? = LanguageDetector::detect,
) {
    /** Detection depends only on the video, so it runs once per video id. */
    private val detected = ConcurrentHashMap<String, Detected>()

    private class Detected(
        val language: String?,
    )

    fun of(
        video: Video,
        channelLanguages: Map<String, String>,
    ): String? =
        channelLanguages[video.channelId]
            ?: detected.getOrPut(video.id) { Detected(detect(video.title, video.description)) }.language
}

/**
 * The Language filter: with no language selected everything shows; otherwise a video shows when its
 * language is selected or unknown, so the filter never hides a video it cannot place.
 */
fun languageFilterAllows(
    language: String?,
    selected: Set<String>,
): Boolean = selected.isEmpty() || language == null || language in selected
