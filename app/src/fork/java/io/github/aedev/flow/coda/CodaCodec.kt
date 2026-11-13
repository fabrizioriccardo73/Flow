package io.github.aedev.flow.coda

import io.github.aedev.flow.data.model.Video
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val CodaJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class StoredCoda(
    val items: List<StoredCodaItem>,
    val playingId: String? = null,
)

@Serializable
private data class StoredCodaItem(
    val id: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val thumbnailUrl: String,
    val duration: Int,
    val isMusic: Boolean = false,
)

/** The Coda as stored text, and back. Only what a queue row shows is kept. */
internal object CodaCodec {
    fun encode(coda: Coda): String =
        CodaJson.encodeToString(
            StoredCoda.serializer(),
            StoredCoda(coda.videos.map { it.toStored() }, coda.playingId),
        )

    /** Null when [raw] is not a stored Coda. */
    fun decode(raw: String): Coda? {
        val stored = runCatching { CodaJson.decodeFromString(StoredCoda.serializer(), raw) }.getOrNull() ?: return null
        val videos = stored.items.map { it.toVideo() }.distinctBy(Video::id)
        return Coda(videos, stored.playingId?.takeIf { id -> videos.any { it.id == id } })
    }
}

private fun Video.toStored() = StoredCodaItem(id, title, channelName, channelId, thumbnailUrl, duration, isMusic)

private fun StoredCodaItem.toVideo() =
    Video(
        id = id,
        title = title,
        channelName = channelName,
        channelId = channelId,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        viewCount = 0,
        uploadDate = "",
        isMusic = isMusic,
    )
