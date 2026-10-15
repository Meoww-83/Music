package com.abhiram.music.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * Muted placeholder text used for empty states and skeleton rows.
 *
 * Renders [text] in a subdued tone of the current colour scheme so it reads as secondary
 * information: empty queue / search / lyrics states wrap it in a centred box, while the
 * [ShimmerHost] skeleton rows seed it with strings such as `"Placeholder title"`.
 *
 * The composable is purely presentational — it never intercepts pointer input, so it is safe
 * to place inside tappable containers.
 *
 * @param text the (possibly localised) message to show.
 * @param modifier styling/positioning modifier; applied to the root [Text] as-is.
 */
@Composable
fun TextPlaceholder(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
        textAlign = TextAlign.Center,
    )
}
