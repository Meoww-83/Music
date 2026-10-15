package com.abhiram.music.ui.component.items

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhiram.music.database.entities.Playlist

/**
 * Locally created (Room) user playlist in list or grid form.
 *
 * Round-square artwork (morphing toward a circle while pressed), playlist name (two-line
 * marquee in grid mode) and a subtitle showing the song count, falling back to the playlist
 * description when the count is unknown.
 *
 * @param playlist local playlist with its song relation.
 * @param onClick invoked on tap (opens the playlist).
 * @param modifier root modifier for the cell/row.
 * @param onLongClick invoked on long press, or `null`.
 * @param gridItem renders the square grid cell variant instead of the list row.
 */
@Composable
fun LocalPlaylistItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    gridItem: Boolean = true,
) {
    val subtitleText = songCountLabel(playlist.songs.size)
        ?: playlist.description?.trim()?.takeIf { it.isNotBlank() }
    val subtitleStyle =
        if (gridItem) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall
    val subtitleSlot = plainSubtitleContent(subtitleText, subtitleStyle)

    if (gridItem) {
        GridCell(
            title = playlist.name,
            onClick = onClick,
            modifier = modifier,
            thumbnail = { Artwork(playlist.thumbnailUrl, Modifier.fillMaxSize()) },
            subtitleContent = subtitleSlot,
            onLongClick = onLongClick,
        )
    } else {
        ListItemRow(
            title = playlist.name,
            onClick = onClick,
            modifier = modifier,
            thumbnail = { Artwork(playlist.thumbnailUrl, Modifier.fillMaxSize()) },
            subtitleContent = subtitleSlot,
            onLongClick = onLongClick,
        )
    }
}
