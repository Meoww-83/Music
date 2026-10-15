package com.abhiram.music.ui.component.items

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhiram.music.database.entities.Song
import com.abhiram.music.model.toMediaMetadata

/**
 * Locally stored (Room) track in list or grid form.
 *
 * Thin delegate: maps the [Song] relation wrapper to a
 * [com.abhiram.music.model.MediaMetadata] via [toMediaMetadata] and renders it with
 * [MediaSongItem], so local tracks get exactly the same artwork, explicit badge, duration
 * handling, playing indicator and press feedback as remote ones.
 *
 * @param song local song with its ordered artist relations.
 * @param onClick invoked on tap (starts playback).
 * @param modifier root modifier for the cell/row.
 * @param onLongClick invoked on long press, or `null`.
 * @param trailingContent row-scoped slot rendered at the end of the row (overflow menu), or the
 * cell's artwork overlay when [gridItem] is true; the duration moves into the subtitle line
 * whenever this slot is supplied.
 * @param leadingContent row-scoped slot rendered before the artwork; overrides the playing bars.
 * @param gridItem renders the square grid cell variant instead of the list row.
 */
@Composable
fun LocalSongItem(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
    gridItem: Boolean = false,
) {
    MediaSongItem(
        item = song.toMediaMetadata(),
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        gridItem = gridItem,
    )
}
