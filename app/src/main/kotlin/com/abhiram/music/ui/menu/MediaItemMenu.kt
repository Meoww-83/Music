package com.abhiram.music.ui.menu

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.model.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Generic media-item menu for a [MediaMetadata] that is not bound to any particular surface
 * (owner CMP-B).
 *
 * Offers the full shared action set — play next, add to queue, song radio, like/unlike, add to
 * playlist (in-sheet picker over the local playlists plus "New playlist…"), view artist, view
 * album, share and show stats — plus optional removal entries. All navigation is delegated to
 * the callbacks so this package never touches a `NavController`.
 *
 * @param item The track the menu describes.
 * @param onDismiss Invoked when the sheet should close.
 * @param modifier Modifier applied to the sheet.
 * @param onArtistClick Receives the tapped [MediaMetadata.Artist] for navigation.
 * @param onAlbumClick Receives the album for navigation.
 * @param onNavigate Extra callback fired after any navigation so hosts can collapse overlays.
 * @param onRemoveFromPlaylist When present, shows a destructive "Remove from playlist" entry.
 * @param onSongRadio Starts a "song radio" queue seeded from this item.
 * @param onShowStats Opens the playback-stats screen for this item.
 */
@Composable
fun MediaItemMenu(
    item: MediaMetadata,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onArtistClick: ((MediaMetadata.Artist) -> Unit)? = null,
    onAlbumClick: ((MediaMetadata.Album) -> Unit)? = null,
    onNavigate: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onSongRadio: (() -> Unit)? = null,
    onShowStats: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var showPlaylists by rememberSaveable { mutableStateOf(false) }

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
            label = "mediaItemMenuPane",
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
                    trailing = {
                        onRemoveFromPlaylist?.let { remove ->
                            MenuEntry(
                                title = "Remove from playlist",
                                icon = Icons.Rounded.Delete,
                                destructive = true,
                            ) {
                                onDismiss()
                                remove()
                            }
                        }
                        if (item.inLibrary != null) {
                            MenuEntry(
                                title = "Remove from library",
                                icon = Icons.Rounded.DeleteOutline,
                                destructive = true,
                            ) {
                                onDismiss()
                                scope.launch(Dispatchers.IO) {
                                    ServiceLocator.container.dao.removeSongFromLibrary(item.id)
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}
