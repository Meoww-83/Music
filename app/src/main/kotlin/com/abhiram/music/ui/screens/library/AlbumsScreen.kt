package com.abhiram.music.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.music.ui.component.items.LocalAlbumItem
import com.abhiram.music.ui.navigation.Routes

/**
 * Library albums tab (route `albums`).
 *
 * Sort chips (title / year / songs / most played + direction toggle) above an adaptive grid of
 * [LocalAlbumItem] cells; tapping a cell opens the album detail screen (`Routes.album(id)`).
 * Shimmer, empty and error states are handled by the shared library components.
 *
 * @param onNavigate Invoked with `album/{id}` when a cell is opened.
 * @param modifier Modifier applied to the screen root.
 * @param viewModel View model holding `dao.albums()` plus the chip state.
 */
@Composable
fun AlbumsScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryAlbumsViewModel = viewModel(factory = LibraryAlbumsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val sortDescending by viewModel.sortDescending.collectAsStateWithLifecycle()

    val error = state.error
    val albums = state.items
    val isLoading = state.isLoading
    val gridState = rememberLazyGridState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        LibraryScreenHeader(
            title = "Albums",
            count = albums.size,
            unitLabel = "albums",
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
        )

        LibraryChipRow(
            options = AlbumSortType.entries.map { ChipOption(it, it.label) },
            key = sort,
            onSelected = viewModel::setSort,
        ) {
            SortDirectionButton(
                descending = sortDescending,
                onClick = viewModel::toggleSortDescending,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                isLoading ->
                    LibraryLoadingGrid(modifier = Modifier.fillMaxSize())

                error != null ->
                    LibraryErrorState(
                        message = error,
                        onRetry = viewModel::retry,
                        modifier = Modifier.align(Alignment.Center),
                    )

                albums.isEmpty() ->
                    LibraryEmptyState(
                        text = "No albums in your library yet",
                        icon = Icons.Rounded.Album,
                        modifier = Modifier.align(Alignment.Center),
                    )

                else ->
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = albums,
                            key = { album -> album.id },
                        ) { album ->
                            LocalAlbumItem(
                                album = album,
                                onClick = { onNavigate(Routes.album(album.id)) },
                                gridItem = true,
                            )
                        }
                    }
            }
        }
    }
}
