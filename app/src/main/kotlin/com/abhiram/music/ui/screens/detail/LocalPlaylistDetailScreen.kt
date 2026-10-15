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
import com.abhiram.music.ui.component.ConfirmationDialog
import com.abhiram.music.ui.component.TextFieldDialog
import com.abhiram.music.ui.component.screen.LocalPlaylistScreen
import com.abhiram.music.ui.menu.InPlaylistMediaItemMenu

/** Long-press target of the local playlist list: the row plus its current play order index. */
private data class PlaylistMenuTarget(
    val song: MediaMetadata,
    val index: Int,
)

/**
 * Local playlist detail screen — route `localPlaylist/{id}`.
 *
 * Assembles the CMP-D [LocalPlaylistScreen] (cover thumbnail mosaic, pill action row, swipe-to-
 * remove and drag-to-reorder rows) on top of [LocalPlaylistDetailViewModel], a direct projection
 * of two Room flows — every mutation is reflected instantly and no refresh gesture is offered.
 * Long-pressed rows open the shared [InPlaylistMediaItemMenu] with move/remove entries; the
 * header's ⋮ opens the screen overflow (rename / delete).
 *
 * @param id Room `PlaylistEntity.id` of the device playlist.
 * @param onNavigate navigates to a deeper route (`artist/{id}`, `album/{id}`, `player/stats`).
 * @param onBack pops back to the previous destination.
 * @param modifier sizing modifier.
 */
@Composable
fun LocalPlaylistDetailScreen(
    id: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LocalPlaylistDetailViewModel =
        viewModel(factory = LocalPlaylistDetailViewModel.factory(id))
    val state by viewModel.state.collectAsState()

    var menuTarget by remember { mutableStateOf<PlaylistMenuTarget?>(null) }
    var overflowOpen by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val playlist = state.playlist
    if (state.notFound) {
        // The playlist was deleted (or the id is unknown): an honest empty state, no header.
        DetailScaffold(onBack = onBack, modifier = modifier) {
            DetailEmptyState(text = "This playlist no longer exists")
        }
        return
    }
    if (playlist == null) {
        // First frame of the Room flow: keep the shell until the row arrives.
        DetailScaffold(onBack = onBack, modifier = modifier) {}
        return
    }

    val openMenu: (MediaMetadata, Int) -> Unit = { song, index -> menuTarget = PlaylistMenuTarget(song, index) }

    Box(modifier = modifier.fillMaxSize()) {
        // Local data is always fresh — pull-to-refresh is intentionally disabled.
        DetailScaffold(onBack = onBack) {
            LocalPlaylistScreen(
                playlist = playlist,
                songs = state.songs,
                onMenu = { overflowOpen = true },
                onSongLongClick = { song ->
                    openMenu(song, state.songs.indexOfFirst { it.id == song.id })
                },
                onSongMenu = { song ->
                    openMenu(song, state.songs.indexOfFirst { it.id == song.id })
                },
                onRemoveSong = { index -> viewModel.removeSongAt(index) },
                onMoveSong = { from, to -> viewModel.moveSong(from, to) },
            )
        }

        DetailOverflowMenu(
            expanded = overflowOpen,
            onDismissRequest = { overflowOpen = false },
            items = listOf(
                DetailOverflowItem(label = "Rename", onClick = { showRename = true }),
                DetailOverflowItem(label = "Delete", destructive = true, onClick = { showDelete = true }),
            ),
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }

    menuTarget?.let { target ->
        InPlaylistMediaItemMenu(
            item = target.song,
            playlistId = id,
            onDismiss = { menuTarget = null },
            songIndex = target.index.takeIf { it >= 0 },
            songCount = state.songs.size,
            onRemoveFromPlaylist = { viewModel.removeSongAt(target.index) },
            onMoveUp = { viewModel.moveSong(target.index, target.index - 1) },
            onMoveDown = { viewModel.moveSong(target.index, target.index + 1) },
            onArtistClick = { artist -> artist.id?.let { artistId -> onNavigate(Routes.artist(artistId)) } },
            onAlbumClick = { album -> onNavigate(Routes.album(album.id)) },
            onShowStats = { onNavigate(Routes.PLAYER_STATS) },
        )
    }

    if (showRename) {
        TextFieldDialog(
            title = "Rename playlist",
            value = playlist.name,
            onDismiss = { showRename = false },
            onConfirm = { name ->
                viewModel.rename(name)
                showRename = false
            },
        )
    }

    if (showDelete) {
        ConfirmationDialog(
            title = "Delete playlist?",
            text = "“${playlist.name}” will be removed. The songs themselves stay in your library.",
            onConfirm = {
                showDelete = false
                viewModel.delete()
                onBack()
            },
            onDismiss = { showDelete = false },
            confirmText = "Delete",
        )
    }
}
