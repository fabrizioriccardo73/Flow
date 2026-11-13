package io.github.aedev.flow.coda.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.aedev.flow.R

/** "Done": the playing Coda video leaves the Coda and the next video starts. */
@Composable
fun CodaDoneButton(onDone: () -> Unit) {
    IconButton(
        onClick = onDone,
        shapes = IconButtonDefaults.shapes(),
    ) {
        Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.coda_done_desc))
    }
}
