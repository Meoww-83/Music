package com.abhiram.music.ui.component.items

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhiram.innertube.models.ArtistItem as ArtistItemModel

/**
 * Remote (InnerTube) artist item in list or grid form.
 *
 * Circular artwork (no corner morph — circles stay circles), artist name as the only line of
 * text; press feedback matches the other containers.
 *
 * @param item InnerTube artist model.
 * @param onClick invoked on tap (opens the artist).
 * @param modifier root modifier for the cell/row.
 * @param onLongClick invoked on long press, or `null`.
 * @param gridItem renders the square grid cell variant instead of the list row.
 */
@Composable
fun ArtistItem(
    item: ArtistItemModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    gridItem: Boolean = true,
) {
    if (gridItem) {
        GridCell(
            title = item.title,
            onClick = onClick,
            modifier = modifier,
            thumbnailRatio = 1f,
            thumbnail = { Artwork(item.thumbnail, Modifier.fillMaxSize(), circle = true) },
            onLongClick = onLongClick,
        )
    } else {
        ListItemRow(
            title = item.title,
            onClick = onClick,
            modifier = modifier,
            thumbnail = { Artwork(item.thumbnail, Modifier.fillMaxSize(), circle = true) },
            onLongClick = onLongClick,
        )
    }
}
