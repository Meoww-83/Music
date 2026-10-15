package com.abhiram.music.ui.component.items

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhiram.music.database.entities.Album

/**
 * Locally stored (Room) album in list or grid form.
 *
 * Round-square artwork (morphing toward a circle while pressed), album title (two-line marquee
 * in grid mode) and a subtitle of "song count • year". The song count prefers the denormalised
 * [Album.songCountText] and falls back to the loaded relation size.
 *
 * @param album local album with its song relation.
 * @param onClick invoked on tap (opens the album).
 * @param modifier root modifier for the cell/row.
 * @param onLongClick invoked on long press, or `null`.
 * @param gridItem renders the square grid cell variant instead of the list row.
 */
@Composable
fun LocalAlbumItem(
    album: Album,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    gridItem: Boolean = true,
) {
    val subtitleText = joinSubtitles(
        album.songCountText ?: songCountLabel(album.songs.size),
        album.year?.toString(),
    )
    val subtitleStyle =
        if (gridItem) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall
    val subtitleSlot = plainSubtitleContent(subtitleText, subtitleStyle)

    if (gridItem) {
        GridCell(
            title = album.title,
            onClick = onClick,
            modifier = modifier,
            thumbnail = { Artwork(album.thumbnailUrl, Modifier.fillMaxSize()) },
            subtitleContent = subtitleSlot,
            onLongClick = onLongClick,
        )
    } else {
        ListItemRow(
            title = album.title,
            onClick = onClick,
            modifier = modifier,
            thumbnail = { Artwork(album.thumbnailUrl, Modifier.fillMaxSize()) },
            subtitleContent = subtitleSlot,
            onLongClick = onLongClick,
        )
    }
}
