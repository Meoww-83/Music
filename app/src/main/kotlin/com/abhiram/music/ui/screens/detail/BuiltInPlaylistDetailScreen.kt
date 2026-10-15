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
import com.abhiram.music.model.BuiltInPlaylist
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.navigation.Routes
import com.abhiram.music.ui.component.ConfirmationDialog
import com.abhiram.music.ui.component.screen.BuiltInPlaylistScreen
import com.abhiram.music.ui.menu.InHistoryMediaItemMenu
import com.abhiram.music.ui.menu.MediaItemMenu

/**
 * Built-in playlist detail screen — route `builtInPlaylist/{id}`.
 *
 * Assembles the CMP-D [BuiltInPlaylistScreen] (signature artwork header + pill action row) on
 * top of [BuiltInPlaylistDetailViewModel]: Favorites from the liked songs flow, History from the
 * listening-event log and Top played from the play-count stats; the Offline / Downloaded /
 * Cache lists stay honestly empty until a download subsystem exists. Long-pressed rows open the
 * matching shared menu — [InHistoryMediaItemMenu] inside History (with per-song removal),
 * [MediaItemMenu] everywhere else. Local data is always fresh, so no refresh gesture is offered.
 *
 * @param id one of [BuiltInPlaylist]'s stable ids (`favorites`, `offline`, `downloaded`,
 * `history`, `top`, `cache`).
 * @param onNavigate navigates to a deeper route (`artist/{id}`, `album/{id}`, `player/stats`).
 * @param onBack pops back to the previous destination.
 * @param modifier sizing modifier.
 */
@Composable
fun BuiltInPlaylistDetailScreen(
    id: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val builtIn = BuiltInPlaylist.fromId(id)
    if (builtIn == null) {
        DetailScaffold(onBack = onBack, modifier = modifier) {
            DetailEmptyState(text = "Unknown playlist")
        }
        return
    }

    val viewModel: BuiltInPlaylistDetailViewModel =
        viewModel(factory = BuiltInPlaylistDetailViewModel.factory(id))
    val state by viewModel.state.collectAsState()

    var menuSong by remember { mutableStateOf<MediaMetadata?>(null) }
    var overflowOpen by remember { mutableStateOf(false) }
    var showClearHistory by remember { mutableStateOf(false) }

    val openMenu: (MediaMetadata) -> Unit = { song -> menuSong = song }

    Box(modifier = modifier.fillMaxSize()) {
        DetailScaffold(onBack = onBack) {
            BuiltInPlaylistScreen(
                builtIn = builtIn,
                songs = state.songs,
                isLoading = state.isLoading,
                onMenu = if (builtIn == BuiltInPlaylist.HISTORY) {
                    { overflowOpen = true }
                } else {
                    null
                },
                onSongLongClick = openMenu,
                onSongMenu = openMenu,
            )
        }

        if (builtIn == BuiltInPlaylist.HISTORY) {
            DetailOverflowMenu(
                expanded = overflowOpen,
                onDismissRequest = { overflowOpen = false },
                items = listOf(
                    DetailOverflowItem(
                        label = "Clear history",
                        destructive = true,
                        onClick = { showClearHistory = true },
                    ),
                ),
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }

    val song = menuSong
    if (song != null) {
        if (builtIn == BuiltInPlaylist.HISTORY) {
            InHistoryMediaItemMenu(
                item = song,
                onDismiss = { menuSong = null },
                onRemoveFromHistory = { viewModel.removeFromHistory(song.id) },
                onArtistClick = { artist ->
                    artist.id?.let { artistId -> onNavigate(Routes.artist(artistId)) }
                },
                onAlbumClick = { album -> onNavigate(Routes.album(album.id)) },
                onShowStats = { onNavigate(Routes.PLAYER_STATS) },
            )
        } else {
            MediaItemMenu(
                item = song,
                onDismiss = { menuSong = null },
                onArtistClick = { artist ->
                    artist.id?.let { artistId -> onNavigate(Routes.artist(artistId)) }
                },
                onAlbumClick = { album -> onNavigate(Routes.album(album.id)) },
                onShowStats = { onNavigate(Routes.PLAYER_STATS) },
            )
        }
    }

    if (showClearHistory) {
        ConfirmationDialog(
            title = "Clear history?",
            text = "Every listening event will be deleted. Your songs stay in the library.",
            onConfirm = {
                showClearHistory = false
                viewModel.clearHistory()
            },
            onDismiss = { showClearHistory = false },
            confirmText = "Clear",
        )
    }
}
