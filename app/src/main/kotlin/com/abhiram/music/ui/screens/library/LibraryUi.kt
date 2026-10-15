package com.abhiram.music.ui.screens.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.component.ShimmerHost
import com.abhiram.music.ui.component.TextPlaceholder
import com.abhiram.music.ui.component.TooltipIconButton
import com.abhiram.music.ui.component.items.ItemPlaceholder
import com.abhiram.music.ui.component.items.ListItemPlaceholder
import com.abhiram.music.ui.glass.LiquidGlassSurface
import com.abhiram.music.ui.menu.MediaItemMenu
import com.abhiram.music.ui.navigation.Routes

/* =============================================================================================
 *  Headers
 * ========================================================================================== */

/**
 * Expressive screen header used by every library tab: a chunky title, an optional item count
 * and a trailing action slot (sort direction, view toggles…).
 *
 * @param title Screen title (e.g. `"Songs"`).
 * @param modifier Modifier applied to the header row.
 * @param count Optional row count rendered next to the title.
 * @param unitLabel Plural noun used with [count] (`"songs"`, `"albums"`…).
 * @param actions Trailing actions, each at least 48 dp.
 */
@Composable
internal fun LibraryScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    unitLabel: String = "items",
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (count != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = "$count $unitLabel",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        actions()
    }
}

/**
 * Small section heading used to separate groups inside a tab (built-in playlists, your
 * playlists…).
 *
 * @param title Heading text.
 * @param modifier Modifier applied to the text.
 */
@Composable
internal fun LibrarySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

/* =============================================================================================
 *  Filter / sort chips
 * ========================================================================================== */

/**
 * Horizontally scrolling chip row shared by the filter and the sort rows of the library tabs.
 *
 * Chips keep a 16 dp gutter on both ends; the optional [trailing] slot sits after the last chip
 * (used for the sort-direction toggle).
 *
 * @param options Every selectable option, in display order.
 * @param key Currently selected option key.
 * @param onSelected Invoked with the key of a tapped chip.
 * @param modifier Modifier applied to the row.
 * @param trailing Optional trailing widget.
 */
@Composable
internal fun <T> LibraryChipRow(
    options: List<ChipOption<T>>,
    key: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option.key == key,
                onClick = { onSelected(option.key) },
                label = { Text(option.label) },
            )
        }
        trailing()
    }
}

/**
 * 48 dp toggle that flips the active sort direction (ascending ⇄ descending).
 *
 * @param descending `true` when the list currently sorts descending.
 * @param onClick Invoked on tap.
 * @param modifier Modifier applied to the button.
 */
@Composable
internal fun SortDirectionButton(
    descending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TooltipIconButton(
        contentDescription = if (descending) "Sort ascending" else "Sort descending",
        onClick = onClick,
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            imageVector = if (descending) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* =============================================================================================
 *  Loading / empty / error states
 * ========================================================================================== */

/**
 * Shimmering list skeleton (one [ListItemPlaceholder] per row) drawn by a single [ShimmerHost].
 *
 * @param modifier Modifier applied to the skeleton.
 * @param rows Number of placeholder rows to draw.
 */
@Composable
internal fun LibraryLoadingList(
    modifier: Modifier = Modifier,
    rows: Int = 6,
) {
    ShimmerHost(modifier = modifier.fillMaxWidth()) {
        Column {
            repeat(rows) { ListItemPlaceholder() }
        }
    }
}

/**
 * Shimmering adaptive grid skeleton; the whole grid lives inside one [ShimmerHost] so every cell
 * shimmers in phase.
 *
 * @param modifier Modifier applied to the skeleton.
 * @param cells Number of placeholder cells to draw.
 * @param minCellSize Minimum cell width of the adaptive grid — keep in sync with the real grid.
 */
@Composable
internal fun LibraryLoadingGrid(
    modifier: Modifier = Modifier,
    cells: Int = 12,
    minCellSize: Dp = 140.dp,
) {
    ShimmerHost(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = minCellSize),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(cells) { ItemPlaceholder() }
        }
    }
}

/**
 * Centered empty state: an optional muted glyph above a [TextPlaceholder].
 *
 * @param text Message shown when the tab has nothing to display.
 * @param modifier Modifier applied to the state.
 * @param icon Optional leading glyph.
 */
@Composable
internal fun LibraryEmptyState(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.height(12.dp))
        }
        TextPlaceholder(text = text)
    }
}

/**
 * Centered failure state with a retry action.
 *
 * @param message Failure message shown under the headline.
 * @param onRetry Invoked by the retry button.
 * @param modifier Modifier applied to the state.
 */
@Composable
internal fun LibraryErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Couldn't load your library",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/* =============================================================================================
 *  Floating glass actions
 * ========================================================================================== */

/**
 * Floating Liquid-Glass action button (play all, shuffle, new playlist…).
 *
 * The 56 dp glass pill comes from the frozen [LiquidGlassSurface] — its backdrop blur degrades
 * to a tint + border fallback below Android 13 — with a ≥48 dp [TooltipIconButton] on top.
 *
 * @param icon Glyph shown in the button.
 * @param contentDescription Accessibility label of the button.
 * @param onClick Invoked on tap.
 * @param modifier Modifier applied to the glass surface.
 * @param tint Colour of [icon].
 */
@Composable
internal fun GlassActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    LiquidGlassSurface(modifier = modifier.size(56.dp)) {
        TooltipIconButton(
            contentDescription = contentDescription,
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
            )
        }
    }
}

/* =============================================================================================
 *  Long-press menu
 * ========================================================================================== */

/**
 * Hosts the shared [MediaItemMenu] (owner CMP-B) for the currently long-pressed row.
 *
 * Single integration point between the library tabs and `ui.menu`: pass the targeted track (or
 * `null` to close) plus the screen's `onNavigate` so the "view artist"/"view album" entries
 * work.
 *
 * @param item Track the menu describes; `null` renders nothing.
 * @param onDismiss Invoked when the sheet should close.
 * @param onNavigate Receives the destination route of a navigation entry.
 */
@Composable
internal fun MediaMenuHost(
    item: MediaMetadata?,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    if (item == null) return
    MediaItemMenu(
        item = item,
        onDismiss = onDismiss,
        onArtistClick = { artist -> artist.id?.let { id -> onNavigate(Routes.artist(id)) } },
        onAlbumClick = { album -> onNavigate(Routes.album(album.id)) },
    )
}
