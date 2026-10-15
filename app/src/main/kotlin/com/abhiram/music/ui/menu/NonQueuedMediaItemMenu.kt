package com.abhiram.music.ui.menu

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.abhiram.music.model.MediaMetadata

/**
 * Media-item menu for a song that is *not* in the playback queue (owner CMP-B) — e.g. an
 * album/playlist/library listing row.
 *
 * Starts with the two queue-entry actions — play next and add to queue (both cold-start the
 * playback service through [com.abhiram.music.playback.PlayerControllerProvider] when needed) —
 * followed by the shared action set: song radio, like/unlike, add to playlist (in-sheet picker),
 * view artist, view album, share and show stats.
 *
 * @param item The track the menu describes.
 * @param onDismiss Invoked when the sheet should close.
 * @param modifier Modifier applied to the sheet.
 * @param onArtistClick Receives the tapped [MediaMetadata.Artist] for navigation.
 * @param onAlbumClick Receives the album for navigation.
 * @param onNavigate Extra callback fired after any navigation so hosts can collapse overlays.
 * @param onSongRadio Starts a "song radio" queue seeded from this item.
 * @param onShowStats Opens the playback-stats screen for this item.
 */
@Composable
fun NonQueuedMediaItemMenu(
    item: MediaMetadata,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onArtistClick: ((MediaMetadata.Artist) -> Unit)? = null,
    onAlbumClick: ((MediaMetadata.Album) -> Unit)? = null,
    onNavigate: (() -> Unit)? = null,
    onSongRadio: (() -> Unit)? = null,
    onShowStats: (() -> Unit)? = null,
) {
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
            label = "nonQueuedMenuPane",
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
                    showQueueActions = true,
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
                    onNavigate = onNavigate,
                    onSongRadio = onSongRadio,
                    onShowStats = onShowStats,
                )
            }
        }
    }
}
