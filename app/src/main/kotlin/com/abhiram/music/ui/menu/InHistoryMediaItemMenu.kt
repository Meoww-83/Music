package com.abhiram.music.ui.menu

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.playback.PlayerControllerProvider

/**
 * Media-item menu for a song shown in the listening history (owner CMP-B).
 *
 * Offers: play (cold-starts the playback service through
 * [com.abhiram.music.playback.PlayerControllerProvider]), play next, add to queue, song radio,
 * like/unlike (through the repository), add to playlist (in-sheet picker), view artist, view
 * album, share and a destructive "Remove from history".
 *
 * The frozen `DatabaseDao` contract (CONTRACT §3.3) has no event-deletion method, so removal is
 * a required callback: the host screen owns the `EventWithSong` rows and wires
 * [onRemoveFromHistory] to the matching DAO delete for the song's play events.
 *
 * @param item The history track the menu describes.
 * @param onDismiss Invoked when the sheet should close.
 * @param onRemoveFromHistory Deletes this song's play events (host wires it to the DAO).
 * @param modifier Modifier applied to the sheet.
 * @param onArtistClick Receives the tapped [MediaMetadata.Artist] for navigation.
 * @param onAlbumClick Receives the album for navigation.
 * @param onNavigate Extra callback fired after any navigation so hosts can collapse overlays.
 * @param onSongRadio Starts a "song radio" queue seeded from this item.
 * @param onShowStats Opens the playback-stats screen for this item.
 */
@Composable
fun InHistoryMediaItemMenu(
    item: MediaMetadata,
    onDismiss: () -> Unit,
    onRemoveFromHistory: () -> Unit,
    modifier: Modifier = Modifier,
    onArtistClick: ((MediaMetadata.Artist) -> Unit)? = null,
    onAlbumClick: ((MediaMetadata.Album) -> Unit)? = null,
    onNavigate: (() -> Unit)? = null,
    onSongRadio: (() -> Unit)? = null,
    onShowStats: (() -> Unit)? = null,
) {
    val context = LocalContext.current
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
            label = "inHistoryMenuPane",
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
                            title = "Play",
                            icon = Icons.Rounded.PlayArrow,
                            subtitle = "Replaces the current queue",
                        ) {
                            onDismiss()
                            playHistoryItem(context, item)
                        }
                    },
                    trailing = {
                        MenuEntry(
                            title = "Remove from history",
                            icon = Icons.Rounded.Delete,
                            destructive = true,
                        ) {
                            onDismiss()
                            onRemoveFromHistory()
                        }
                    },
                )
            }
        }
    }
}

/** Plays [item] on its own, cold-starting the playback service when it is not running yet. */
private fun playHistoryItem(
    context: Context,
    item: MediaMetadata,
) {
    PlayerControllerProvider.playAll(context, listOf(item), startIndex = 0, shuffle = false)
}
