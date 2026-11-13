package io.github.aedev.flow.coda.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import io.github.aedev.flow.coda.Coda
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.ui.components.shared.FlowBottomSheet
import io.github.aedev.flow.ui.components.shared.FlowSheetHeader
import io.github.aedev.flow.ui.components.shared.defaultSheetExpandedHeight
import io.github.aedev.flow.ui.components.shared.rememberFlowBottomSheetState
import io.github.aedev.flow.ui.components.videoplayer.sheet.PlaylistQueueItem
import io.github.aedev.flow.ui.components.videoplayer.sheet.queueRowKeys

/** The Contesto di riproduzione as the sheet shows it, with what the viewer can do to it. */
class ContestoSection(
    val videos: List<Video>,
    val currentIndex: Int,
    val title: String?,
    val isLooping: Boolean,
    val isShuffled: Boolean,
    val onLoopToggle: (Boolean) -> Unit,
    val onShuffleToggle: (Boolean) -> Unit,
    val onPlayAt: (Int) -> Unit,
    val onRemoveAt: (Int) -> Unit,
    val onMove: (Int, Int) -> Unit,
)

/** The Coda as the sheet shows it, with what the viewer can do to it. */
class CodaSection(
    val coda: Coda,
    val onPlay: (Video) -> Unit,
    val onRemove: (Video) -> Unit,
    val onMove: (Int, Int) -> Unit,
    val onClear: () -> Unit,
    /** Starts the Coda when nothing is playing; null while something is. */
    val onStart: (() -> Unit)?,
)

/**
 * What plays next, top to bottom in playback order: the rest of the Contesto di riproduzione,
 * then the Coda. Each section reorders, removes and plays its own videos.
 */
@Composable
fun CodaQueueSheet(
    contesto: ContestoSection,
    coda: CodaSection,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    expandedHeight: Dp? = null,
    collapsedHeight: Dp = 0.dp,
    enableVerticalDismiss: Boolean = true,
    onSheetProgressChange: (Float) -> Unit = {},
    footer: (@Composable () -> Unit)? = null,
) {
    val sheetState = rememberFlowBottomSheetState()
    val listState = rememberLazyListState()
    val hasContesto = contesto.videos.isNotEmpty()
    // Lazy indices: the Contesto's header and rows, then the Coda's header and rows.
    val contestoOffset = 1
    val codaOffset = if (hasContesto) contestoOffset + contesto.videos.size + 1 else 1

    val contestoRows =
        rememberReorderableSection(
            items = contesto.videos,
            keys = queueRowKeys(contesto.videos).map { "contesto-$it" },
            listState = listState,
            indexOffset = contestoOffset,
            onMove = contesto.onMove,
        )
    val codaRows =
        rememberReorderableSection(
            items = coda.coda.videos,
            keys = coda.coda.videos.map { "coda-${it.id}" },
            listState = listState,
            indexOffset = codaOffset,
            onMove = coda.onMove,
        )

    LaunchedEffect(contesto.currentIndex, hasContesto) {
        if (hasContesto && contesto.currentIndex in contesto.videos.indices) {
            listState.scrollToItem(contestoOffset + contesto.currentIndex)
        }
    }

    FlowBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        state = sheetState,
        expandedHeight = expandedHeight ?: defaultSheetExpandedHeight(),
        collapsedHeight = collapsedHeight,
        dismissible = enableVerticalDismiss,
        dismissOnOutsideTap = false,
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        onProgressChange = onSheetProgressChange,
        header = { dragModifier ->
            FlowSheetHeader(
                inSidePane = !enableVerticalDismiss,
                title = stringResource(R.string.next_up),
                onClose = { sheetState.dismiss() },
                modifier = dragModifier,
                titleMaxLines = 1,
            )
        },
    ) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (hasContesto) contestoItems(contesto, contestoRows)
            codaItems(coda, codaRows)
        }
        footer?.invoke()
    }
}

private fun LazyListScope.contestoItems(
    contesto: ContestoSection,
    rows: ReorderableSection<Video>,
) {
    item(key = "contesto-header") { ContestoHeader(contesto) }
    itemsIndexed(rows.items, key = { _, row -> row.key }) { index, row ->
        val isPlaying = index == contesto.currentIndex
        PlaylistQueueItem(
            video = row.value,
            isPlaying = isPlaying,
            isPlayed = index < contesto.currentIndex,
            reorderModifier = rows.state.itemModifier(index),
            dragHandleModifier = rows.state.handleModifier(index),
            onClick = { contesto.onPlayAt(index) },
            onRemove = if (isPlaying) null else ({ contesto.onRemoveAt(index) }),
            onMoveUp = if (index > 0) ({ contesto.onMove(index, index - 1) }) else null,
            onMoveDown = if (index < rows.items.lastIndex) ({ contesto.onMove(index, index + 1) }) else null,
        )
    }
}

private fun LazyListScope.codaItems(
    coda: CodaSection,
    rows: ReorderableSection<Video>,
) {
    item(key = "coda-header") { CodaHeader(coda) }
    if (rows.items.isEmpty()) {
        item(key = "coda-empty") { CodaEmpty() }
        return
    }
    itemsIndexed(rows.items, key = { _, row -> row.key }) { index, row ->
        val video = row.value
        PlaylistQueueItem(
            video = video,
            isPlaying = video.id == coda.coda.playingId,
            isPlayed = false,
            reorderModifier = rows.state.itemModifier(index),
            dragHandleModifier = rows.state.handleModifier(index),
            onClick = { coda.onPlay(video) },
            onRemove = { coda.onRemove(video) },
            onMoveUp = if (index > 0) ({ coda.onMove(index, index - 1) }) else null,
            onMoveDown = if (index < rows.items.lastIndex) ({ coda.onMove(index, index + 1) }) else null,
        )
    }
}
