package com.abhiram.music.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.innertube.models.AlbumItem
import com.abhiram.innertube.models.ArtistItem
import com.abhiram.innertube.models.PlaylistItem
import com.abhiram.innertube.models.PodcastItem
import com.abhiram.innertube.models.YTItem
import com.abhiram.innertube.models.SongItem as InnertubeSongItem
import com.abhiram.music.model.toMediaMetadata
import com.abhiram.music.playback.PlayerControllerProvider
import com.abhiram.music.ui.component.TextPlaceholder
import com.abhiram.music.ui.navigation.Routes

/**
 * The search screen (route `search`).
 *
 * Assembles the floating [SearchInputField] (recent-search chips while idle, debounced inline
 * suggestions while typing) over the [SearchResults] body, driven entirely by [SearchViewModel].
 * Committed queries are persisted through `MusicRepository.pushSearchHistory()` (when enabled)
 * and songs play through `PlayerControllerProvider.controller?.playAll(items, index)`.
 *
 * @param onBack invoked when the user backs out of the search screen (empty query).
 * @param onNavigate invoked with a destination route (`artist/{id}`, `album/{id}`,
 * `playlist/{id}`) when a non-playable result or suggestion is opened.
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val query by viewModel.query.collectAsStateWithLifecycle()
    val resultsQuery by viewModel.resultsQuery.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val controller by PlayerControllerProvider.controller.collectAsStateWithLifecycle()

    val sections = viewModel.sections
    val filtered = viewModel.filteredResults[filter]
    val isLoading = viewModel.isLoading
    val isLoadingMore = viewModel.isLoadingMore
    val error = viewModel.errorMessage
    val playableItems = viewModel.playableItems

    // True while the user is editing the query — the inline suggestion list replaces the results.
    var isEditing by rememberSaveable { mutableStateOf(true) }

    val onSongClick: (InnertubeSongItem) -> Unit = remember(playableItems) {
        { song ->
            val index = playableItems.indexOfFirst { it.id == song.id }
            if (index >= 0) {
                controller?.playAll(playableItems.map { it.toMediaMetadata() }, index)
            } else {
                controller?.play(song.toMediaMetadata())
            }
        }
    }

    val onSuggestionNavigate: (YTItem) -> Unit = { item ->
        when (item) {
            is AlbumItem -> onNavigate(Routes.album(item.id))
            is ArtistItem -> onNavigate(Routes.artist(item.id))
            is PlaylistItem -> onNavigate(Routes.playlist(item.id))
            is PodcastItem -> onNavigate(Routes.playlist(item.asPlaylistItem().id))
            else -> Unit
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        SearchInputField(
            query = query,
            onQueryChange = { value ->
                isEditing = true
                viewModel.onQueryChange(value)
            },
            onSearch = { value ->
                keyboardController?.hide()
                isEditing = false
                viewModel.commitSearch(value)
            },
            onBack = {
                if (query.isNotBlank()) {
                    viewModel.clearQuery()
                    isEditing = true
                } else {
                    onBack()
                }
            },
            supportingContent = {
                if (query.isBlank()) {
                    RecentSearchChips(
                        history = history,
                        onQueryClick = { value ->
                            keyboardController?.hide()
                            isEditing = false
                            viewModel.commitSearch(value)
                        },
                        onHistoryDelete = viewModel::deleteHistory,
                    )
                } else if (isEditing && suggestions != null) {
                    SearchSuggestionList(
                        suggestions = suggestions.orEmptySuggestions(),
                        onQueryClick = { value ->
                            keyboardController?.hide()
                            isEditing = false
                            viewModel.commitSearch(value)
                        },
                        onSongClick = { song ->
                            keyboardController?.hide()
                            isEditing = false
                            onSongClick(song)
                        },
                        onNavigateToItem = { item ->
                            keyboardController?.hide()
                            isEditing = false
                            onSuggestionNavigate(item)
                        },
                    )
                }
            },
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
        ) {
            when {
                isEditing && query.isNotBlank() -> {
                    // The inline suggestion list (rendered by SearchInputField) takes over.
                }

                resultsQuery.isBlank() -> {
                    TextPlaceholder(
                        text = "Search YouTube Music",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    SearchResults(
                        filter = filter,
                        onFilterChange = viewModel::setFilter,
                        sections = sections,
                        filteredItems = filtered?.items.orEmpty(),
                        isLoading = isLoading,
                        isLoadingMore = isLoadingMore,
                        hasMore = filter != SearchFilterType.ALL && (filtered?.continuation != null),
                        error = error,
                        onRetry = { viewModel.commitSearch(resultsQuery) },
                        onLoadMore = viewModel::loadMore,
                        onSongClick = onSongClick,
                        onAlbumClick = { onNavigate(Routes.album(it.id)) },
                        onArtistClick = { onNavigate(Routes.artist(it.id)) },
                        onPlaylistClick = { onNavigate(Routes.playlist(it.id)) },
                    )
                }
            }
        }
    }
}

/**
 * Null-safe helper for the suggestion payload; the caller guarantees non-null via its branch
 * condition, this keeps the call site free of non-local returns.
 */
private fun com.abhiram.innertube.models.SearchSuggestions?.orEmptySuggestions(): com.abhiram.innertube.models.SearchSuggestions =
    this ?: com.abhiram.innertube.models.SearchSuggestions(queries = emptyList(), recommendedItems = emptyList())
