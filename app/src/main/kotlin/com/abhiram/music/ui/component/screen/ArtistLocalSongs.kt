package com.abhiram.music.ui.component.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.component.TextPlaceholder

/**
 * List of the locally stored songs of an artist (Room cache), without any screen header.
 *
 * Rows are built on CMP-C's [com.abhiram.music.ui.component.items.MediaSongItem]: artist/album
 * line, duration on the trailing side and animated music bars while the row is playing. For
 * in-screen lists prefer `ArtistScreen(songs = ...)` (it renders the same rows inside the
 * screen's own lazy column); this composable is the standalone variant for tab bodies that own
 * their scrolling.
 *
 * @param songs the artist's songs (already mapped to [MediaMetadata]).
 * @param onSongClick row click; `null` plays that song (and the rest of the list) through the
 * frozen `PlayerControllerProvider`.
 */
@Composable
fun ArtistLocalSongs(
    songs: List<MediaMetadata>,
    onSongClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    showIndex: Boolean = false,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
    onSongMenu: ((MediaMetadata) -> Unit)? = null,
    emptyText: String = "No songs stored on this device",
) {
    val playback = rememberScreenPlayback()
    val handleClick = songClickHandler(songs, onSongClick)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = rememberLazyListState(),
    ) {
        if (songs.isEmpty()) {
            item(key = "artist_local_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TextPlaceholder(text = emptyText)
                }
            }
        } else {
            screenSongItems(
                songs = songs,
                playback = playback,
                keyPrefix = "artist_local",
                onSongClick = handleClick,
                showIndex = showIndex,
                onSongLongClick = onSongLongClick,
                onSongMenu = onSongMenu,
            )
        }
    }
}
