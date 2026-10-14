package com.abhiram.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.playback.PlayerState
import com.abhiram.music.ui.navigation.Routes

/** Panels the scaffold can present as modal bottom sheets when no [onNavigate] is wired. */
private enum class PlayerPanel { LYRICS, QUEUE, SLEEP_TIMER, STATS }

/**
 * Container that wires the collapsed [MiniPlayer] to the expandable full [Player].
 *
 * Structure (top → bottom): `content` (the app screens, given weight), then the pill mini player —
 * so it naturally sits **above the bottom navigation** when the host keeps the nav bar outside.
 * Expanding (mini-player tap or swipe-up) reveals the full player as an animated overlay with a
 * **spring expansion + shape morph** (top corners animate pill → square as it settles); collapsing
 * reverses it and reports through [onClose].
 *
 * Panels: the player's bottom row prefers [onNavigate] (fixed route keys of §8) when provided;
 * otherwise the scaffold presents **Lyrics / Queue / Sleep timer / Stats** as modal bottom sheets
 * itself, so the component works even before navigation is wired.
 *
 * Playback state is never owned here — everything is read from [playerState]; control callbacks
 * are optional and fall back to the frozen [com.abhiram.music.playback.PlayerController].
 *
 * @param playerState snapshot of the player.
 * @param onOpen notified whenever the full player expands (e.g. hide the bottom bar).
 * @param onClose notified whenever the full player collapses.
 * @param modifier sizing modifier — `Modifier.fillMaxWidth()` inside a vertical container.
 * @param onPlayPause overrides play/pause.
 * @param onNext overrides skip-to-next.
 * @param onPrevious overrides skip-to-previous.
 * @param onLike overrides like.
 * @param onShuffle overrides shuffle.
 * @param onRepeat overrides the repeat cycle.
 * @param onSeek overrides seeking.
 * @param onArtistClick invoked with the tapped artist of the player.
 * @param onOverflowClick opens the overflow menu of the player.
 * @param onNavigate route callback; `player/lyrics`, `player/queue`, `player/sleepTimer`,
 * `player/stats` — when `null` the scaffold opens its own sheets.
 * @param content the app content that lives above the mini player.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScaffold(
    playerState: PlayerState,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayPause: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    onLike: (() -> Unit)? = null,
    onShuffle: (() -> Unit)? = null,
    onRepeat: (() -> Unit)? = null,
    onSeek: ((Long) -> Unit)? = null,
    onArtistClick: ((MediaMetadata.Artist) -> Unit)? = null,
    onOverflowClick: (() -> Unit)? = null,
    onNavigate: ((String) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val controller = rememberPlayerController()
    val reduceMotion = rememberReduceMotion()
    var expanded by rememberSaveable { mutableStateOf(false) }
    var panel by remember { mutableStateOf<PlayerPanel?>(null) }

    // A dismissed queue hides the player so a fresh session starts collapsed.
    LaunchedEffect(playerState.current?.id) {
        if (playerState.current == null) expanded = false
    }

    // Entry points: host navigation first, otherwise the scaffold's own sheet.
    val lyricsEntry: () -> Unit = {
        val nav = onNavigate
        if (nav != null) nav(Routes.PLAYER_LYRICS) else panel = PlayerPanel.LYRICS
    }
    val queueEntry: () -> Unit = {
        val nav = onNavigate
        if (nav != null) nav(Routes.PLAYER_QUEUE) else panel = PlayerPanel.QUEUE
    }
    val sleepEntry: () -> Unit = {
        val nav = onNavigate
        if (nav != null) nav(Routes.PLAYER_SLEEP_TIMER) else panel = PlayerPanel.SLEEP_TIMER
    }
    val statsEntry: () -> Unit = {
        val nav = onNavigate
        if (nav != null) nav(Routes.PLAYER_STATS) else panel = PlayerPanel.STATS
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxSize()) {
            // App content sits above the mini player (which in turn sits above the bottom nav).
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                content()
            }

            AnimatedVisibility(
                visible = playerState.current != null,
                enter = if (reduceMotion) {
                    fadeIn()
                } else {
                    expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ) + fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                },
                exit = if (reduceMotion) {
                    fadeOut()
                } else {
                    shrinkVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ) + fadeOut()
                },
            ) {
                MiniPlayer(
                    playerState = playerState,
                    onOpen = {
                        expanded = true
                        onOpen()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Shape morph: pill-like rounded top corners while it travels, square once expanded.
        val corner by animateDpAsState(
            targetValue = if (expanded) 0.dp else 28.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "playerCorner",
        )

        AnimatedVisibility(
            visible = expanded && playerState.current != null,
            modifier = Modifier.matchParentSize(),
            enter = if (reduceMotion) {
                fadeIn()
            } else {
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) { it } + fadeIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            },
            exit = if (reduceMotion) {
                fadeOut()
            } else {
                slideOutVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) { it } + fadeOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = corner, topEnd = corner)),
            ) {
                Player(
                    playerState = playerState,
                    modifier = Modifier.fillMaxSize(),
                    onClose = {
                        expanded = false
                        onClose()
                    },
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onLike = onLike,
                    onShuffle = onShuffle,
                    onRepeat = onRepeat,
                    onSeek = onSeek,
                    onArtistClick = onArtistClick,
                    onOverflowClick = onOverflowClick,
                    onLyricsClick = lyricsEntry,
                    onQueueClick = queueEntry,
                    onSleepTimerClick = sleepEntry,
                    onStatsClick = statsEntry,
                    onNavigate = onNavigate,
                )
            }
        }
    }

    /* --- modal bottom sheet panels (fallback when the host has no routes wired) ---------- */
    val activePanel = panel
    if (activePanel != null) {
        ModalBottomSheet(
            onDismissRequest = { panel = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            val sheetModifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 660.dp)
            when (activePanel) {
                PlayerPanel.LYRICS -> Lyrics(
                    playerState = playerState,
                    modifier = sheetModifier,
                    onSeek = { target ->
                        onSeek?.invoke(target) ?: controller.seekTo(target)
                    },
                )

                PlayerPanel.QUEUE -> Queue(
                    playerState = playerState,
                    modifier = sheetModifier,
                )

                PlayerPanel.SLEEP_TIMER -> SleepTimer(
                    playerState = playerState,
                    modifier = sheetModifier,
                    onDismiss = { panel = null },
                )

                PlayerPanel.STATS -> StatsForNerds(
                    playerState = playerState,
                    modifier = sheetModifier,
                    onDismiss = { panel = null },
                )
            }
        }
    }
}
