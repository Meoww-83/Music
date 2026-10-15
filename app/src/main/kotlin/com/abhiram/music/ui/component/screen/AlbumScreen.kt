package com.abhiram.music.ui.component.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhiram.music.database.entities.Album as LocalAlbum
import com.abhiram.innertube.models.AlbumItem as InnertubeAlbumItem
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.component.ShimmerHost
import com.abhiram.music.ui.component.TextPlaceholder
import com.abhiram.music.ui.component.items.ListItemPlaceholder
import com.abhiram.music.ui.theme.expressiveShapes

/**
 * Album detail body: big cover, title, clickable artist line, a "year · songs · duration" stats
 * line and a unified play/shuffle/add-to-library/menu action row above the track list.
 *
 * The component is a full-height scrolling list meant to sit inside the caller's
 * `Scaffold`/`HomeScaffold` (the caller owns the top bar, insets and navigation). Playback goes
 * through `PlayerControllerProvider` unless [onPlayAll] is supplied; long-press/menu callbacks
 * are wired by the detail screens (CMP-B owns the menus).
 *
 * @param album online album from InnerTube.
 * @param songs the album's tracks in track order (already mapped to [MediaMetadata]).
 * @param onPlayAll play-all hook; `null` starts playback through `PlayerControllerProvider`.
 * @param header extra sections rendered between the cover header and the song list.
 * @param content replaces the built-in song list when non-`null` (pass `songs = emptyList()`).
 */
@Composable
fun AlbumScreen(
    album: InnertubeAlbumItem,
    songs: List<MediaMetadata>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    onPlayAll: ((List<MediaMetadata>, Int, Boolean) -> Unit)? = null,
    onAddToLibrary: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    onArtistClick: ((MediaMetadata.Artist) -> Unit)? = null,
    onSongClick: ((Int) -> Unit)? = null,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
    onSongMenu: ((MediaMetadata) -> Unit)? = null,
    header: @Composable ColumnScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    AlbumScreenLayout(
        title = album.title,
        thumbnailUrl = album.thumbnail,
        artists = album.artists.orEmpty().map { MediaMetadata.Artist(id = it.id, name = it.name) },
        extraMeta = album.year?.toString(),
        songs = songs,
        modifier = modifier,
        isLoading = isLoading,
        error = error,
        onRetry = onRetry,
        onPlayAll = onPlayAll,
        onAddToLibrary = onAddToLibrary,
        onMenu = onMenu,
        onArtistClick = onArtistClick,
        onSongClick = onSongClick,
        onSongLongClick = onSongLongClick,
        onSongMenu = onSongMenu,
        header = header,
        content = content,
    )
}

/**
 * Album detail body for a locally stored album (Room). Same layout as the online variant; the
 * artist line is derived from the album's songs because the local `Album` entity does not carry
 * its own artist relation.
 *
 * @param album local album with its cached songs (use the dedicated [songs] list to override).
 * @param onPlayAll play-all hook; `null` starts playback through `PlayerControllerProvider`.
 */
@Composable
fun AlbumScreen(
    album: LocalAlbum,
    songs: List<MediaMetadata>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    onPlayAll: ((List<MediaMetadata>, Int, Boolean) -> Unit)? = null,
    onAddToLibrary: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    onArtistClick: ((MediaMetadata.Artist) -> Unit)? = null,
    onSongClick: ((Int) -> Unit)? = null,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
    onSongMenu: ((MediaMetadata) -> Unit)? = null,
    header: @Composable ColumnScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    AlbumScreenLayout(
        title = album.title,
        thumbnailUrl = album.thumbnailUrl,
        artists = songs.firstOrNull()?.artists.orEmpty(),
        extraMeta = album.year?.toString() ?: album.songCountText,
        songs = songs,
        modifier = modifier,
        isLoading = isLoading,
        error = error,
        onRetry = onRetry,
        onPlayAll = onPlayAll,
        onAddToLibrary = onAddToLibrary,
        onMenu = onMenu,
        onArtistClick = onArtistClick,
        onSongClick = onSongClick,
        onSongLongClick = onSongLongClick,
        onSongMenu = onSongMenu,
        header = header,
        content = content,
    )
}

