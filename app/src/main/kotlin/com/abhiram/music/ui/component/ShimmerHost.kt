package com.abhiram.music.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.drawBehind
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Wraps skeleton [content] in an infinite shimmer sweep.
 *
 * A translucent highlight built from [MaterialTheme.colorScheme.surfaceContainerHigh] travels
 * from left to right across the whole host, making the placeholder blocks underneath read as a
 * loading state. Content is always drawn first, so the shimmer behaves as an overlay and any
 * colour the skeleton paints itself (rounded blocks, [TextPlaceholder] lines) stays visible.
 *
 * Pass `enabled = false` to render [content] completely unchanged — call sites use this to
 * honour the system "reduce motion" setting (see the `rememberReduceMotion()` helpers used
 * across the UI) without having to branch their layout code.
 *
 * @param modifier sizing/positioning modifier for the shimmering container.
 * @param enabled `false` disables the animated sweep and renders [content] statically.
 * @param content the skeleton to shimmer; drawn inside a [Box] scope so it can align itself.
 */
@Composable
fun ShimmerHost(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!enabled) {
        Box(modifier = modifier, content = content)
        return
    }

    val transition = rememberInfiniteTransition(label = "shimmerHost")
    val fraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerHostFraction",
    )
    val highlight = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(modifier = modifier) {
        content()
        // Drawn (and re-drawn every frame) outside the composition scope: reading the animated
        // fraction inside the draw lambda only invalidates the draw phase.
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    if (size.width <= 0f) return@drawBehind
                    val band = size.width
                    val centre = size.width * (fraction * 3f - 1f)
                    val brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            highlight.copy(alpha = 0.85f),
                            Color.Transparent,
                        ),
                        startX = centre - band,
                        endX = centre + band,
                    )
                    drawRect(brush = brush, topLeft = Offset.Zero)
                },
        )
    }
}
