package io.github.aedev.flow.coda

import io.github.aedev.flow.data.model.Video

/** What the player's "Next up" dock shows: the next video, its place in its list, and that list's name and size. */
data class DockNextUp(
    val video: Video?,
    val position: Int,
    val listName: String,
    val listSize: Int,
)

/**
 * The dock follows playback order: the Contesto's next video while it has one, then the Coda's,
 * named [codaName] and counted within the Coda.
 */
fun dockNextUp(
    contestoNext: IndexedValue<Video>?,
    contestoSize: Int,
    contestoTitle: String?,
    coda: Coda,
    currentVideoId: String?,
    codaName: String,
): DockNextUp {
    if (contestoNext != null) {
        return DockNextUp(contestoNext.value, contestoNext.index + 1, contestoTitle.orEmpty(), contestoSize)
    }
    val fromCoda = coda.nextAfter(currentVideoId) ?: return DockNextUp(null, 1, contestoTitle.orEmpty(), contestoSize)
    return DockNextUp(fromCoda, coda.videos.indexOf(fromCoda) + 1, codaName, coda.videos.size)
}
