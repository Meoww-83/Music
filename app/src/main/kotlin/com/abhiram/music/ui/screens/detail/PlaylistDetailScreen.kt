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
import com.abhiram.music.ui.component.screen.PlaylistScreen
import com.abhiram.music.ui.menu.MediaItemMenu

/**
 * Online playlist detail screen — route `playlist/{id}`.
 *
 * Assembles the CMP-D [PlaylistScreen] (cover header with song-count thumbnail, expandable
 * description, pill action row) on top of
 * [com.abhiram.music.ui.screens.detail.PlaylistDetailViewModel], which follows every song
 * continuation so the list is complete before it renders. Long-pressed rows open the shared
 * [MediaItemMenu]; the header's ⋮ opens the screen overflow (share).
 *
 * @param id InnerTube playlist browse id.
 * @param onNavigate navigates to a deeper route (`artist/{id}`, `album/{id}`, `player/stats`).
 * @param onBack pops back to the previous destination.
 * @param modifier sizing modifier.
 */
@Composable
fun PlaylistDetailScreen(
    id: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PlaylistDetailViewModel = viewModel(factory = PlaylistDetailViewModel.factory(id))
    val state by viewModel.state.collectAsState()

    var menuSong by remember { mutableStateOf<MediaMetadata?>(null) }
    var overflowOpen by remember { mutableStateOf(false) }

    val playlist = state.playlist
    val share = rememberShareAction(
        link = "https://music.youtube.com/playlist?list=$id",
        title = playlist?.title?.ifBlank { "Playlist" } ?: "Playlist",
    )
    val openMenu: (MediaMetadata) -> Unit = { song -> menuSong = song }

    Box(modifier = modifier.fillMaxSize()) {
        DetailScaffold(
            onBack = onBack,
            refreshing = state.isLoading && playlist != null,
            onRefresh = viewModel::refresh,
        ) {
            // While loading (or after a failure without cached data) a blank placeholder item
            // keeps the shared screen renderable — it draws its own shimmer/error states.
            PlaylistScreen(
                playlist = playlist ?: placeholderPlaylistItem(id),
                songs = state.songs,
                isLoading = state.isLoading && playlist == null,
                error = state.error,
                onRetry = viewModel::refresh,
                onMenu = { overflowOpen = true },
                onAuthorClick = { author ->
                    author.id?.let { authorId -> onNavigate(Routes.artist(authorId)) }
                },
                onSongLongClick = openMenu,
                onSongMenu = openMenu,
            )
        }

        DetailOverflowMenu(
            expanded = overflowOpen,
            onDismissRequest = { overflowOpen = false },
            items = listOf(DetailOverflowItem(label = "Share", onClick = share)),
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
