package com.abhiram.music.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhiram.innertube.models.AlbumItem
import com.abhiram.innertube.models.ArtistItem
import com.abhiram.innertube.models.EpisodeItem
import com.abhiram.innertube.models.PlaylistItem
import com.abhiram.innertube.models.PodcastItem
import com.abhiram.innertube.models.SearchSuggestions
import com.abhiram.innertube.models.YTItem
import com.abhiram.innertube.models.SongItem as InnertubeSongItem
import com.abhiram.music.database.entities.SearchHistory
import com.abhiram.music.ui.component.items.AlbumItem
import com.abhiram.music.ui.component.items.ArtistItem
import com.abhiram.music.ui.component.items.PlaylistItem
import com.abhiram.music.ui.component.items.SongItem
import com.abhiram.music.ui.glass.LiquidGlassSurface
import com.abhiram.music.ui.theme.expressiveShapes

/**
 * Expressive floating search bar for the search screen.
 *
 * Renders a liquid-glass pill (deep backdrop blur + translucent tint + concentric border +
 * deep shadow; graceful tint/border fallback below API 33) with back and clear buttons, an
 * auto-focused single line text field with the IME search action, and an optional
 * [supportingContent] area below the pill that hosts the recent-search chips row (blank query)
 * or the inline suggestion list (typing).
 */
@Composable
fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
    supportingContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(modifier = modifier) {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = MaterialTheme.expressiveShapes.pill,
            tint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
            borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .heightIn(min = 52.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            onSearch(query)
                        },
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search YouTube Music",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear query",
                        )
                    }
                } else {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        supportingContent?.invoke(this)
    }
}

/**
 * Horizontally scrolling row of recent-search chips rendered under the search pill while the
 * query is blank. Every chip is ≥48 dp, springs on press and carries its own delete affordance.
 */
@Composable
fun RecentSearchChips(
    history: List<SearchHistory>,
    onQueryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onHistoryDelete: (String) -> Unit = {},
) {
    if (history.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(items = history, key = { "history_${it.id ?: it.query}" }) { entry ->
            RecentSearchChip(
                query = entry.query,
                onClick = { onQueryClick(entry.query) },
                onDelete = { onHistoryDelete(entry.query) },
            )
        }
    }
}

/** Inline suggestion list rendered under the search pill while the user is typing. */
@Composable
fun SearchSuggestionList(
    suggestions: SearchSuggestions,
    onQueryClick: (String) -> Unit,
    onSongClick: (InnertubeSongItem) -> Unit,
    onNavigateToItem: (YTItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        suggestions.queries.take(5).forEach { suggestion ->
            SuggestionRow(
                icon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
                text = suggestion,
                onClick = { onQueryClick(suggestion) },
            )
        }
        suggestions.recommendedItems.take(3).distinctBy { it.id }.forEach { item ->
            when (item) {
                is InnertubeSongItem -> SongItem(item = item, onClick = { onSongClick(item) })

                is AlbumItem -> AlbumItem(item = item, onClick = { onNavigateToItem(item) }, gridItem = false)
                is ArtistItem -> ArtistItem(item = item, onClick = { onNavigateToItem(item) }, gridItem = false)
                is PlaylistItem -> PlaylistItem(item = item, onClick = { onNavigateToItem(item) }, gridItem = false)
                is PodcastItem ->
                    PlaylistItem(
                        item = item.asPlaylistItem(),
                        onClick = { onNavigateToItem(item.asPlaylistItem()) },
                        gridItem = false,
                    )

                is EpisodeItem -> SongItem(item = item.asSongItem(), onClick = { onSongClick(item.asSongItem()) })
                else -> Unit
            }
        }
    }
}

/** One pill-shaped recent-search chip with a trailing delete button. */
@Composable
private fun RecentSearchChip(
    query: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.expressiveShapes.pill,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = query,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Delete \"$query\" from history",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** A single inline suggestion row (icon + text, ≥48 dp, spring press). */
@Composable
private fun SuggestionRow(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp)
            .heightIn(min = 48.dp),
    ) {
        icon()
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
