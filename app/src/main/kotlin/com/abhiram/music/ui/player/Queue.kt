package com.abhiram.music.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.abhiram.music.playback.PlayerState
import com.abhiram.music.playback.RepeatMode
import com.abhiram.music.ui.component.ConfirmationDialog
import com.abhiram.music.ui.component.SwipeToActionBox
import com.abhiram.music.ui.component.TextPlaceholder
import com.abhiram.music.ui.component.TooltipIconButton
import com.abhiram.music.ui.component.items.MediaSongItem
import com.abhiram.music.utils.formatDuration
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Playback queue list.
 *
 * Shows the current + upcoming tracks with [MediaSongItem] rows (the active row is marked and
 * animated with [Modifier.animateItem]), and offers:
 *
 * * **drag-to-reorder** — the handle in `leadingContent` starts a drag; crossings call
 *   `PlayerController.moveQueueItem` immediately so the real queue reorders under the finger, with
 *   a haptic tick on grab and a spring settle on release;
 * * **swipe-to-remove** — each row is wrapped in [SwipeToActionBox] (delete affordance, threshold
 *   96 dp) which calls `PlayerController.removeQueueItem`;
 * * **“Clear queue”** behind a [ConfirmationDialog];
 * * a header that mirrors the **repeat/shuffle state** (toggles included).
 *
 * Playback state comes exclusively from [playerState]; every mutation goes through the frozen
 * [com.abhiram.music.playback.PlayerController] unless an override callback is supplied.
 *
 * @param playerState snapshot of the player (queue, currentIndex, isPlaying, repeat, shuffle).
 * @param modifier sizing modifier (`fillMaxWidth`/`heightIn` are typical in a sheet).
 * @param onSongClick plays the tapped row (defaults to `playAll(queue, index)` on the controller).
 * @param onRemove removes a row (defaults to `removeQueueItem`).
 * @param onMove reorders two rows (defaults to `moveQueueItem`).
 * @param onClear clears the queue (defaults to `clearQueue`).
 * @param onShuffle toggles shuffle (defaults to the controller).
 * @param onRepeat cycles repeat (defaults to `OFF → ALL → ONE` on the controller).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Queue(
    playerState: PlayerState,
    modifier: Modifier = Modifier,
    onSongClick: ((index: Int) -> Unit)? = null,
    onRemove: ((index: Int) -> Unit)? = null,
    onMove: ((fromIndex: Int, toIndex: Int) -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    onShuffle: (() -> Unit)? = null,
    onRepeat: (() -> Unit)? = null,
) {
    val queue = playerState.queue
    val controller = rememberPlayerController()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Fresh views of composition values for the long-lived pointer inputs below.
    val currentQueue by rememberUpdatedState(queue)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnSongClick by rememberUpdatedState(onSongClick)
    val currentOnRemove by rememberUpdatedState(onRemove)

    /* --- drag-to-reorder state ---------------------------------------------------------- */
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val rowHeights = remember { mutableStateMapOf<Int, Int>() }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    fun beginDrag(index: Int) {
        settleJob?.cancel()
        draggedIndex = index
        dragOffsetY = 0f
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun dragBy(deltaY: Float) {
        val from = draggedIndex ?: return
        val target0 = currentQueue
        if (target0.isEmpty()) return
        dragOffsetY += deltaY
        val rowHeight = rowHeights[from]
            ?: with(density) { 72.dp.toPx() }.toInt()
        if (rowHeight <= 0) return
        val target = (from + (dragOffsetY / rowHeight).roundToInt())
            .coerceIn(target0.indices)
        if (target != from) {
            currentOnMove?.invoke(from, target) ?: controller.moveQueueItem(from, target)
            // Keep the row under the finger: its slot just moved by (target - from) rows.
            dragOffsetY -= (target - from) * rowHeight
            draggedIndex = target
        }
    }

    fun endDrag() {
        val start = dragOffsetY
        if (abs(start) < 0.5f) {
            draggedIndex = null
            dragOffsetY = 0f
            return
        }
        settleJob = scope.launch {
            val anim = Animatable(start)
            anim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) { dragOffsetY = value }
            draggedIndex = null
            dragOffsetY = 0f
        }
    }

    fun playIndex(index: Int) {
        val handler = currentOnSongClick
        if (handler != null) {
            handler(index)
        } else {
            controller.playAll(currentQueue, index, playerState.shuffleEnabled)
        }
    }

    fun removeIndex(index: Int) {
        val handler = currentOnRemove
        if (handler != null) handler(index) else controller.removeQueueItem(index)
    }

    var showClearDialog by remember { mutableStateOf(false) }

    // Stable identity per row (id + occurrence) so reorders animate as placements, while
    // duplicated ids in the queue stay unique.
    val rowKeys = remember(queue) {
        val counts = HashMap<String, Int>(queue.size)
        queue.map { item ->
            val next = (counts[item.id] ?: 0) + 1
            counts[item.id] = next
            "${item.id}#$next"
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        QueueHeader(
            state = playerState,
            onShuffle = { onShuffle?.invoke() ?: controller.toggleShuffle() },
            onRepeat = {
                onRepeat?.invoke()
                    ?: controller.setRepeatMode(cycleRepeatMode(playerState.repeatMode))
            },
            onClear = { showClearDialog = true },
        )

        if (queue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                TextPlaceholder(text = "The queue is empty")
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(
                    items = queue,
                    key = { index, _ -> rowKeys[index] },
                ) { index, item ->
                    val isActive = index == playerState.currentIndex
                    val isDragged = index == draggedIndex
                    SwipeToActionBox(
                        onAction = { removeIndex(index) },
                        modifier = Modifier
                            .onSizeChanged { rowHeights[index] = it.height }
                            .then(
                                if (isDragged) {
                                    Modifier.graphicsLayer {
                                        translationY = dragOffsetY
                                        scaleX = 1.02f
                                        scaleY = 1.02f
                                        shadowElevation = 12.dp
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .zIndex(if (isDragged) 1f else 0f)
                            .animateItem(),
                        actionIcon = Icons.Rounded.Delete,
                        actionColor = MaterialTheme.colorScheme.error,
                    ) {
                        MediaSongItem(
                            item = item,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { playIndex(index) },
                            active = isActive,
                            playing = isActive && playerState.isPlaying,
                            leadingContent = {
                                QueueDragHandle(
                                    onDragStart = { beginDrag(index) },
                                    onDrag = { delta -> dragBy(delta) },
                                    onDragEnd = { endDrag() },
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = item.duration.formatDuration(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        ConfirmationDialog(
            title = "Clear the queue?",
            text = "Upcoming tracks will be removed; the current track keeps playing.",
            onConfirm = {
                showClearDialog = false
                onClear?.invoke() ?: controller.clearQueue()
            },
            onDismiss = { showClearDialog = false },
            confirmText = "Clear",
        )
    }
}

/* -------------------------------------------------------------------------------------------
 * Header
 * ---------------------------------------------------------------------------------------- */

/** Queue title + live repeat/shuffle state + shuffle, repeat and clear affordances. */
@Composable
private fun QueueHeader(
    state: PlayerState,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Queue",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            val labels = buildList {
                add("${state.queue.size} tracks")
                if (state.shuffleEnabled) add("Shuffle on")
                when (state.repeatMode) {
                    RepeatMode.ALL -> add("Repeat all")
                    RepeatMode.ONE -> add("Repeat one")
                    RepeatMode.OFF -> Unit
                }
            }
            Text(
                text = labels.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(4.dp))

        ControlCircle(
            icon = Icons.Rounded.Shuffle,
            contentDescription = "Shuffle",
            onClick = onShuffle,
            active = state.shuffleEnabled,
            glyphTint = if (state.shuffleEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        Spacer(Modifier.width(2.dp))
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
            active = state.repeatMode != RepeatMode.OFF,
            glyphTint = if (state.repeatMode != RepeatMode.OFF) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        Spacer(Modifier.width(2.dp))
        TooltipIconButton(
            contentDescription = "Clear queue",
            onClick = onClear,
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/* -------------------------------------------------------------------------------------------
 * Drag handle
 * ---------------------------------------------------------------------------------------- */

/**
 * 48 dp drag handle placed in the row's `leadingContent`; vertical drags drive the reorder logic
 * of [Queue]. Callbacks are kept in [rememberUpdatedState] so the pointer input never restarts
 * mid-drag (which would cancel the gesture on the first reorder).
 */
@Composable
private fun QueueDragHandle(
    onDragStart: () -> Unit,
    onDrag: (deltaY: Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentStart by rememberUpdatedState(onDragStart)
    val currentDrag by rememberUpdatedState(onDrag)
    val currentEnd by rememberUpdatedState(onDragEnd)

    Box(
        modifier = modifier
            .size(48.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { currentStart() },
                    onDrag = { change, amount ->
                        change.consume()
                        currentDrag(amount.y)
                    },
                    onDragEnd = { currentEnd() },
                    onDragCancel = { currentEnd() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.DragIndicator,
            contentDescription = "Reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
    }
}
