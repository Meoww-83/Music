package com.abhiram.music.ui.component.items

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.component.MusicBars

/**
 * The workhorse row: a unified song cell that renders a [MediaMetadata] track as either a list
 * row (`gridItem = false`) or a grid cell (`gridItem = true`).
 *
 * Visuals: round-square coil artwork with a morphing press state, title, artist line (with an
 * explicit "E" badge when needed) and either the duration or the caller's [trailingContent]
 * slot — the duration moves into the subtitle line whenever a trailing slot is supplied, so it
 * is never shown twice. The [leadingContent] slot sits before the artwork (track index, drag
 * handle…); when it is absent and [playing] is true, animated
 * [com.abhiram.music.ui.component.MusicBars] occupy it (list rows) or a dark now-playing pill
 * overlays the artwork (grid cells). [active] fills the cell with `primaryContainer` and swaps
 * the content colour to `onPrimaryContainer`; press feedback is a spring scale plus a subtle
 * brightness dim, both skipped under reduce-motion.
 *
 * @param item track to render.
 * @param onClick invoked on tap (typically starts playback or opens the album).
 * @param modifier root modifier for the cell/row.
 * @param onLongClick invoked on long press (opens the item menu), or `null`.
 * @param leadingContent row-scoped slot rendered before the artwork; overrides the playing bars.
 * @param trailingContent row-scoped slot rendered at the end of the row (overflow menu), or in
 * the cell's bottom-end artwork overlay when [gridItem] is true.
 * @param active whether this track is the current/selected one.
 * @param playing whether this track is the audible one (shows the playing indicator).
 * @param gridItem renders the square grid cell variant instead of the list row.
 */
@Composable
fun MediaSongItem(
    item: MediaMetadata,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    active: Boolean = false,
    playing: Boolean = false,
    gridItem: Boolean = false,
) {
    val artistLine: String? =
        item.artists
            .joinToString(", ") { it.name }
            .ifBlank { null }
            ?: item.album?.title?.ifBlank { null }
    val durationText = formatDuration(item.duration)
    // The duration lives either in the trailing slot or in the subtitle — never twice.
    val subtitleText: String? =
        if (gridItem || trailingContent != null) {
            joinSubtitles(artistLine, durationText)
        } else {
            artistLine
        }
    val subtitleStyle =
        if (gridItem) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall
    val subtitleSlot = songSubtitleContent(
        explicit = item.explicit,
        text = subtitleText,
        style = subtitleStyle,
    )

    val musicBarsSlot: (@Composable RowScope.() -> Unit)? = {
        MusicBars(
            isPlaying = true,
            color = if (active) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
    val playingPillSlot: (@Composable RowScope.() -> Unit)? = { PlayingIndicatorPill() }
    val durationSlot: (@Composable RowScope.() -> Unit)? = { DurationLabel(item.duration) }

    if (gridItem) {
        GridCell(
            title = item.title,
            onClick = onClick,
            modifier = modifier,
            thumbnailRatio = 1f,
            thumbnail = { Artwork(item.thumbnailUrl, Modifier.fillMaxSize()) },
            subtitleContent = subtitleSlot,
            icon = menuIconSlot(trailingContent),
            leadingOverlay = leadingContent ?: if (playing) playingPillSlot else null,
            selected = active,
            onLongClick = onLongClick,
        )
    } else {
        ListItemRow(
            title = item.title,
            onClick = onClick,
            modifier = modifier,
            thumbnail = { Artwork(item.thumbnailUrl, Modifier.fillMaxSize()) },
            subtitleContent = subtitleSlot,
            leadingContent = leadingContent ?: if (playing) musicBarsSlot else null,
            trailingContent = trailingContent ?: durationSlot,
            selected = active,
            onLongClick = onLongClick,
        )
    }
}
