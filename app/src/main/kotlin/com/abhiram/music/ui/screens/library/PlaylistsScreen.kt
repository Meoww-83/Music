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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.music.model.BuiltInPlaylist
import com.abhiram.music.ui.component.TextFieldDialog
import com.abhiram.music.ui.component.items.BuiltInPlaylistItem
import com.abhiram.music.ui.component.items.LocalPlaylistItem
import com.abhiram.music.ui.navigation.Routes

/**
 * Library playlists tab (route `playlists`).
 *
 * Sort chips (name / date created / recently updated + direction toggle) above a single adaptive
 * grid holding the built-in playlist cards (Favorites, History, Top played →
 * `Routes.builtInPlaylist(id)`) followed by the device playlists
 * (`Routes.localPlaylist(id)`). The floating glass action opens the frozen [TextFieldDialog] and
 * creates a playlist through `MusicRepository.createLocalPlaylist(name)`.
 *
 * @param onNavigate Invoked with `builtInPlaylist/{id}` or `localPlaylist/{id}` when a card is
 * opened.
 * @param modifier Modifier applied to the screen root.
 * @param viewModel View model holding `MusicRepository.localPlaylists()` plus the chip state.
 */
@Composable
fun PlaylistsScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryPlaylistsViewModel = viewModel(factory = LibraryPlaylistsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val sortDescending by viewModel.sortDescending.collectAsStateWithLifecycle()

    val error = state.error
    val playlists = state.items
    val isLoading = state.isLoading
    val gridState = rememberLazyGridState()
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        LibraryScreenHeader(
            title = "Playlists",
            count = playlists.size,
            unitLabel = "playlists",
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
        )

        LibraryChipRow(
            options = PlaylistSortType.entries.map { ChipOption(it, it.label) },
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

                else ->
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LibrarySectionHeader(title = "Built-in")
                        }
                        items(
                            items = BUILT_IN_PLAYLISTS,
                            key = { builtIn -> builtIn.id },
                        ) { builtIn ->
                            BuiltInPlaylistItem(
                                builtIn = builtIn,
                                onClick = { onNavigate(Routes.builtInPlaylist(builtIn.id)) },
                                gridItem = true,
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LibrarySectionHeader(title = "Your playlists")
                        }
                        if (playlists.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LibraryEmptyState(
                                    text = "No playlists yet — tap + to create one",
                                    icon = Icons.Rounded.PlaylistPlay,
                                )
                            }
                        } else {
                            items(
                                items = playlists,
                                key = { playlist -> playlist.id },
                            ) { playlist ->
                                LocalPlaylistItem(
                                    playlist = playlist,
                                    onClick = { onNavigate(Routes.localPlaylist(playlist.id)) },
                                    gridItem = true,
                                )
                            }
                        }
                    }
            }

            if (!isLoading && error == null) {
                GlassActionButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = "New playlist",
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        .padding(16.dp),
                )
            }
        }
    }

    if (showCreateDialog) {
        TextFieldDialog(
            title = "New playlist",
            value = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                showCreateDialog = false
                viewModel.createPlaylist(name)
            },
            placeholder = "Playlist name",
            label = "Name",
        )
    }
}

/** Built-in playlists offered at the top of the playlists tab, in display order. */
private val BUILT_IN_PLAYLISTS: List<BuiltInPlaylist> =
    listOf(BuiltInPlaylist.FAVORITES, BuiltInPlaylist.HISTORY, BuiltInPlaylist.TOP)
