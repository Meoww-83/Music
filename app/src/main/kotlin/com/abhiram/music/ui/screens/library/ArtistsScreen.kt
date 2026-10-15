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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.music.ui.component.items.LocalArtistItem
import com.abhiram.music.ui.navigation.Routes

/**
 * Library artists tab (route `artists`).
 *
 * Sort chips (title / songs / most played + direction toggle) above an adaptive grid of
 * [LocalArtistItem] cells; tapping a cell opens the artist detail screen
 * (`Routes.artist(id)`). Shimmer, empty and error states are handled by the shared library
 * components.
 *
 * @param onNavigate Invoked with `artist/{id}` when a cell is opened.
 * @param modifier Modifier applied to the screen root.
 * @param viewModel View model holding `dao.artists()` plus the chip state.
 */
@Composable
fun ArtistsScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryArtistsViewModel = viewModel(factory = LibraryArtistsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val sortDescending by viewModel.sortDescending.collectAsStateWithLifecycle()

    val error = state.error
    val artists = state.items
    val isLoading = state.isLoading
    val gridState = rememberLazyGridState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        LibraryScreenHeader(
            title = "Artists",
            count = artists.size,
            unitLabel = "artists",
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
        )

        LibraryChipRow(
            options = ArtistSortType.entries.map { ChipOption(it, it.label) },
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

                artists.isEmpty() ->
                    LibraryEmptyState(
                        text = "No artists in your library yet",
                        icon = Icons.Rounded.Person,
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
                            items = artists,
                            key = { artist -> artist.id },
                        ) { artist ->
                            LocalArtistItem(
                                artist = artist,
                                onClick = { onNavigate(Routes.artist(artist.id)) },
                                gridItem = true,
                            )
                        }
                    }
            }
        }
    }
}
