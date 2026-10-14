package com.abhiram.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhiram.music.playback.PlayerState
import com.abhiram.music.playback.RepeatMode
import com.abhiram.music.ui.glass.LiquidGlassSurface

/**
 * The pill-shaped control holder of the full player (design spec: “player pill control holder”).
 *
 * Primary controls — **play/pause, previous, next and like** — are white glyphs inside **dark
 * circles** so they stay readable on top of the album-art gradient; the secondary controls
 * (shuffle / repeat) morph into the same pill group when [showSecondary] is enabled. Every target
 * is at least 48 dp and uses the shared bouncy-spring press feedback ([Modifier.springPress]).
 *
 * The holder itself is a [LiquidGlassSurface] (pill shape by default) — a floating layer, which is
 * the only place liquid glass is allowed.
 *
 * @param state player snapshot; controls are disabled while `state.current` is `null`.
 * @param onPlayPause play/pause action.
 * @param onNext skip to next track.
 * @param onPrevious skip to previous track.
 * @param onLike toggles the like state of the current track.
 * @param onShuffle toggles shuffle.
 * @param onRepeat cycles the repeat mode (`OFF → ALL → ONE`), resolved by the caller.
 * @param modifier sizing modifier (`Modifier.fillMaxWidth()` is typical).
 * @param showSecondary whether shuffle and repeat are part of the pill.
 */
@Composable
fun Controls(
    state: PlayerState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    showSecondary: Boolean = true,
) {
    val current = state.current
    val enabled = current != null

    LiquidGlassSurface(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            if (showSecondary) {
                ControlCircle(
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    onClick = onShuffle,
                    active = state.shuffleEnabled,
                    enabled = enabled,
                    glyphTint = if (state.shuffleEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Spacer(Modifier.width(2.dp))
            }

            ControlCircle(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous track",
                onClick = onPrevious,
                size = 52.dp,
                iconSize = 26.dp,
                enabled = enabled,
            )
            Spacer(Modifier.width(2.dp))

            ControlCircle(
                icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                onClick = onPlayPause,
                size = 64.dp,
                iconSize = 30.dp,
                enabled = enabled,
            )
            Spacer(Modifier.width(2.dp))

            ControlCircle(
                icon = Icons.Rounded.SkipNext,
                contentDescription = "Next track",
                onClick = onNext,
                size = 52.dp,
                iconSize = 26.dp,
                enabled = enabled,
            )
            Spacer(Modifier.width(2.dp))

            ControlCircle(
                icon = if (current?.liked == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (current?.liked == true) "Remove from favorites" else "Add to favorites",
                onClick = onLike,
                active = current?.liked == true,
                enabled = enabled,
                glyphTint = if (current?.liked == true) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )

            if (showSecondary) {
                Spacer(Modifier.width(2.dp))
                val repeatActive = state.repeatMode != RepeatMode.OFF
                ControlCircle(
                    icon = if (state.repeatMode == RepeatMode.ONE) {
                        Icons.Rounded.RepeatOne
                    } else {
                        Icons.Rounded.Repeat
                    },
                    contentDescription = when (state.repeatMode) {
                        RepeatMode.OFF -> "Repeat off"
                        RepeatMode.ALL -> "Repeat all"
                        RepeatMode.ONE -> "Repeat one"
                    },
                    onClick = onRepeat,
                    active = repeatActive,
                    enabled = enabled,
                    glyphTint = if (repeatActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

/**
 * A single control target: **dark circle + white glyph**, ≥48 dp, bouncy spring press.
 *
 * Internal building block shared by [Controls] and [MiniPlayer].
 *
 * @param icon glyph drawn in the centre.
 * @param contentDescription accessibility label.
 * @param onClick click handler.
 * @param modifier sizing/positioning modifier.
 * @param size diameter of the circle (must be ≥ 48 dp for touch targets).
 * @param iconSize diameter of the glyph.
 * @param active draws the accent ring (in [glyphTint]) around the circle.
 * @param enabled whether the control accepts input.
 * @param glyphTint colour of the glyph — also the colour of the active ring.
 */
@Composable
internal fun ControlCircle(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    active: Boolean = false,
    enabled: Boolean = true,
    glyphTint: Color = Color.White,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val reduceMotion = rememberReduceMotion()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = if (active) 0.92f else 0.80f))
            .then(
                if (active) {
                    Modifier.border(width = 2.dp, color = glyphTint, shape = CircleShape)
                } else {
                    Modifier
                },
            )
            .springPress(interactionSource = interactionSource, reduceMotion = reduceMotion)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) glyphTint else glyphTint.copy(alpha = 0.38f),
            modifier = Modifier.size(iconSize),
        )
    }
}