/** Shared layout of both [AlbumScreen] overloads. */
@Composable
private fun AlbumScreenLayout(
    title: String,
    thumbnailUrl: String?,
    artists: List<MediaMetadata.Artist>,
    extraMeta: String?,
    songs: List<MediaMetadata>,
    modifier: Modifier,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onPlayAll: ((List<MediaMetadata>, Int, Boolean) -> Unit)?,
    onAddToLibrary: (() -> Unit)?,
    onMenu: (() -> Unit)?,
    onArtistClick: ((MediaMetadata.Artist) -> Unit)?,
    onSongClick: ((Int) -> Unit)?,
    onSongLongClick: ((MediaMetadata) -> Unit)?,
    onSongMenu: ((MediaMetadata) -> Unit)?,
    header: @Composable ColumnScope.() -> Unit,
    content: (@Composable ColumnScope.() -> Unit)?,
) {
    val context = LocalContext.current
    val playback = rememberScreenPlayback()
    val handleClick = songClickHandler(songs, onSongClick)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = rememberLazyListState(),
    ) {
        item(key = "album_header") {
            ScreenCoverHeader(
                title = title,
                artwork = {
                    Surface(
                        shape = MaterialTheme.expressiveShapes.squircle,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shadowElevation = 12.dp,
                        modifier = Modifier.size(220.dp),
                    ) {
                        ArtworkImage(
                            url = thumbnailUrl,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                },
                subtitle = if (artists.isNotEmpty()) {
                    { AlbumArtistLine(artists = artists, onArtistClick = onArtistClick) }
                } else {
                    null
                },
                meta = songStats(songs, extra = extraMeta),
                actions = {
                    ScreenActionRow(
                        onPlay = { playAllOrDefault(context, songs, onPlayAll) },
                        onShuffle = { playAllOrDefault(context, songs, onPlayAll, shuffle = true) },
                        onAddToLibrary = onAddToLibrary,
                        onMenu = onMenu,
                    )
                },
            )
        }

        item(key = "album_extra_header") {
            Column(Modifier.fillMaxWidth()) { header() }
        }

        when {
            isLoading && songs.isEmpty() ->
                item(key = "album_loading") {
                    ShimmerHost(Modifier.fillMaxWidth()) {
                        repeat(6) { ListItemPlaceholder(Modifier.fillMaxWidth()) }
                    }
                }

            error != null && songs.isEmpty() ->
                item(key = "album_error") {
                    ScreenError(onRetry = onRetry, message = error)
                }

            songs.isEmpty() ->
                item(key = "album_empty") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextPlaceholder(text = "No songs in this album yet")
                    }
                }

            else ->
                screenSongItems(
                    songs = songs,
                    playback = playback,
                    keyPrefix = "album",
                    onSongClick = handleClick,
                    onSongLongClick = onSongLongClick,
                    onSongMenu = onSongMenu,
                )
        }

        if (content != null) {
            item(key = "album_content") {
                Column(Modifier.fillMaxWidth()) { content() }
            }
        }

        if (error != null && songs.isNotEmpty()) {
            item(key = "album_error_footer") {
                ScreenError(onRetry = onRetry, message = error, compact = true)
            }
        }
    }
}

/**
 * Album track list without any header: numbered rows (the number is replaced by animated music
 * bars for the playing track), duration on the trailing side, optional drag-to-reorder handles.
 *
 * @param onSongClick row click; `null` plays that song (and the rest of the list) through the
 * frozen `PlayerControllerProvider`.
 * @param onMove when provided, every row gets a drag handle that reorders through this callback.
 */
@Composable
fun AlbumSongs(
    songs: List<MediaMetadata>,
    onSongClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    showIndex: Boolean = true,
    showDuration: Boolean = true,
    onMove: ((Int, Int) -> Unit)? = null,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
    onSongMenu: ((MediaMetadata) -> Unit)? = null,
    emptyText: String = "No songs yet",
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
            item(key = "album_songs_empty") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TextPlaceholder(text = emptyText)
                }
            }
        } else {
            screenSongItems(
                songs = songs,
                playback = playback,
                keyPrefix = "album_list",
                onSongClick = handleClick,
                showIndex = showIndex,
                showDuration = showDuration,
                onSongLongClick = onSongLongClick,
                onSongMenu = onSongMenu,
                reorder = if (onMove != null) reorderState else null,
            )
        }
    }
}

/** Clickable, comma separated artist line used under the album title. */
@Composable
private fun AlbumArtistLine(
    artists: List<MediaMetadata.Artist>,
    onArtistClick: ((MediaMetadata.Artist) -> Unit)?,
) {
    CenteredSubtitleLine {
        artists.forEachIndexed { index, artist ->
            val handler = onArtistClick
            val text = if (index == 0) artist.name else ", ${artist.name}"
            if (handler != null && artist.id != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clickable { handler(artist) }
                        .padding(vertical = 4.dp),
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}
