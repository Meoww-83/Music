package com.abhiram.music.ui.component.items

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.abhiram.music.model.BuiltInPlaylist

/**
 * System (built-in) playlist — Favorites, Offline, Downloaded, History, Top played, Cache — in
 * list or grid form.
 *
 * These playlists have no remote artwork, so the cell shows a tinted vector on the standard
 * surface block: `error` red for Favorites, `primary` for everything else. No subtitle line;
 * the title (two-line marquee in grid mode) carries the row.
 *
 * @param builtIn which built-in playlist to render.
 * @param onClick invoked on tap (opens the built-in playlist).
 * @param modifier root modifier for the cell/row.
 * @param onLongClick invoked on long press, or `null`.
 * @param gridItem renders the square grid cell variant instead of the list row.
 */
@Composable
fun BuiltInPlaylistItem(
    builtIn: BuiltInPlaylist,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    gridItem: Boolean = true,
) {
    val iconTint: Color =
        if (builtIn == BuiltInPlaylist.FAVORITES) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
    val thumbnail: @Composable () -> Unit = {
        Artwork(
            url = null,
            modifier = Modifier.fillMaxSize(),
            icon = builtIn.icon,
            iconTint = iconTint,
        )
    }

    if (gridItem) {
        GridCell(
            title = builtIn.title,
            onClick = onClick,
            modifier = modifier,
            thumbnail = thumbnail,
            onLongClick = onLongClick,
        )
    } else {
        ListItemRow(
            title = builtIn.title,
            onClick = onClick,
            modifier = modifier,
            thumbnail = thumbnail,
            onLongClick = onLongClick,
        )
    }
}

/** Fallback vector drawn on the artwork of each built-in playlist. */
private val BuiltInPlaylist.icon: ImageVector
    get() = when (this) {
        BuiltInPlaylist.FAVORITES -> Icons.Rounded.Favorite
        BuiltInPlaylist.OFFLINE -> Icons.Rounded.CloudDone
        BuiltInPlaylist.DOWNLOADED -> Icons.Rounded.Download
        BuiltInPlaylist.HISTORY -> Icons.Rounded.History
        BuiltInPlaylist.TOP -> Icons.Rounded.TrendingUp
        BuiltInPlaylist.CACHE -> Icons.Rounded.Storage
    }
