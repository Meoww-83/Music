package com.abhiram.music.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abhiram.innertube.models.EpisodeItem
import com.abhiram.innertube.models.PodcastItem
import com.abhiram.innertube.models.YTItem
import com.abhiram.innertube.models.AlbumItem as InnertubeAlbumItem
import com.abhiram.innertube.models.ArtistItem as InnertubeArtistItem
import com.abhiram.innertube.models.PlaylistItem as InnertubePlaylistItem
import com.abhiram.innertube.models.SongItem as InnertubeSongItem
import com.abhiram.music.ui.component.ShimmerHost
import com.abhiram.music.ui.component.TextPlaceholder
import com.abhiram.music.ui.component.items.AlbumItem
import com.abhiram.music.ui.component.items.ArtistItem
import com.abhiram.music.ui.component.items.ListItemPlaceholder
import com.abhiram.music.ui.component.items.PlaylistItem
import com.abhiram.music.ui.component.items.SongItem
import com.abhiram.music.ui.component.items.VideoItem
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

/** Key of the sentinel list item that triggers the next continuation page when it scrolls in. */
private const val LoadMoreKey = "search_load_more"

/**
 * Search results body: pinned filter chips (All / Songs / Albums / Artists / Videos / Playlists,
 * mapped to the real InnerTube search filters) above a scrolling list.
 *
 * Renders sectioned results for [SearchFilterType.ALL] and a flat, continuation-paged list for
 * every other chip. Shows shimmer placeholders while loading, a [TextPlaceholder] when the
 * response is empty and a retry state on failure; scrolling the [LoadMoreKey] sentinel into view
 * requests the next page through `onLoadMore`.
 */
@Composable
fun SearchResults(
    filter: SearchFilterType,
    onFilterChange: (SearchFilterType) -> Unit,
    sections: List<SearchSection>,
    filteredItems: List<YTItem>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onSongClick: (InnertubeSongItem) -> Unit,
    onAlbumClick: (InnertubeAlbumItem) -> Unit,
    onArtistClick: (InnertubeArtistItem) -> Unit,
    onPlaylistClick: (InnertubePlaylistItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val hasContent = sections.isNotEmpty() || filteredItems.isNotEmpty()

    LaunchedEffect(listState, filter) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.any { it.key == LoadMoreKey } }
            .distinctUntilChanged()
            .collect { visible -> if (visible) onLoadMore() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchFilterChips(selected = filter, onSelected = onFilterChange)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 96.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                val itemContent: @Composable LazyItemScope.(YTItem) -> Unit = { item ->
                    when (item) {
                        is InnertubeSongItem ->
                            if (item.isVideoSong) {
                                VideoItem(item = item, onClick = { onSongClick(item) })
                            } else {
                                SongItem(item = item, onClick = { onSongClick(item) })
                            }

                        is InnertubeAlbumItem ->
                            AlbumItem(item = item, onClick = { onAlbumClick(item) }, gridItem = false)

                        is InnertubeArtistItem ->
                            ArtistItem(item = item, onClick = { onArtistClick(item) }, gridItem = false)

                        is InnertubePlaylistItem ->
                            PlaylistItem(item = item, onClick = { onPlaylistClick(item) }, gridItem = false)

                        is PodcastItem ->
                            PlaylistItem(
                                item = item.asPlaylistItem(),
                                onClick = { onPlaylistClick(item.asPlaylistItem()) },
                                gridItem = false,
                            )

                        is EpisodeItem ->
                            SongItem(item = item.asSongItem(), onClick = { onSongClick(item.asSongItem()) })

                        else ->
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                    }
                }

                when {
                    isLoading && !hasContent ->
                        item(key = "shimmer") {
                            ShimmerHost {
                                repeat(8) { ListItemPlaceholder() }
                            }
                        }

                    error != null && !hasContent ->
                        item(key = "error") {
                            SearchError(
                                message = error,
                                onRetry = onRetry,
                            )
                        }

                    filter == SearchFilterType.ALL -> {
                        sections.forEach { section ->
                            item(key = "header_${section.title}") {
                                SectionHeader(title = section.title)
                            }
                            items(
                                items = section.items,
                                key = { "${section.title}_${it.id}" },
                                itemContent = itemContent,
                            )
                        }
                        if (sections.isEmpty() && !isLoading) {
                            item(key = "empty") { TextPlaceholder(text = "No results found") }
                        }
                    }

                    else -> {
                        items(
                            items = filteredItems,
                            key = { "filtered_${it.id}" },
                            itemContent = itemContent,
                        )
                        if (filteredItems.isEmpty() && !isLoading) {
                            item(key = "empty") { TextPlaceholder(text = "No results found") }
                        }
                        if ((isLoadingMore || (hasMore && filteredItems.isNotEmpty())) && !isLoading) {
                            item(key = LoadMoreKey) {
                                ShimmerHost {
                                    repeat(3) { ListItemPlaceholder() }
                                }
                            }
                        }
                    }
                }

                if (error != null && hasContent) {
                    item(key = "error_footer") {
                        SearchError(
                            message = error,
                            onRetry = onRetry,
                            compact = true,
                        )
                    }
                }
            }
        }
    }
}

/** Horizontally scrolling filter chip row mapped to `YouTube.SearchFilter` values. */
@Composable
fun SearchFilterChips(
    selected: SearchFilterType,
    onSelected: (SearchFilterType) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = SearchFilterType.entries) { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelected(type) },
                label = { Text(type.label) },
            )
        }
    }
}

/** Expressive section header used between result groups of the "All" filter. */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Centered failure state with a retry action. */
@Composable
private fun SearchError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = if (compact) 16.dp else 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Couldn't load results",
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
