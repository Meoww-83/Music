package com.abhiram.music.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhiram.music.ui.theme.LocalReduceMotion
import kotlin.math.abs

/** Diameter of the revealed action glyph. */
private val SwipeActionIconSize = 28.dp

/** Distance of the glyph from the edge it is revealed against. */
private val SwipeActionIconInset = 20.dp

/** Peak opacity of the tinted reveal bed behind the swiped content. */
private const val SwipeRevealTintAlpha = 0.14f

/** Glyph opacity at the very start of a drag; grows to 1f once the threshold is crossed. */
private const val SwipeIconRestAlpha = 0.25f

/** Glyph scale at the start of a drag. */
private const val SwipeIconRestScale = 0.7f

/**
 * Swipe-to-action row: drag the [content] sideways past [threshold] to fire [onAction].
 *
 * The row is a two-layer [Box]: a reveal bed (tinted [actionColor] with the [actionIcon] pinned
 * to the side the content is moving away from) and the swiped [content] itself, translated with
 * a graphics layer. Reveal opacity, glyph fade/scale and the translation are all evaluated in
 * *layer/draw* lambdas, so dragging never recomposes the row.
 *
 * Motion: while the finger is down the content tracks it exactly (`snap()`); on release — or
 * on cancel — it springs back to rest with a low-bouncy spring (instant under reduce motion).
 * If the release point is still at/behind [threshold], [onAction] fires exactly once before the
 * snap-back, after which the row is rearmed for the next gesture. The whole row is clipped to
 * its bounds so a swiped row never paints over its neighbours in a lazy list.
 *
 * @param onAction invoked once per gesture that ends past [threshold].
 * @param modifier sizing/positioning modifier; honouring it first lets list callers attach
 *   `animateItem()`, size reporting or elevation.
 * @param actionIcon glyph revealed behind the content while swiping.
 * @param actionColor accent for the glyph and the tinted reveal bed.
 * @param threshold drag distance (in each direction) that arms the action.
 * @param content the interactive row; receives the full [BoxScope] of the row.
 */
@Composable
fun SwipeToActionBox(
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector = Icons.Rounded.Check,
    actionColor: Color = MaterialTheme.colorScheme.primary,
    threshold: Dp = 96.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    val thresholdPx = LocalDensity.current.run { threshold.toPx() }
    val currentOnAction by rememberUpdatedState(onAction)

    var dragging by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(0f) }

    val animatedOffset by animateFloatAsState(
        targetValue = offset,
        animationSpec = if (dragging || reduceMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "swipeToActionOffset",
    )

    fun progress(): Float = (abs(offset) / thresholdPx).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(thresholdPx) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        offset = (offset + dragAmount.x).coerceIn(-width, width)
                    },
                    onDragEnd = {
                        if (abs(offset) >= thresholdPx) currentOnAction()
                        dragging = false
                        offset = 0f
                    },
                    onDragCancel = {
                        dragging = false
                        offset = 0f
                    },
                )
            },
    ) {
        // Reveal bed — grows with drag distance, fully transparent at rest.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = progress() }
                .background(actionColor.copy(alpha = SwipeRevealTintAlpha)),
        )

        // Glyph on the side being uncovered: dragged right → start side, left → end side.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = SwipeActionIconInset)
                .graphicsLayer {
                    val p = progress()
                    alpha = if (offset > 0f) SwipeIconRestAlpha + (1f - SwipeIconRestAlpha) * p else 0f
                    val scale = SwipeIconRestScale + (1f - SwipeIconRestScale) * p
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            Icon(
                imageVector = actionIcon,
                contentDescription = null,
                tint = actionColor,
                modifier = Modifier.size(SwipeActionIconSize),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = SwipeActionIconInset)
                .graphicsLayer {
                    val p = progress()
                    alpha = if (offset < 0f) SwipeIconRestAlpha + (1f - SwipeIconRestAlpha) * p else 0f
                    val scale = SwipeIconRestScale + (1f - SwipeIconRestScale) * p
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            Icon(
                imageVector = actionIcon,
                contentDescription = null,
                tint = actionColor,
                modifier = Modifier.size(SwipeActionIconSize),
            )
        }

        // Swiped layer: follows the finger, springs back on release.
        Box(modifier = Modifier.graphicsLayer { translationX = animatedOffset }) {
            content()
        }
    }
}
