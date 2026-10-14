package com.abhiram.music.ui.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhiram.music.ui.component.ShimmerHost
import kotlin.math.abs

/**
 * Square album artwork with slightly rounded corners (design spec) used by the full player.
 *
 * Behaviour:
 * * **shimmer while loading** — a [ShimmerHost] placeholder covers the art until Coil delivers it;
 * * **palette-accented ring** — a thin border sampled from the artwork's own colours (falls back
 *   to [ringColor]) is drawn on top of the image so it is never covered;
 * * **press-to-release** — the art compresses with a bouncy spring while held and springs back;
 * * **drag gestures** — when [interactive] is enabled, pointer movement is reported through
 *   [onDrag] (per-event delta) and [onDragEnd] (cumulative offset), which is how the full player
 *   implements vertical-drag-to-dismiss and horizontal-drag-to-skip.
 *
 * The composable itself never changes layout while dragging; the caller owns the offsets so it can
 * choose what a gesture means (dismiss, skip, carousel paging…).
 *
 * @param thumbnailUrl artwork URL, or `null` for the neutral placeholder.
 * @param modifier sizing modifier — the caller decides the drawn size (square aspect is natural).
 * @param contentDescription accessibility label for the artwork.
 * @param cornerRadius slightly-rounded corner radius from the design spec.
 * @param ringColor fallback colour of the accent ring when no palette could be extracted.
 * @param showRing draws the palette-accented ring around the art.
 * @param interactive enables press + drag reporting.
 * @param onTap invoked on a tap (also drives the press-to-release feedback).
 * @param onDrag per-event drag delta, in pixels.
 * @param onDragEnd cumulative drag offset when the gesture is released (or cancelled).
 */
@Composable
fun Thumbnail(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    cornerRadius: Dp = 14.dp,
    ringColor: Color = MaterialTheme.colorScheme.primary,
    showRing: Boolean = true,
    interactive: Boolean = false,
    onTap: (() -> Unit)? = null,
    onDrag: ((delta: Offset) -> Unit)? = null,
    onDragEnd: ((total: Offset) -> Unit)? = null,
) {
    val artwork = rememberArtworkState(thumbnailUrl)
    val reduceMotion = rememberReduceMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.945f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "thumbnailPress",
    )
    val shape = RoundedCornerShape(cornerRadius)
    val palette = (artwork.value as? ArtworkState.Ready)?.palette
    var totalDrag by remember { mutableStateOf(Offset.Zero) }
    // Kept in a holder so the pointer input can be keyed on stable values only; keying on the
    // caller's lambdas would cancel an in-flight drag on every recomposition.
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val gestureModifier = if (interactive) {
        Modifier.pointerInput(interactive) {
            detectDragGestures(
                onDragStart = { totalDrag = Offset.Zero },
                onDrag = { change, amount ->
                    change.consume()
                    totalDrag += amount
                    currentOnDrag?.invoke(amount)
                },
                onDragEnd = { currentOnDragEnd?.invoke(totalDrag) },
                onDragCancel = { currentOnDragEnd?.invoke(Offset.Zero) },
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black)
            .then(
                if (onTap != null || interactive) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onTap?.invoke() },
                    )
                } else {
                    Modifier
                },
            )
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(gestureModifier),
        contentAlignment = Alignment.Center,
    ) {
        ArtworkContent(
            artwork = artwork,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
        )

        if (showRing) {
            // Drawn last so the ring is never covered by the image.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        drawContent()
                        val strokeWidth = 1.5.dp.toPx()
                        val inset = strokeWidth / 2f
                        drawRect(
                            brush = if (palette != null) {
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(palette.primary, palette.secondary, palette.tertiary),
                                )
                            } else {
                                SolidColor(ringColor)
                            },
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            style = Stroke(width = strokeWidth),
                        )
                    },
            )
        }
    }
}

/**
 * Circular playback progress ring drawn around the **circular** album art of the mini player
 * (design spec: “show progress bar around the circle album art”).
 *
 * The art is loaded through the same Coil + palette pipeline as [Thumbnail]; while it loads a
 * shimmer fills the circle, and when [isPlaying] is `false` a translucent scrim with a play glyph
 * makes the paused state readable at a glance.
 *
 * @param thumbnailUrl artwork URL of the current track.
 * @param progress playback progress in the range `0f..1f`.
 * @param isPlaying whether audio is currently playing (draws the paused overlay when `false`).
 * @param modifier sizing modifier; give it an explicit size (e.g. `Modifier.size(52.dp)`), or it
 * will collapse to the incoming constraints.
 * @param ringColor colour of the progress arc; the track is the same colour at 25 % alpha.
 */
@Composable
fun ProgressRing(
    thumbnailUrl: String?,
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    ringColor: Color = MaterialTheme.colorScheme.primary,
) {
    val artwork = rememberArtworkState(thumbnailUrl)
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "progressRing",
    )
    val ringInset = 5.dp

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            val inset = strokeWidth / 2f + 1.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val arcTopLeft = Offset(inset, inset)
            drawArc(
                color = ringColor.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            if (animatedProgress > 0.001f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(ringInset)
                .clip(CircleShape)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkContent(
                artwork = artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize(fraction = 0.5f),
                    )
                }
            }
        }
    }
}

/**
 * Renders one [ArtworkState]: shimmer placeholder while loading, neutral placeholder on failure,
 * the decoded bitmap (crop-scaled) once ready. Used by both [Thumbnail] and [ProgressRing].
 */
@Composable
internal fun ArtworkContent(
    artwork: androidx.compose.runtime.State<ArtworkState>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val state = artwork.value
    when (state) {
        is ArtworkState.Loading -> {
            ShimmerHost(modifier = modifier) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
        }

        is ArtworkState.Failed -> {
            Box(
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize(fraction = 0.42f),
                )
            }
        }

        is ArtworkState.Ready -> {
            val alpha by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "artworkFadeIn",
            )
            Image(
                bitmap = state.image,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier.graphicsLayer { this.alpha = alpha },
            )
        }
    }
}

/** Distance in pixels a full-player drag must travel before a gesture counts as “intent”. */
internal val DragSkipThresholdPx = 110f

/** Fraction of the height a vertical drag must cover before the player dismisses. */
internal val DragDismissHeightFraction = 0.28f

/** Whether a drag offset has moved far enough to be visible as a skip hint. */
internal fun isSkipIntent(total: Offset): Boolean = abs(total.x) > DragSkipThresholdPx
