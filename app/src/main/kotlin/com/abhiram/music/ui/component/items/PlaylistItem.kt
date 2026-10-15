package com.abhiram.music.ui.component.items

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhiram.innertube.models.PlaylistItem as PlaylistItemModel

/**
 * Remote (InnerTube) playlist item in list or grid form.
 *
 * Round-square artwork (music-note placeholder when the playlist has no thumbnail), playlist
 * title and a "curator • song count" subtitle; press feedback matches the other containers.
 *
 * @param item InnerTube playlist model.
 * @param onClick invoked on tap (opens the playlist).
 * @param modifier root modifier for the cell/row.
 * @param onLongClick invoked on long press, or `null`.
 * @param gridItem renders the square grid cell variant instead of the list row.
 */
@Composable
fun PlaylistItem(
    item: PlaylistItemModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    gridItem: Boolean = true,
) {
    val subtitleText = joinSubtitles(item.author?.name, item.songCountText)
    val subtitleStyle =
        if (gridItem) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall
    val subtitleSlot = plainSubtitleContent(subtitleText, subtitleStyle)

    if (gridItem) {
        GridCell(
            title = item.title,
            onClick = onClick,
            modifier = modifier,
            thumbnailRatio = 1f,
            thumbnail = { Artwork(item.thumbnail, Modifier.fillMaxSize()) },
            subtitleContent = subtitleSlot,
            onLongClick = onLongClick,
        )
    } else {
        ListItemRow(
            title = item.title,
            onClick = onClick,
            modifier = modifier,
            thumbnail = { Artwork(item.thumbnail, Modifier.fillMaxSize()) },
            subtitleContent = subtitleSlot,
            onLongClick = onLongClick,
        )
    }
}
