package com.abhiram.music.ui.screens.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.navigation.Routes
import com.abhiram.music.ui.component.screen.AlbumScreen
import com.abhiram.music.ui.menu.MediaItemMenu

/**
 * Album detail screen — route `album/{id}`.
 *
 * Assembles the CMP-D [AlbumScreen] (cover header + pill action row with the save-to-library
 * affordance) on top of an online-first
 * [com.abhiram.music.ui.screens.detail.AlbumDetailViewModel]: the InnerTube album page is
 * preferred and the Room cache takes over when the page cannot be fetched. Long-pressed rows
 * open the shared [MediaItemMenu]; the header's ⋮ opens the screen overflow (share / save).
 *
 * @param id album browse id (the same value is stored as the local `AlbumEntity.id`).
 * @param onNavigate navigates to a deeper route (`artist/{id}`, `album/{id}`, `player/stats`).
 * @param onBack pops back to the previous destination.
 * @param modifier sizing modifier.
 */
@Composable
fun AlbumDetailScreen(
    id: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AlbumDetailViewModel = viewModel(factory = AlbumDetailViewModel.factory(id))
    val state by viewModel.state.collectAsState()

    var menuSong by remember { mutableStateOf<MediaMetadata?>(null) }
    var overflowOpen by remember { mutableStateOf(false) }

    val share = rememberShareAction(link = "https://music.youtube.com/album/$id", title = "Album")
    val openMenu: (MediaMetadata) -> Unit = { song -> menuSong = song }
    val songs = state.songs

    Box(modifier = modifier.fillMaxSize()) {
        DetailScaffold(
            onBack = onBack,
            refreshing = state.isLoading && state.online != null,
            onRefresh = viewModel::refresh,
        ) {
            val online = state.online
            if (online != null) {
                AlbumScreen(
                    album = online.album,
                    songs = songs,
                    error = state.error,
                    onRetry = viewModel::refresh,
                    onAddToLibrary = { viewModel.saveToLibrary(songs) },
                    onMenu = { overflowOpen = true },
                    onArtistClick = { artist ->
                        artist.id?.let { artistId -> onNavigate(Routes.artist(artistId)) }
                    },
                    onSongLongClick = openMenu,
                    onSongMenu = openMenu,
                )
            } else {
                // Local-only (or still loading): the Room album when present, otherwise a blank
                // placeholder so the shared screen can render its shimmer state.
                AlbumScreen(
                    album = state.local ?: placeholderLocalAlbum(id),
                    songs = state.localSongs,
                    isLoading = state.isLoading && state.local == null,
                    onAddToLibrary = { viewModel.saveToLibrary(songs) },
                    onArtistClick = { artist ->
                        artist.id?.let { artistId -> onNavigate(Routes.artist(artistId)) }
                    },
                    onSongLongClick = openMenu,
                    onSongMenu = openMenu,
                )
            }
        }

        DetailOverflowMenu(
            expanded = overflowOpen,
            onDismissRequest = { overflowOpen = false },
            items = listOf(
                DetailOverflowItem(label = "Share", onClick = share),
                DetailOverflowItem(
                    label = "Save to library",
                    enabled = songs.isNotEmpty(),
                    onClick = { viewModel.saveToLibrary(songs) },
                ),
            ),
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }

    MediaItemMenu(
        item = menuSong ?: return,
        onDismiss = { menuSong = null },
        onArtistClick = { artist -> artist.id?.let { artistId -> onNavigate(Routes.artist(artistId)) } },
        onAlbumClick = { album -> onNavigate(Routes.album(album.id)) },
        onShowStats = { onNavigate(Routes.PLAYER_STATS) },
    )
}
