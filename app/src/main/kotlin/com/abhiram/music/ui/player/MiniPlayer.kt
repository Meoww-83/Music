package com.abhiram.music.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhiram.music.playback.PlayerState
import com.abhiram.music.ui.glass.LiquidGlassSurface
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Collapsed player bar that sits above the bottom navigation (design spec §9).
 *
 * Rendered as a **pill-shaped liquid-glass** surface ([LiquidGlassSurface] falls back to a tinted
 * + bordered surface on API < 33) containing, left to right: the album/song title with a marquee,
 * the like button, play/pause, skip-to-next — and on the **right edge the circular album art with
 * a [ProgressRing] around it**. Tapping the bar opens the full player via [onOpen]; dragging it
 * **upwards** also opens the player (optional, see [swipeUpToOpen]).
 *
 * All playback actions go through the frozen [com.abhiram.music.playback.PlayerController]; the
 * optional callbacks exist so the host can intercept them (e.g. analytics) — when left `null` the
 * controller is called directly.
 *
 * @param playerState snapshot of the player; when `current` is `null` nothing is rendered.
 * @param onOpen expands the full player.
 * @param modifier sizing modifier — width should be `Modifier.fillMaxWidth()`.
 * @param onPlayPause overrides the play/pause action (defaults to the controller).
 * @param onNext overrides the skip-to-next action (defaults to the controller).
 * @param onLike overrides the like action (defaults to the controller).
 * @param swipeUpToOpen enables the swipe-up gesture that expands the full player.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(
    playerState: PlayerState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayPause: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onLike: (() -> Unit)? = null,
    swipeUpToOpen: Boolean = true,
) {
    val current = playerState.current ?: return
    val controller = rememberPlayerController()
    val scope = rememberCoroutineScope()
    val lift = remember { Animatable(0f) }
    val currentOnOpen by rememberUpdatedState(onOpen)
    val progress =
        if (playerState.durationMs > 0L) {
            (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val dragModifier = if (swipeUpToOpen) {
        Modifier
            .graphicsLayer {
                translationY = lift.value
                alpha = 1f - (abs(lift.value) / 260f).coerceIn(0f, 0.55f)
            }
            .pointerInput(swipeUpToOpen) {
                val settleSpec = spring<Float>(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
                detectVerticalDragGestures(
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        scope.launch {
                            lift.snapTo((lift.value + amount).coerceAtMost(0f))
                        }
                    },
                    onDragEnd = {
                        if (lift.value <= -88f) currentOnOpen()
                        scope.launch { lift.animateTo(0f, settleSpec) }
                    },
                    onDragCancel = {
                        scope.launch { lift.animateTo(0f, settleSpec) }
                    },
                )
            }
    } else {
        Modifier
    }

    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp)
            .then(dragModifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onOpen)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
                val artistLine = current.artists.joinToString(", ") { it.name }
                if (artistLine.isNotBlank()) {
                    Text(
                        text = artistLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(),
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            ControlCircle(
                icon = if (current.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (current.liked) "Remove from favorites" else "Add to favorites",
                onClick = { onLike?.invoke() ?: controller.toggleLike() },
                active = current.liked,
                glyphTint = if (current.liked) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )

            Spacer(Modifier.width(2.dp))

            ControlCircle(
                icon = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                onClick = { onPlayPause?.invoke() ?: controller.togglePlayPause() },
                size = 48.dp,
            )

            Spacer(Modifier.width(2.dp))

            ControlCircle(
                icon = Icons.Rounded.SkipNext,
                contentDescription = "Next track",
                onClick = { onNext?.invoke() ?: controller.seekToNext() },
            )

            Spacer(Modifier.width(6.dp))

            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center,
            ) {
                ProgressRing(
                    thumbnailUrl = current.thumbnailUrl,
                    progress = progress,
                    isPlaying = playerState.isPlaying,
                    modifier = Modifier.fillMaxSize(),
                    ringColor = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
