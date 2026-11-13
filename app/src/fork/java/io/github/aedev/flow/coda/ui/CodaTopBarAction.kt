package io.github.aedev.flow.coda.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aedev.flow.R
import kotlinx.coroutines.flow.StateFlow

private const val BADGE_MAX = 99

/** The Coda's entry in every root top bar, counting its videos; opens the Coda from anywhere. */
@Composable
fun CodaTopBarAction(
    count: StateFlow<Int>,
    onClick: () -> Unit,
) {
    val videos by count.collectAsStateWithLifecycle()
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (videos > 0) Badge { Text(if (videos > BADGE_MAX) "$BADGE_MAX+" else videos.toString()) }
            },
        ) {
            Icon(Icons.AutoMirrored.Outlined.QueueMusic, contentDescription = stringResource(R.string.coda_title))
        }
    }
}
