package com.abhiram.music.ui.component.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.abhiram.music.database.entities.Playlist as LocalPlaylist
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.component.PlaylistThumbnail
import com.abhiram.music.ui.component.ShimmerHost
import com.abhiram.music.ui.component.TextPlaceholder
import com.abhiram.music.ui.component.items.ListItemPlaceholder

/**
 * Detail body for a user's local (Room) playlist: a montage thumbnail built from the songs'
 * artwork, the playlist name, its description, a "songs · duration" stats line and the unified
 * play/shuffle/menu action row above the tracks.
 *
 * The component is a full-height scrolling list inside the caller's `Scaffold`. Rows support the
 * two playlist-specific gestures: [onRemoveSong] wraps each row in a swipe-to-remove box and
 * [onMoveSong] gives each row a drag handle for reordering.
 *
 * @param playlist local playlist with its cached songs (pass the rows via [songs]).
 * @param songs the playlist's tracks in playlist order (already mapped to [MediaMetadata]).
 * @param onPlayAll play-all hook; `null` starts playback through `PlayerControllerProvider`.
 * @param header extra sections rendered between the cover header and the song list.
 * @param content replaces the built-in song list when non-`null` (pass `songs = emptyList()`).
 */
@Composable
fun LocalPlaylistScreen(
    playlist: LocalPlaylist,
    songs: List<MediaMetadata>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    onPlayAll: ((List<MediaMetadata>, Int, Boolean) -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    onSongClick: ((Int) -> Unit)? = null,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
    onSongMenu: ((MediaMetadata) -> Unit)? = null,
    onRemoveSong: ((Int) -> Unit)? = null,
    onMoveSong: ((Int, Int) -> Unit)? = null,
    header: @Composable ColumnScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val playback = rememberScreenPlayback()
    val handleClick = songClickHandler(songs, onSongClick)
    val density = LocalDensity.current
    val currentOnMove = onMoveSong
    val reorderState = remember(density) {
        ScreenReorderState(fallbackRowHeightPx = with(density) { 72.dp.toPx() }) { from, to ->
            currentOnMove?.invoke(from, to)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = rememberLazyListState(),
    ) {
        item(key = "local_playlist_header") {
            ScreenCoverHeader(
                title = playlist.name,
                artwork = {
                    PlaylistThumbnail(
                        thumbnails = songs.mapNotNull { it.thumbnailUrl }.take(4),
                        modifier = Modifier.size(170.dp),
                        songCount = songs.size,
                    )
                },
                subtitle = playlist.description?.takeIf { it.isNotBlank() }?.let { description ->
                    { ExpandableText(text = description, collapsedMaxLines = 3) }
                },
                meta = songStats(songs),
                actions = {
                    ScreenActionRow(
                        onPlay = { playAllOrDefault(context, songs, onPlayAll) },
                        onShuffle = { playAllOrDefault(context, songs, onPlayAll, shuffle = true) },
                        onMenu = onMenu,
                    )
                },
            )
        }

        item(key = "local_playlist_extra_header") {
            Column(Modifier.fillMaxWidth()) { header() }
        }

        when {
            isLoading && songs.isEmpty() ->
                item(key = "local_playlist_loading") {
                    ShimmerHost(Modifier.fillMaxWidth()) {
                        repeat(6) { ListItemPlaceholder(Modifier.fillMaxWidth()) }
                    }
                }

            error != null && songs.isEmpty() ->
                item(key = "local_playlist_error") {
                    ScreenError(onRetry = onRetry, message = error)
                }

            songs.isEmpty() ->
                item(key = "local_playlist_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextPlaceholder(text = "This playlist is empty")
                    }
                }

            else ->
                screenSongItems(
                    songs = songs,
                    playback = playback,
                    keyPrefix = "local_playlist",
                    onSongClick = handleClick,
                    onSongLongClick = onSongLongClick,
                    onSongMenu = onSongMenu,
                    onRemove = onRemoveSong,
                    reorder = if (onMoveSong != null) reorderState else null,
                )
        }

        if (content != null) {
            item(key = "local_playlist_content") {
                Column(Modifier.fillMaxWidth()) { content() }
            }
        }

        if (error != null && songs.isNotEmpty()) {
            item(key = "local_playlist_error_footer") {
                ScreenError(onRetry = onRetry, message = error, compact = true)
            }
        }
    }
}

/**
 * Playlist track list without any header, with the two reorder/remove affordances:
 * [onRemove] wraps rows in a swipe-to-remove box and [onMove] gives rows drag handles that
 * reorder through the callback. Rows keep animated music bars while playing and the duration on
 * the trailing side.
 *
 * @param onSongClick row click; `null` plays that song (and the rest of the list) through the
 * frozen `PlayerControllerProvider`.
 */
@Composable
fun LocalPlaylistSongs(
    songs: List<MediaMetadata>,
    onSongClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onRemove: ((Int) -> Unit)? = null,
    onMove: ((Int, Int) -> Unit)? = null,
    showDuration: Boolean = true,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
    onSongMenu: ((MediaMetadata) -> Unit)? = null,
    emptyText: String = "This playlist is empty",
) {
    val playback = rememberScreenPlayback()
    val handleClick = songClickHandler(songs, onSongClick)
    val density = LocalDensity.current
    val currentOnMove = onMove
    val reorderState = remember(density) {
        ScreenReorderState(fallbackRowHeightPx = with(density) { 72.dp.toPx() }) { from, to ->
            currentOnMove?.invoke(from, to)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = rememberLazyListState(),
    ) {
        if (songs.isEmpty()) {
            item(key = "local_playlist_list_empty") {
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
                keyPrefix = "local_playlist_list",
                onSongClick = handleClick,
                showIndex = false,
                showDuration = showDuration,
                onSongLongClick = onSongLongClick,
                onSongMenu = onSongMenu,
                onRemove = onRemove,
                reorder = if (onMove != null) reorderState else null,
            )
        }
    }
}
