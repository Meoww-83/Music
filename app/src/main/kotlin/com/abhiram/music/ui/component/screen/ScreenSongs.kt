package com.abhiram.music.ui.component.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.component.MusicBars
import com.abhiram.music.ui.component.SwipeToActionBox
import com.abhiram.music.ui.component.TooltipIconButton
import com.abhiram.music.ui.component.items.ListItemContainer
import com.abhiram.music.ui.component.items.MediaSongItem
import com.abhiram.music.utils.formatDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * The song list building blocks shared by every album/artist/playlist screen component
 * (owner CMP-D): one row renderer on top of CMP-C's `MediaSongItem`/`ListItemContainer` plus a
 * list builder that every `*Songs` composable and screen delegates to.
 */

/**
 * Drag-to-reorder bookkeeping for the local playlist (and album) song lists.
 *
 * The rows report their measured height through [setRowHeight]; the handle drives [dragBy] which
 * calls [onMoveHandler] as soon as a row crosses its neighbour and keeps the dragged row under
 * the finger; [endDrag] springs the visual offset back to zero.
 */
internal class ScreenReorderState(
    private val fallbackRowHeightPx: Float,
    private val onMoveHandler: (from: Int, to: Int) -> Unit,
) {
    /** Total number of rows in the list; kept up to date by the list builder. */
    var itemCount: Int = 0

    /** Index of the row currently under the user's finger, `null` when idle. */
    var draggedIndex by mutableStateOf<Int?>(null)
        private set

    private var dragOffsetY by mutableFloatStateOf(0f)
    private val heights = mutableStateMapOf<Int, Int>()
    private var settleJob: Job? = null

    /** Records the measured height of a row (falls back to 72 dp while unknown). */
    fun setRowHeight(index: Int, heightPx: Int) {
        heights[index] = heightPx
    }

    /** Vertical translation applied to the dragged row. */
    fun rowOffset(index: Int): Float = if (index == draggedIndex) dragOffsetY else 0f

    /** True while [index] is dragged. */
    fun isDragged(index: Int): Boolean = index == draggedIndex

    /** Grabs the row; cancels any running settle animation. */
    fun beginDrag(index: Int) {
        settleJob?.cancel()
        draggedIndex = index
        dragOffsetY = 0f
    }

    /** Applies a vertical drag delta, firing [onMoveHandler] on row crossings. */
    fun dragBy(deltaY: Float) {
        val from = draggedIndex ?: return
        val count = itemCount
        if (count <= 0) return
        dragOffsetY += deltaY
        val height = heights[from] ?: fallbackRowHeightPx
        if (height <= 0f) return
        val target = (from + (dragOffsetY / height).roundToInt()).coerceIn(0, count - 1)
        if (target != from) {
            onMoveHandler(from, target)
            dragOffsetY -= (target - from) * height
            draggedIndex = target
        }
    }

    /** Releases the row: snaps the visual offset back with an expressive spring. */
    fun endDrag(scope: CoroutineScope) {
        val start = dragOffsetY
        if (abs(start) < 0.5f) {
            draggedIndex = null
            dragOffsetY = 0f
            return
        }
        settleJob?.cancel()
        settleJob = scope.launch {
            val anim = Animatable(start)
            anim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) { value -> dragOffsetY = value }
            draggedIndex = null
            dragOffsetY = 0f
        }
    }
}

/** Applies the drag visuals (lift, translation, measure reporting) to one reorderable row. */
internal fun Modifier.reorderRow(
    state: ScreenReorderState?,
    index: Int,
): Modifier {
    if (state == null) return this
    val dragged = state.isDragged(index)
    return this
        .onSizeChanged { state.setRowHeight(index, it.height) }
        .zIndex(if (dragged) 1f else 0f)
        .graphicsLayer {
            translationY = state.rowOffset(index)
            if (dragged) {
                scaleX = 1.02f
                scaleY = 1.02f
                shadowElevation = 12.dp.toPx()
            }
        }
}

/**
 * 48 dp drag handle placed in a row's leading slot; vertical drags drive the reorder logic of
 * [ScreenReorderState]. Callbacks are kept in `rememberUpdatedState` so a mid-drag reorder never
 * restarts the gesture (the row that moves under the finger reports its own index).
 */
