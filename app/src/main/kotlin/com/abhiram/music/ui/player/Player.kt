package com.abhiram.music.ui.player

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueueMusic
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.playback.PlayerState
import com.abhiram.music.playback.RepeatMode
import com.abhiram.music.ui.component.SeekBar
import com.abhiram.music.ui.component.TooltipIconButton
import com.abhiram.music.ui.navigation.Routes
import com.abhiram.music.utils.formatDuration
import kotlinx.coroutines.launch

/**
 * Full player screen content (design language §9).
 *
 * Layout, top to bottom: a floating top row (collapse + “Now playing” + overflow), the square
 * slightly-rounded album art, the album/song title with the tappable artist line, the pill
 * [Controls] holder, the squiggly [SeekBar] and the bottom row of entry points
 * (lyrics / queue / sleep timer / stats / overflow).
 *
 * The **top 50 % of the background is the dynamic colour gradient extracted from the album art**
 * (via `rememberArtworkState` + [artworkPlayerBrush]) fading into **pure black** for the bottom.
 *
 * Gestures on the artwork: **vertical drag-down dismisses** (past 28 % of its height) and
 * **horizontal drag skips** to the next/previous track; every settle uses springs, layout changes
 * animate with [Modifier.animateContentSize], and reduced-motion disables the decorative motion.
 *
 * Playback state is never owned here: everything is read from [playerState] and every action goes
 * through the frozen [com.abhiram.music.playback.PlayerController] unless an override callback is
 * supplied.
 *
 * @param playerState snapshot of the player (queue, position, error…). When `null` (the default,
 * so the component can be wired into `NavHost` with no arguments) the live state of the frozen
 * controller is used.
 * @param modifier sizing modifier — normally `Modifier.fillMaxSize()`.
 * @param onClose collapses/dismisses the player; when `null`, drag-to-dismiss springs back instead.
 * @param onPlayPause overrides play/pause (defaults to the controller).
 * @param onNext overrides skip-to-next (defaults to the controller).
 * @param onPrevious overrides skip-to-previous (defaults to the controller).
 * @param onLike overrides like (defaults to the controller).
 * @param onShuffle overrides shuffle (defaults to the controller).
 * @param onRepeat overrides the repeat cycle; when `null` the controller cycles `OFF→ALL→ONE`.
 * @param onSeek overrides seeking (defaults to the controller).
 * @param onArtistClick invoked with the artist of the tappable artist line.
 * @param onLyricsClick opens the lyrics panel (defaults to [onNavigate] with [Routes.PLAYER_LYRICS]).
 * @param onQueueClick opens the queue panel (defaults to [onNavigate] with [Routes.PLAYER_QUEUE]).
 * @param onSleepTimerClick opens the sleep-timer sheet (defaults to [onNavigate] with [Routes.PLAYER_SLEEP_TIMER]).
 * @param onStatsClick opens the diagnostics (defaults to [onNavigate] with [Routes.PLAYER_STATS]).
 * @param onOverflowClick opens the overflow menu; the overflow button is hidden when this is `null`.
 * @param onNavigate generic navigation callback receiving the fixed route keys of §8.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Player(
    playerState: PlayerState? = null,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    onPlayPause: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    onLike: (() -> Unit)? = null,
    onShuffle: (() -> Unit)? = null,
    onRepeat: (() -> Unit)? = null,
    onSeek: ((Long) -> Unit)? = null,
    onArtistClick: ((MediaMetadata.Artist) -> Unit)? = null,
    onLyricsClick: (() -> Unit)? = null,
    onQueueClick: (() -> Unit)? = null,
    onSleepTimerClick: (() -> Unit)? = null,
    onStatsClick: (() -> Unit)? = null,
    onOverflowClick: (() -> Unit)? = null,
    onNavigate: ((String) -> Unit)? = null,
) {
    val state = playerState ?: rememberPlayerState()
    val current = state.current
    val controller = rememberPlayerController()
    val reduceMotion = rememberReduceMotion()
    val scope = rememberCoroutineScope()
    val artwork = rememberArtworkState(current?.thumbnailUrl)
    val palette = (artwork.value as? ArtworkState.Ready)?.palette
    val backgroundBrush = remember(palette) { artworkPlayerBrush(palette) }

    // Gesture state lives in draw phase only — no recomposition while dragging.
    val dragY = remember { Animatable(0f) }
    val dragX = remember { Animatable(0f) }
    val currentOnClose by rememberUpdatedState(onClose)
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)

    fun settleDrag() {
        val spec = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )
        scope.launch { dragY.animateTo(0f, spec) }
        scope.launch { dragX.animateTo(0f, spec) }
    }

    val gestureModifier = Modifier.pointerInput(state.current?.id, controller) {
        detectDragGestures(
            onDragStart = { },
            onDrag = { change, amount ->
                change.consume()
                scope.launch {
                    dragX.snapTo(dragX.value + amount.x)
                    dragY.snapTo(dragY.value + amount.y)
                }
            },
            onDragEnd = {
                val dy = dragY.value
                val dx = dragX.value
                val dismissAt = size.height * DragDismissHeightFraction
                when {
                    dy > dismissAt || dy > 340f -> {
                        val close = currentOnClose
                        if (close != null) {
                            // Let the host's exit animation take over from the dragged offset.
                            close()
                        } else {
                            settleDrag()
                        }
                    }
                    dx <= -DragSkipThresholdPx -> {
                        currentOnNext?.invoke() ?: controller.seekToNext()
                        settleDrag()
                    }
                    dx >= DragSkipThresholdPx -> {
                        currentOnPrevious?.invoke() ?: controller.seekToPrevious()
                        settleDrag()
                    }
                    else -> settleDrag()
                }
            },
            onDragCancel = { settleDrag() },
        )
    }

    val lyricsAction = entryOrDefault(onLyricsClick, Routes.PLAYER_LYRICS, onNavigate)
    val queueAction = entryOrDefault(onQueueClick, Routes.PLAYER_QUEUE, onNavigate)
    val sleepAction = entryOrDefault(onSleepTimerClick, Routes.PLAYER_SLEEP_TIMER, onNavigate)
    val statsAction = entryOrDefault(onStatsClick, Routes.PLAYER_STATS, onNavigate)
    val overflowAction = onOverflowClick

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                val dismiss =
                    (dragY.value / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                alpha = 1f - dismiss * 0.45f
                val scale = 1f - dismiss * 0.05f
                scaleX = scale
                scaleY = scale
            },
    ) {
        // Top 50 %: dynamic colour from the album art → pure black for the bottom half.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(backgroundBrush),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .animateContentSize(
                    animationSpec = if (reduceMotion) {
                        snap()
                    } else {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        )
                    },
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            /* --- top bar ------------------------------------------------------------ */
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (onClose != null) {
                    TooltipIconButton(
                        contentDescription = "Collapse player",
                        onClick = { onClose() },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = "Now playing",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                    )
                    val albumTitle = current?.album?.title
                    if (!albumTitle.isNullOrBlank()) {
                        Text(
                            text = albumTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.52f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.basicMarquee(),
                        )
                    }
                }

                if (overflowAction != null) {
                    TooltipIconButton(
                        contentDescription = "More options",
                        onClick = { overflowAction() },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            /* --- artwork (or playback error) ---------------------------------------- */
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(gestureModifier),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationX = dragX.value
                            translationY = dragY.value
                            if (!reduceMotion) {
                                rotationZ = (dragX.value / 48f).coerceIn(-7f, 7f)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val error = state.error
                    when {
                        error != null -> PlaybackError(
                            error = error,
                            videoId = current?.id,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                        )

                        current != null -> Thumbnail(
                            thumbnailUrl = current.thumbnailUrl,
                            contentDescription = current.title,
                            modifier = Modifier.fillMaxSize(),
                            interactive = true,
                            ringColor = palette?.primary ?: MaterialTheme.colorScheme.primary,
                            onTap = { /* press-to-release feedback only */ },
                        )

                        else -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.size(72.dp),
                            )
                        }
                    }
                }
            }

            /* --- title + artists ----------------------------------------------------- */
            Spacer(Modifier.height(12.dp))
            Text(
                text = current?.title ?: "Nothing playing",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(),
            )
            if (current != null) {
                val artists = current.artists
                val artistLine = artists.joinToString(" • ") { it.name }
                val handleArtistClick: (() -> Unit)? =
                    if (onArtistClick != null && artists.isNotEmpty()) {
                        { onArtistClick(artists.firstOrNull { it.id != null } ?: artists.first()) }
                    } else {
                        null
                    }
                Text(
                    text = artistLine,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = if (handleArtistClick != null) 0.85f else 0.65f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .let {
                            if (handleArtistClick != null) {
                                it.clickable(onClick = handleArtistClick)
                            } else {
                                it
                            }
                        },
                )
            } else {
                Text(
                    text = "Pick something to play",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            /* --- controls ------------------------------------------------------------ */
            Spacer(Modifier.height(14.dp))
            Controls(
                state = state,
                onPlayPause = { onPlayPause?.invoke() ?: controller.togglePlayPause() },
                onNext = { onNext?.invoke() ?: controller.seekToNext() },
                onPrevious = { onPrevious?.invoke() ?: controller.seekToPrevious() },
                onLike = { onLike?.invoke() ?: controller.toggleLike() },
                onShuffle = { onShuffle?.invoke() ?: controller.toggleShuffle() },
                onRepeat = {
                    onRepeat?.invoke()
                        ?: controller.setRepeatMode(cycleRepeatMode(state.repeatMode))
                },
                modifier = Modifier.fillMaxWidth(),
                showSecondary = true,
            )

            /* --- position ------------------------------------------------------------ */
            Spacer(Modifier.height(10.dp))
            SeekBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                onSeek = { target -> onSeek?.invoke(target) ?: controller.seekTo(target) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.durationMs > 0L,
                squiggly = true,
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = state.positionMs.formatDuration(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.70f),
                )
                Text(
                    text = "-" + (state.durationMs - state.positionMs)
                        .coerceAtLeast(0L)
                        .formatDuration(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.70f),
                )
            }

            /* --- bottom entry row ---------------------------------------------------- */
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                PlayerEntry(
                    icon = Icons.Rounded.Lyrics,
                    label = "Lyrics",
                    onClick = lyricsAction,
                )
                PlayerEntry(
                    icon = Icons.Rounded.QueueMusic,
                    label = "Queue",
                    onClick = queueAction,
                )
                PlayerEntry(
                    icon = Icons.Rounded.Bedtime,
                    label = "Sleep timer",
                    onClick = sleepAction,
                    active = state.sleepTimerProgress != null,
                )
                PlayerEntry(
                    icon = Icons.Rounded.BarChart,
                    label = "Stats for nerds",
                    onClick = statsAction,
                )
                if (overflowAction != null) {
                    PlayerEntry(
                        icon = Icons.Rounded.MoreVert,
                        label = "More options",
                        onClick = { overflowAction() },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

/** Cycles `OFF → ALL → ONE → OFF` for the repeat control. */
internal fun cycleRepeatMode(mode: RepeatMode): RepeatMode = when (mode) {
    RepeatMode.OFF -> RepeatMode.ALL
    RepeatMode.ALL -> RepeatMode.ONE
    RepeatMode.ONE -> RepeatMode.OFF
}

/**
 * Resolves a bottom-row entry point: the explicit callback wins, then generic [navigate] with the
 * fixed route key, then a no-op so the button is inert but harmless.
 */
private fun entryOrDefault(
    entry: (() -> Unit)?,
    route: String,
    navigate: ((String) -> Unit)?,
): () -> Unit {
    if (entry != null) return entry
    val nav = navigate
    if (nav != null) return { nav(route) }
    return {}
}

/** One ≥48 dp entry point of the player's bottom row, with an expressive press bounce. */
@Composable
private fun PlayerEntry(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val reduceMotion = rememberReduceMotion()
    TooltipIconButton(
        contentDescription = label,
        onClick = onClick,
        modifier = modifier.springPress(
            interactionSource = interactionSource,
            reduceMotion = reduceMotion,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) Color.White else Color.White.copy(alpha = 0.70f),
            modifier = Modifier.size(24.dp),
        )
    }
}
