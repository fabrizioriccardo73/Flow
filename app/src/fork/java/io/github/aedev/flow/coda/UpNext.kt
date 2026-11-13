package io.github.aedev.flow.coda

import io.github.aedev.flow.data.model.Video

/** Where the next video comes from. */
enum class UpNextSource {
    CONTESTO,
    CODA,
    AUTOPLAY,
}

data class UpNext(
    val video: Video,
    val source: UpNextSource,
)

/**
 * What plays once [currentVideoId] is over: the rest of the Contesto di riproduzione first, then the
 * Coda, then the Autoplay. A video put in with "Play next" sits in the Contesto right after the
 * current one, so it comes first without an exception here.
 */
fun upNext(
    contestoNext: Video?,
    coda: Coda,
    currentVideoId: String?,
    autoplayNext: Video?,
): UpNext? =
    contestoNext?.let { UpNext(it, UpNextSource.CONTESTO) }
        ?: coda.nextAfter(currentVideoId)?.let { UpNext(it, UpNextSource.CODA) }
        ?: autoplayNext?.let { UpNext(it, UpNextSource.AUTOPLAY) }
