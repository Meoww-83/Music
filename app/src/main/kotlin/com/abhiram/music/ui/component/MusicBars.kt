package com.abhiram.music.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abhiram.music.ui.theme.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.sin

/** Default footprint of the equaliser when the caller does not size it (e.g. inside list rows). */
private val MusicBarsDefaultSize = 16.dp

/** Gap between two bars, kept hairline-thin so four bars fit into 14 dp boxes. */
private val MusicBarsSpacing = 1.5.dp

/** Bars breathe between 30 % and 100 % of the available height while playing. */
private const val MinBarFraction = 0.30f

/** Frozen (paused / reduce-motion) height of a bar: always low, with a slight per-bar variation. */
private const val PausedBarFraction = 0.35f

/** Extra height added to every third paused bar so the frozen state still reads as separate bars. */
private const val PausedBarJitter = 0.05f

/** One animation cycle; bars are phase-shifted inside the cycle instead of using per-bar timers. */
private const val MusicBarsCycleMs = 1_100

/**
 * Animated "now playing" equaliser: [barCount] rounded bars whose heights oscillate smoothly.
 *
 * The bars are drawn into a single [Canvas] and the wobble is evaluated from one infinite
 * transition during the *draw* phase, so playback animation never triggers recomposition.
 * Each bar is phase-shifted by `2π / barCount`, which keeps them out of sync without needing
 * per-bar animation specs or randomised seeds.
 *
 * When [isPlaying] is `false` the bars freeze at a low, deterministic height; the same frozen
 * rendering is used when [LocalReduceMotion] reports reduced motion — the wobble value is then
 * never read, so the canvas redraws nothing per frame.
 *
 * @param modifier sizing modifier. Callers usually pass a fixed `size`/`width`; when none is
 *   given the bars fall back to a square [MusicBarsDefaultSize] footprint so they still measure.
 * @param isPlaying `false` freezes the bars low.
 * @param barCount number of bars to draw (coerced to at least one).
 * @param color fill colour of every bar.
 */
@Composable
fun MusicBars(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    barCount: Int = 4,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val reduceMotion = LocalReduceMotion.current
    val animate = isPlaying && !reduceMotion

    val transition = rememberInfiniteTransition(label = "musicBars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = MusicBarsCycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "musicBarsPhase",
    )

    Canvas(
        modifier = modifier.size(MusicBarsDefaultSize),
    ) {
        val count = barCount.coerceAtLeast(1)
        val gap = MusicBarsSpacing.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1f)

        for (index in 0 until count) {
            val fraction = if (animate) {
                // One shared cycle, phase-shifted per bar → smooth, never-uniform bobbing.
                val angle = 2f * PI.toFloat() * (phase + index / count.toFloat())
                val wobble = 0.5f + 0.5f * sin(angle)
                MinBarFraction + (1f - MinBarFraction) * wobble
            } else {
                PausedBarFraction + PausedBarJitter * (index % 3)
            }
            val height = (size.height * fraction).coerceAtLeast(1f)
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap), size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(minOf(barWidth, height) / 2f),
            )
        }
    }
}