@Composable
internal fun ReorderHandle(
    onDragStart: () -> Unit,
    onDrag: (deltaY: Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val currentStart by rememberUpdatedState(onDragStart)
    val currentDrag by rememberUpdatedState(onDrag)
    val currentEnd by rememberUpdatedState(onDragEnd)

    Box(
        modifier = modifier
            .size(48.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentStart()
                    },
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

/* -------------------------------------------------------------------------------------------
 * Song row
 * ---------------------------------------------------------------------------------------- */

/**
 * One song row: number (or music bars while it is playing) up front, duration + menu at the end,
 * built on CMP-C's [MediaSongItem]. When [subtitleOverride] is set the row switches to
 * [ListItemContainer] so online lists can show an author/date line instead of the artists.
 */
@Composable
internal fun SongRow(
    song: MediaMetadata,
    index: Int,
    playback: ScreenPlayback,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIndex: Boolean = true,
    showDuration: Boolean = true,
    subtitleOverride: String? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    reorder: ScreenReorderState? = null,
) {
    val current = playback.songId == song.id
    val reorderScope = rememberCoroutineScope()
    val leadingSlot: @Composable RowScope.() -> Unit = leading ?: {
        if (reorder != null) {
            ReorderHandle(
                onDragStart = { reorder.beginDrag(index) },
                onDrag = { delta -> reorder.dragBy(delta) },
                onDragEnd = { reorder.endDrag(reorderScope) },
            )
        } else if (current) {
            MusicBars(
                isPlaying = playback.isPlaying,
                modifier = Modifier.width(24.dp),
            )
        } else if (showIndex) {
            Text(
                text = (index + 1).toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
    val trailingSlot: @Composable RowScope.() -> Unit = {
        if (showDuration && song.duration > 0) {
            Text(
                text = song.duration.formatDuration(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onMenu != null) {
            TooltipIconButton(
                contentDescription = "More options",
                onClick = onMenu,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    if (subtitleOverride == null) {
        MediaSongItem(
            item = song,
            modifier = modifier,
            onClick = onClick,
            onLongClick = onLongClick,
            leadingContent = leadingSlot,
            trailingContent = trailingSlot,
            active = current,
            playing = current && playback.isPlaying,
        )
    } else {
        ListItemContainer(
            title = song.title,
            onClick = onClick,
            modifier = modifier,
            thumbnail = {
                ArtworkImage(
                    url = song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    requestSize = 240,
                )
            },
            subtitle = subtitleOverride,
            leadingContent = leadingSlot,
            trailingContent = trailingSlot,
            selected = current,
            onLongClick = onLongClick,
        )
    }
}

/* -------------------------------------------------------------------------------------------
 * List builder
 * ---------------------------------------------------------------------------------------- */

/**
 * Emits the song rows into an enclosing `LazyColumn` with stable keys, `animateItem` placement
 * and optional drag-to-reorder / swipe-to-remove affordances. Both the public `*Songs`
 * composables and the `*Screen` components build their lists through this function so rows
 * behave identically everywhere.
 *
 * @param songs rows to render (empty lists render nothing — callers show their own empty state).
 * @param playback current player snapshot used for the "now playing" indicators.
 * @param keyPrefix stable identity prefix so rows animate as placements across refreshes.
 * @param subtitleFor optional per-row subtitle; switches the row to [ListItemContainer].
 * @param onRemove when provided, rows are wrapped in [SwipeToActionBox] (swipe to remove).
 * @param reorder when provided, every row gets a drag handle driving [ScreenReorderState].
 */
internal fun LazyListScope.screenSongItems(
    songs: List<MediaMetadata>,
    playback: ScreenPlayback,
    keyPrefix: String,
    onSongClick: (Int) -> Unit,
    showIndex: Boolean = true,
    showDuration: Boolean = true,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
    onSongMenu: ((MediaMetadata) -> Unit)? = null,
    subtitleFor: ((index: Int, song: MediaMetadata) -> String)? = null,
    onRemove: ((index: Int) -> Unit)? = null,
    reorder: ScreenReorderState? = null,
) {
    if (reorder != null) reorder.itemCount = songs.size
    itemsIndexed(
        items = songs,
        key = { index, song -> "$keyPrefix:${song.id}#$index" },
    ) { index, song ->
        val baseModifier = Modifier
            .fillMaxWidth()
            .reorderRow(reorder, index)
        if (onRemove != null) {
            SwipeToActionBox(
                onAction = { onRemove(index) },
                modifier = Modifier.animateItem(),
                actionIcon = Icons.Rounded.Close,
                actionColor = MaterialTheme.colorScheme.error,
            ) {
                SongRow(
                    song = song,
                    index = index,
                    playback = playback,
                    onClick = { onSongClick(index) },
                    modifier = baseModifier,
                    showIndex = showIndex,
                    showDuration = showDuration,
                    subtitleOverride = subtitleFor?.invoke(index, song),
                    onLongClick = onSongLongClick?.let { handler -> { handler(song) } },
                    onMenu = onSongMenu?.let { handler -> { handler(song) } },
                    reorder = reorder,
                )
            }
        } else {
            SongRow(
                song = song,
                index = index,
                playback = playback,
                onClick = { onSongClick(index) },
                modifier = baseModifier.animateItem(),
                showIndex = showIndex,
                showDuration = showDuration,
                subtitleOverride = subtitleFor?.invoke(index, song),
                onLongClick = onSongLongClick?.let { handler -> { handler(song) } },
                onMenu = onSongMenu?.let { handler -> { handler(song) } },
                reorder = reorder,
            )
        }
    }
}
