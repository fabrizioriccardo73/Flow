package io.github.aedev.flow.coda.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import io.github.aedev.flow.ui.components.shared.ReorderableLazyListState
import io.github.aedev.flow.ui.components.shared.rememberReorderableLazyListState

internal class KeyedRow<T>(
    val key: String,
    val value: T,
)

/** One section's rows as shown while a drag is under way, and the drag that moves them. */
internal class ReorderableSection<T>(
    val items: SnapshotStateList<KeyedRow<T>>,
    val state: ReorderableLazyListState,
)

/**
 * A section of a shared list that reorders on its own: the rows move as the finger does, and
 * [onMove] hears the whole move once the drag ends. A drag never leaves the section.
 */
@Composable
internal fun <T> rememberReorderableSection(
    items: List<T>,
    keys: List<String>,
    listState: LazyListState,
    indexOffset: Int,
    onMove: (Int, Int) -> Unit,
): ReorderableSection<T> {
    val rows = remember(items) { items.zip(keys) { item, key -> KeyedRow(key, item) }.toMutableStateList() }
    var pendingFrom by remember(items) { mutableStateOf<Int?>(null) }
    var pendingTo by remember(items) { mutableStateOf<Int?>(null) }
    val state =
        rememberReorderableLazyListState(
            listState = listState,
            itemIndexOffset = indexOffset,
            onMove = { from, to ->
                if (pendingFrom == null) pendingFrom = from
                pendingTo = to
                rows.add(to, rows.removeAt(from))
            },
            onDragStopped = {
                val from = pendingFrom
                val to = pendingTo
                pendingFrom = null
                pendingTo = null
                if (from != null && to != null && from != to) onMove(from, to)
            },
            canMoveTo = { index -> index in rows.indices },
        )
    return remember(rows, state) { ReorderableSection(rows, state) }
}

@Composable
internal fun ContestoHeader(contesto: ContestoSection) {
    val name = contesto.title ?: stringResource(R.string.playlist_queue)
    SectionHeader(
        title = stringResource(R.string.coda_next_from, name),
        subtitle = stringResource(R.string.queue_position_template, contesto.currentIndex + 1, contesto.videos.size),
    ) {
        ModeToggle(contesto.isShuffled, contesto.onShuffleToggle, Icons.Rounded.Shuffle, stringResource(R.string.shuffle))
        ModeToggle(contesto.isLooping, contesto.onLoopToggle, Icons.Rounded.Repeat, stringResource(R.string.repeat))
    }
}

@Composable
internal fun CodaHeader(coda: CodaSection) {
    val count = coda.coda.videos.size
    SectionHeader(
        title = stringResource(R.string.coda_title),
        subtitle = if (count > 0) pluralCount(count) else null,
    ) {
        coda.onStart?.takeIf { count > 0 }?.let { start ->
            TextButton(onClick = start) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.play), modifier = Modifier.padding(start = 4.dp))
            }
        }
        if (count > 0) {
            TextButton(onClick = coda.onClear) { Text(stringResource(R.string.coda_clear)) }
        }
    }
}

@Composable
internal fun CodaEmpty() {
    Text(
        text = stringResource(R.string.coda_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun pluralCount(count: Int): String = pluralStringResource(R.plurals.coda_video_count, count, count)

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String?,
    actions: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        actions()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModeToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
) {
    FilledTonalIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = IconButtonDefaults.toggleableShapes(),
    ) {
        Icon(imageVector = imageVector, contentDescription = contentDescription)
    }
}
