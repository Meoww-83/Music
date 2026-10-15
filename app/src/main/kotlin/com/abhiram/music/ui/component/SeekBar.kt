package com.abhiram.music.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Total touch height — the whole (invisible) box is draggable, well past the visual line. */
private val SeekTouchHeight = 48.dp

/** Stroke weight of the track / progress line. */
private val SeekStrokeWidth = 3.dp

/** Half height of the sine wave used when [SeekBar] draws in squiggly mode. */
private val SeekWaveAmplitude = 5.dp

/** One full squiggle cycle. */
private val SeekWavelength = 32.dp

/** Resting radius of the progress thumb (it springs up while dragging). */
private val SeekThumbRadius = 4.dp

/** Thumb scale while the finger is down. */
private const val SeekDragThumbScale = 1.7f

/** Mute factor for the whole line when the bar is disabled. */
private const val SeekDisabledAlpha = 0.38f

/**
 * Draggable playback position line, with an optional squiggly (waveform-style) rendering.
 *
 * Layout: a 48 dp tall hit area wraps a centred [Canvas]. The canvas draws, back to front — the
 * plain `surfaceVariant` track, the buffered span in a dimmed primary, the played span in
 * primary, and finally a spring-snapped thumb that follows the wave in squiggly mode. The same
 * sine path is drawn three times and clipped, so buffered/played portions always line up
 * perfectly with the track.
 *
 * Interaction: a tap seeks immediately; a drag previews the new position under the finger
 * (the thumb pops up with a bouncy spring) and commits `onSeek((ratio * durationMs).toLong())`
 * on release. `enabled = false` keeps the line visible but mutes it and removes all gestures —
 * call sites use that while `durationMs` is still unknown.
 *
 * @param positionMs current playback position.
 * @param durationMs total duration; zero disables seeking and collapses progress to `0f`.
 * @param onSeek target position (ms) derived from the released ratio.
 * @param modifier sizing/positioning modifier (e.g. `Modifier.fillMaxWidth()`).
 * @param enabled `false` disables tap/drag seeking and dims the line.
 * @param squiggly `true` draws the wavy progress line, `false` a straight one.
 * @param bufferedPercentage 0…100 portion of the track reported as buffered.
 */
@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    squiggly: Boolean = true,
    bufferedPercentage: Int = 0,
) {
    val duration = durationMs.coerceAtLeast(0L)
    val progress =
        if (duration > 0L) (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    // `null` while idle: the drawn position then falls back to the real playback progress.
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    var dragging by remember { mutableStateOf(false) }

    val thumbScale by animateFloatAsState(
        targetValue = if (dragging) SeekDragThumbScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "seekBarThumb",
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val bufferedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val progressColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SeekTouchHeight)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }
            // Tap to seek.
            .pointerInput(enabled, duration) {
                if (!enabled || duration <= 0L) return@pointerInput
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek((ratio * duration).toLong())
                }
            }
            // Drag to preview, release to commit.
            .pointerInput(enabled, duration) {
                if (!enabled || duration <= 0L) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragProgress?.let { ratio -> onSeek((ratio * duration).toLong()) }
                        dragging = false
                        dragProgress = null
                    },
                    onDragCancel = {
                        dragging = false
                        dragProgress = null
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = SeekStrokeWidth.toPx()
            val style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val centerY = size.height / 2f
            val amplitude = SeekWaveAmplitude.toPx()
            val wavelength = SeekWavelength.toPx()

            fun yAt(x: Float): Float = if (squiggly) {
                centerY + amplitude * sin(2.0 * PI * (x / wavelength)).toFloat()
            } else {
                centerY
            }

            val path = Path().apply {
                moveTo(0f, yAt(0f))
                if (squiggly) {
                    var x = 2f
                    while (x < size.width) {
                        lineTo(x, yAt(x))
                        x += 2f
                    }
                }
                lineTo(size.width, yAt(size.width))
            }

            val alpha = if (enabled) 1f else SeekDisabledAlpha

            // 1. full track
            drawPath(
                path = path,
                color = trackColor.copy(alpha = trackColor.alpha * alpha),
                style = style,
            )

            // 2. buffered portion, dimmer than the played span
            val bufferedPx = size.width * (bufferedPercentage.coerceIn(0, 100) / 100f)
            if (bufferedPx > 0f) {
                clipRect(right = bufferedPx) {
                    drawPath(
                        path = path,
                        color = bufferedColor.copy(alpha = bufferedColor.alpha * alpha),
                        style = style,
                    )
                }
            }

            // 3. played portion
            val shown = dragProgress ?: progress
            val progressX = shown * size.width
            clipRect(right = progressX) {
                drawPath(
                    path = path,
                    color = progressColor.copy(alpha = progressColor.alpha * alpha),
                    style = style,
                )
            }

            // 4. spring-snapped thumb (sits on the wave in squiggly mode)
            drawCircle(
                color = progressColor.copy(alpha = progressColor.alpha * alpha),
                radius = SeekThumbRadius.toPx() * thumbScale,
                center = Offset(progressX, yAt(progressX)),
            )
        }
    }
}
