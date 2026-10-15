package com.abhiram.music.ui.component.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhiram.innertube.models.Artist as InnertubeArtist
import com.abhiram.innertube.models.PlaylistItem as InnertubePlaylistItem
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.component.PlaylistThumbnail
import com.abhiram.music.ui.component.ShimmerHost
import com.abhiram.music.ui.component.TextPlaceholder
import com.abhiram.music.ui.component.items.ListItemPlaceholder
import com.abhiram.music.utils.resize

/**
 * Detail body for an online (InnerTube) playlist: montage thumbnail, title, a clickable author
 * line with the song count, the description and the unified play/shuffle/menu action row above
 * the tracks.
 *
 * The component is a full-height scrolling list inside the caller's `Scaffold`; the caller owns
 * the top bar, insets and navigation.
 *
 * @param playlist the online playlist.
 * @param songs the playlist's tracks in order (already mapped to [MediaMetadata]).
 * @param onPlayAll play-all hook; `null` starts playback through `PlayerControllerProvider`.
 * @param header extra sections rendered between the cover header and the song list (the built-in
 * description is rendered here).
 * @param content replaces the built-in song list when non-`null` (pass `songs = emptyList()`).
 */
@Composable
fun PlaylistScreen(
    playlist: InnertubePlaylistItem,
    songs: List<MediaMetadata>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    onPlayAll: ((List<MediaMetadata>, Int, Boolean) -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    onAuthorClick: ((InnertubeArtist) -> Unit)? = null,
    onSongClick: ((Int) -> Unit)? = null,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
    onSongMenu: ((MediaMetadata) -> Unit)? = null,
    header: @Composable ColumnScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val playback = rememberScreenPlayback()
    val handleClick = songClickHandler(songs, onSongClick)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = rememberLazyListState(),
    ) {
        item(key = "playlist_header") {
            ScreenCoverHeader(
                title = playlist.title,
                artwork = {
                    PlaylistThumbnail(
                        thumbnails = listOfNotNull(playlist.thumbnail?.resize(1080, 1080)),
                        modifier = Modifier.size(170.dp),
                        songCount = songs.size.takeIf { it > 0 },
                    )
                },
                subtitle = {
                    PlaylistAuthorLine(
                        playlist = playlist,
                        onAuthorClick = onAuthorClick,
                    )
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

        item(key = "playlist_extra_header") {
            Column(Modifier.fillMaxWidth()) {
                playlist.description
                    ?.takeIf { it.isNotBlank() }
                    ?.let { description ->
                        ExpandableText(
                            text = description,
                            collapsedMaxLines = 4,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        )
                    }
                header()
            }
        }

        when {
            isLoading && songs.isEmpty() ->
                item(key = "playlist_loading") {
                    ShimmerHost(Modifier.fillMaxWidth()) {
                        repeat(6) { ListItemPlaceholder(Modifier.fillMaxWidth()) }
                    }
                }

            error != null && songs.isEmpty() ->
                item(key = "playlist_error") {
                    ScreenError(onRetry = onRetry, message = error)
                }

            songs.isEmpty() ->
                item(key = "playlist_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextPlaceholder(text = "This playlist has no songs yet")
                    }
                }

            else ->
                screenSongItems(
                    songs = songs,
                    playback = playback,
                    keyPrefix = "playlist",
                    onSongClick = handleClick,
                    onSongLongClick = onSongLongClick,
                    onSongMenu = onSongMenu,
                )
        }

        if (content != null) {
            item(key = "playlist_content") {
                Column(Modifier.fillMaxWidth()) { content() }
            }
        }

        if (error != null && songs.isNotEmpty()) {
            item(key = "playlist_error_footer") {
                ScreenError(onRetry = onRetry, message = error, compact = true)
            }
        }
    }
}

/**
 * Online playlist track list without any header. Unlike the local playlist rows these can show
 * an extra per-row line (author, "added on" date, ...) through [subtitle]; rows keep animated
 * music bars while playing, the duration on the trailing side and the row menu slot.
 *
 * @param onSongClick row click; `null` plays that song (and the rest of the list) through the
 * frozen `PlayerControllerProvider`.
 * @param subtitle optional per-row line (e.g. author or publish date); `null` falls back to the
 * song's artists.
 */
@Composable
fun PlaylistSongList(
    songs: List<MediaMetadata>,
    onSongClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: ((index: Int, song: MediaMetadata) -> String)? = null,
    showIndex: Boolean = false,
    showDuration: Boolean = true,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
    onSongMenu: ((MediaMetadata) -> Unit)? = null,
    emptyText: String = "This playlist has no songs yet",
) {
    val playback = rememberScreenPlayback()
    val handleClick = songClickHandler(songs, onSongClick)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = rememberLazyListState(),
    ) {
        if (songs.isEmpty()) {
            item(key = "playlist_list_empty") {
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
                keyPrefix = "playlist_list",
                onSongClick = handleClick,
                showIndex = showIndex,
                showDuration = showDuration,
                onSongLongClick = onSongLongClick,
                onSongMenu = onSongMenu,
                subtitleFor = subtitle,
            )
        }
    }
}

/** "Author · 120 songs" line under the online playlist title. */
@Composable
private fun PlaylistAuthorLine(
    playlist: InnertubePlaylistItem,
    onAuthorClick: ((InnertubeArtist) -> Unit)?,
) {
    CenteredSubtitleLine {
        val author = playlist.author
        val handler = onAuthorClick
        if (author != null) {
            if (handler != null) {
                Text(
                    text = author.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clickable { handler(author) }
                        .padding(vertical = 4.dp),
                )
            } else {
                Text(
                    text = author.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
        val countText = playlist.songCountText
        if (author != null && countText != null) {
            Text(
                text = "  ·  ",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (countText != null) {
            Text(
                text = countText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}
