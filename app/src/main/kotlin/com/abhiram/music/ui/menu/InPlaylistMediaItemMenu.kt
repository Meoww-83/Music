package com.abhiram.music.ui.menu

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.model.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Media-item menu for a song shown inside a (local or remote) playlist (owner CMP-B).
 *
 * Adds playlist-management entries — move up, move down, a reorder affordance and a destructive
 * "Remove from playlist" — on top of the shared action set (play next, add to queue, song radio,
 * like, add to playlist, view artist/album, share, show stats).
 *
 * Removal defaults to
 * [com.abhiram.music.repository.MusicRepository.removeSongFromPlaylist] for local playlists;
 * hosts of remote (browseId-backed) playlists pass [onRemoveFromPlaylist] and/or
 * [onRemoveFromRemotePlaylist] so both the local cache and the YouTube playlist stay in sync
 * (the remote call is the host's — it owns the `YouTubeRepository` flow).
 *
 * @param item The playlist track the menu describes.
 * @param playlistId Room `PlaylistEntity.id` of the containing playlist.
 * @param onDismiss Invoked when the sheet should close.
 * @param modifier Modifier applied to the sheet.
 * @param onRemoveFromPlaylist Host-provided removal; `null` removes via the local repository.
 * @param songIndex 0-based position of [item] inside the playlist, `null` when unknown.
 * @param songCount Total number of songs in the playlist, `null` when unknown.
 * @param onMoveUp Host callback that reorders [item] one slot up; shows the entry when present.
 * @param onMoveDown Host callback that reorders [item] one slot down; shows the entry when present.
 * @param onReorder Host callback that opens the full reorder sheet/drag mode.
 * @param onRemoveFromRemotePlaylist Extra callback for browseId-backed playlists.
 * @param onArtistClick Receives the tapped [MediaMetadata.Artist] for navigation.
 * @param onAlbumClick Receives the album for navigation.
 * @param onNavigate Extra callback fired after any navigation so hosts can collapse overlays.
 * @param onSongRadio Starts a "song radio" queue seeded from this item.
 * @param onShowStats Opens the playback-stats screen for this item.
 */
@Composable
fun InPlaylistMediaItemMenu(
    item: MediaMetadata,
    playlistId: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    songIndex: Int? = null,
    songCount: Int? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onReorder: (() -> Unit)? = null,
    onRemoveFromRemotePlaylist: (() -> Unit)? = null,
    onArtistClick: ((MediaMetadata.Artist) -> Unit)? = null,
    onAlbumClick: ((MediaMetadata.Album) -> Unit)? = null,
    onNavigate: (() -> Unit)? = null,
    onSongRadio: (() -> Unit)? = null,
    onShowStats: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val repository = remember { ServiceLocator.container.musicRepository }
    var showPlaylists by rememberSaveable { mutableStateOf(false) }

    val canMoveUp = songIndex != null && songIndex > 0 && onMoveUp != null
    val canMoveDown = songIndex != null && songCount != null &&
        songIndex < songCount - 1 && onMoveDown != null

    BaseMediaItemMenu(
        item = item,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        AnimatedContent(
            targetState = showPlaylists,
            transitionSpec = {
                if (targetState) {
                    (slideInVertically { it / 8 } + fadeIn()) togetherWith
                        slideOutVertically { -it / 8 } + fadeOut()
                } else {
                    (slideInVertically { -it / 8 } + fadeIn()) togetherWith
                        slideOutVertically { it / 8 } + fadeOut()
                }
            },
            label = "inPlaylistMenuPane",
        ) { inPlaylists ->
            if (inPlaylists) {
                AddToPlaylistPane(
                    item = item,
                    onDismiss = onDismiss,
                    onBack = { showPlaylists = false },
                )
            } else {
                SharedMediaItemActions(
                    item = item,
                    onDismiss = onDismiss,
                    onAddToPlaylist = { showPlaylists = true },
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
                    onNavigate = onNavigate,
                    onSongRadio = onSongRadio,
                    onShowStats = onShowStats,
                    leading = {
                        MenuEntry(
                            title = "Move up",
                            icon = Icons.Rounded.ArrowUpward,
                            enabled = canMoveUp,
                        ) {
                            onDismiss()
                            onMoveUp?.invoke()
                        }
                        MenuEntry(
                            title = "Move down",
                            icon = Icons.Rounded.ArrowDownward,
                            enabled = canMoveDown,
                        ) {
                            onDismiss()
                            onMoveDown?.invoke()
                        }
                        onReorder?.let { reorder ->
                            MenuEntry(
                                title = "Reorder",
                                icon = Icons.Rounded.SwapVert,
                                subtitle = "Drag to change the order",
                            ) {
                                onDismiss()
                                reorder()
                            }
                        }
                    },
                    trailing = {
                        MenuEntry(
                            title = "Remove from playlist",
                            icon = Icons.Rounded.Delete,
                            destructive = true,
                        ) {
                            onDismiss()
                            onRemoveFromRemotePlaylist?.invoke()
                            val remove = onRemoveFromPlaylist
                            if (remove != null) {
                                remove()
                            } else {
                                scope.launch(Dispatchers.IO) {
                                    repository.removeSongFromPlaylist(playlistId, item.id)
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}
