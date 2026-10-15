package com.abhiram.music.ui.component.items

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhiram.music.database.entities.Artist

/**
 * Locally stored (Room) artist in list or grid form.
 *
 * Circular artwork (the one item shape that stays round at rest and while pressed), artist name
 * (two-line marquee in grid mode) and a subtitle of "subscribers • song count". The counts come
 * from [Artist.subscriberCountText] and the loaded [Artist.songs] relation.
 *
 * @param artist local artist with its song relation.
 * @param onClick invoked on tap (opens the artist).
 * @param modifier root modifier for the cell/row.
 * @param onLongClick invoked on long press, or `null`.
 * @param gridItem renders the square grid cell variant instead of the list row.
 */
@Composable
fun LocalArtistItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    gridItem: Boolean = true,
) {
    val subtitleText = joinSubtitles(
        artist.subscriberCountText,
        songCountLabel(artist.songs.size),
    )
    val subtitleStyle =
        if (gridItem) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall
    val subtitleSlot = plainSubtitleContent(subtitleText, subtitleStyle)

    if (gridItem) {
        GridCell(
            title = artist.name,
            onClick = onClick,
            modifier = modifier,
            thumbnail = {
                Artwork(artist.thumbnailUrl, Modifier.fillMaxSize(), circle = true)
            },
            subtitleContent = subtitleSlot,
            onLongClick = onLongClick,
        )
    } else {
        ListItemRow(
            title = artist.name,
            onClick = onClick,
            modifier = modifier,
            thumbnail = {
                Artwork(artist.thumbnailUrl, Modifier.fillMaxSize(), circle = true)
            },
            subtitleContent = subtitleSlot,
            onLongClick = onLongClick,
        )
    }
}
