package com.abhiram.music.ui.component.items

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhiram.innertube.models.AlbumItem as AlbumItemModel

/**
 * Remote (InnerTube) album item in list or grid form.
 *
 * Round-square artwork (morphing toward a circle while pressed), album title (two-line marquee
 * in grid mode) and a subtitle of "artists • year" with an explicit "E" badge when needed.
 *
 * @param item InnerTube album model.
 * @param onClick invoked on tap (opens the album).
 * @param modifier root modifier for the cell/row.
 * @param onLongClick invoked on long press, or `null`.
 * @param gridItem renders the square grid cell variant instead of the list row.
 */
@Composable
fun AlbumItem(
    item: AlbumItemModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    gridItem: Boolean = true,
) {
    val subtitleText = joinSubtitles(
        item.artists?.joinToString(", ") { it.name },
        item.year?.toString(),
    )
    val subtitleStyle =
        if (gridItem) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall
    val subtitleSlot = songSubtitleContent(
        explicit = item.explicit,
        text = subtitleText,
        style = subtitleStyle,
    )

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
