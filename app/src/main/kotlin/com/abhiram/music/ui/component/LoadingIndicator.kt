package com.abhiram.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abhiram.music.ui.theme.expressiveShapes
import com.abhiram.music.ui.theme.rememberReduceMotion
import androidx.compose.material3.LoadingIndicator as Material3LoadingIndicator

/**
 * The Material 3 Expressive morphing loading indicator: a 48 dp mark of [color].
 *
 * While motion is reduced (theme reduce-motion or a system animator scale of 0) the animated
 * M3 indicator is replaced by a static expressive pill of the same colour, so the state stays
 * legible without any movement. Pass a size through [modifier] to override the 48 dp default;
 * the colour defaults to the theme's `primary`.
 *
 * @param modifier Modifier applied to the indicator (e.g. `Modifier.size(32.dp)`).
 * @param color Fill colour of the indicator.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val reduceMotion = rememberScaffoldReduceMotion()
    if (reduceMotion) {
        // Static fallback: the morphing polygons never animate.
        Box(
            modifier = modifier
                .size(48.dp)
                .background(color, MaterialTheme.expressiveShapes.pill),
        )
    } else {
        Material3LoadingIndicator(
            modifier = modifier,
            color = color,
        )
    }
}

/**
 * Resolves whether animations should be reduced for this subtree.
 *
 * Thin adapter over the theme's `rememberReduceMotion` (which combines `LocalReduceMotion`
 * with the system animator-duration scale), kept under a package-local name so the scaffolds
 * never collide with the other feature packages' identically named helpers.
 */
@Composable
internal fun rememberScaffoldReduceMotion(): Boolean = rememberReduceMotion()
