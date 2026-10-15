package com.abhiram.music.ui.component.items

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhiram.innertube.models.SongItem as SongItemModel
import com.abhiram.music.model.toMediaMetadata

/**
 * Remote (InnerTube) song item in list or grid form.
 *
 * Maps the catalogue model onto [MediaMetadata] and delegates to [MediaSongItem], so remote
 * songs render identically to local ones: round-square artwork with press morph, title, artist
 * line (plus explicit "E" badge), duration or the caller's trailing/menu slot, and optional
 * leading slot (index / drag handle / playing bars).
 *
 * @param item InnerTube song model.
 * @param onClick invoked on tap.
 * @param modifier root modifier for the cell/row.
 * @param onLongClick invoked on long press (opens the item menu), or `null`.
 * @param trailingContent row-scoped slot rendered at the end of the row / as the cell's
 * artwork overlay in grid mode; when `null`, the duration is shown instead.
 * @param leadingContent row-scoped slot rendered before the artwork (list mode).
 * @param gridItem renders the square grid cell variant instead of the list row.
 */
@Composable
fun SongItem(
    item: SongItemModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
    gridItem: Boolean = false,
) {
    MediaSongItem(
        item = item.toMediaMetadata(),
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        gridItem = gridItem,
    )
}
