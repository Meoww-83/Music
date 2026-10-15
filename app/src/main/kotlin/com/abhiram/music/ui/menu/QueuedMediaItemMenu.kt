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
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.playback.PlayerControllerProvider
import com.abhiram.music.ui.component.ConfirmationDialog

/**
 * Media-item menu for a song that is inside the playback queue (owner CMP-B).
 *
 * Adds queue-management entries — play next, move up, move down, remove from queue (all driven
 * through the frozen [com.abhiram.music.playback.PlayerController]) and a confirmed
 * "Clear queue" — on top of the shared action set (like, add to playlist, song radio, view
 * artist/album, share, show stats). The currently playing entry never exposes move/remove.
 * The playback service is already running whenever a queue exists, so the live controller is
 * used directly (falls back to disabled entries if it somehow is gone).
 *
 * @param item The queued track the menu describes.
 * @param queueIndex Index of [item] inside `PlayerState.queue`.
 * @param onDismiss Invoked when the sheet should close.
 * @param modifier Modifier applied to the sheet.
 * @param isCurrentItem True when [item] is the currently playing entry.
 * @param onArtistClick Receives the tapped [MediaMetadata.Artist] for navigation.
 * @param onAlbumClick Receives the album for navigation.
 * @param onNavigate Extra callback fired after any navigation so hosts can collapse overlays.
 * @param onSongRadio Starts a "song radio" queue seeded from this item.
 * @param onShowStats Opens the playback-stats screen for this item.
 */
@Composable
fun QueuedMediaItemMenu(
    item: MediaMetadata,
    queueIndex: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrentItem: Boolean = false,
    onArtistClick: ((MediaMetadata.Artist) -> Unit)? = null,
    onAlbumClick: ((MediaMetadata.Album) -> Unit)? = null,
    onNavigate: (() -> Unit)? = null,
    onSongRadio: (() -> Unit)? = null,
    onShowStats: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val controller = rememberMenuPlayerController()
    val playerState by queueStateFlow(controller).collectAsState()
    var showPlaylists by rememberSaveable { mutableStateOf(false) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    if (showClearDialog) {
        ConfirmationDialog(
            title = "Clear queue?",
            text = "Every song after the current one will be removed.",
            onConfirm = {
                showClearDialog = false
                controller?.clearQueue()
                onDismiss()
            },
            onDismiss = { showClearDialog = false },
        )
    }

    val queueEditable = controller != null && !isCurrentItem

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
            label = "queuedMenuPane",
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
                    showQueueActions = false,
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
                    onNavigate = onNavigate,
                    onSongRadio = onSongRadio,
                    onShowStats = onShowStats,
                    leading = {
                        MenuEntry(
                            title = "Play next",
                            icon = Icons.Rounded.PlaylistPlay,
                            subtitle = "Moves this song after the current one",
                        ) {
                            onDismiss()
                            PlayerControllerProvider.with(context) { it.playNext(item) }
                        }
                        MenuEntry(
                            title = "Move up",
                            icon = Icons.Rounded.ArrowUpward,
                            enabled = queueEditable && queueIndex > 0,
                        ) {
                            onDismiss()
                            controller?.moveQueueItem(queueIndex, queueIndex - 1)
                        }
                        MenuEntry(
                            title = "Move down",
                            icon = Icons.Rounded.ArrowDownward,
                            enabled = queueEditable && queueIndex < playerState.queue.lastIndex,
                        ) {
                            onDismiss()
                            controller?.moveQueueItem(queueIndex, queueIndex + 1)
                        }
                    },
                    trailing = {
                        MenuEntry(
                            title = "Remove from queue",
                            icon = Icons.Rounded.Delete,
                            enabled = queueEditable,
                            destructive = true,
                        ) {
                            onDismiss()
                            controller?.removeQueueItem(queueIndex)
                        }
                        MenuEntry(
                            title = "Clear queue",
                            icon = Icons.Rounded.ClearAll,
                            destructive = true,
                        ) {
                            showClearDialog = true
                        }
                    },
                )
            }
        }
    }
}
