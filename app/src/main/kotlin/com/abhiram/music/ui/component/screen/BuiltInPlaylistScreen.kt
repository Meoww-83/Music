package com.abhiram.music.ui.component.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.abhiram.music.model.BuiltInPlaylist
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.component.ShimmerHost
import com.abhiram.music.ui.component.TextPlaceholder
import com.abhiram.music.ui.component.items.ListItemPlaceholder
import com.abhiram.music.ui.theme.expressiveShapes

/**
 * Detail body for one of the [BuiltInPlaylist] system lists (Favorites, Offline, Downloaded,
 * History, Top played, Cache): a distinctive icon tile, the list's title, a
 * "songs · duration" stats line and the unified play/shuffle/menu action row above the tracks.
 *
 * The component is a full-height scrolling list meant to sit inside the caller's `Scaffold`; the
 * caller owns the top bar, insets and navigation.
 *
 * @param builtIn which built-in list is shown (also decides the thumbnail tile).
 * @param songs the list's tracks (already mapped to [MediaMetadata]).
 * @param onPlayAll play-all hook; `null` starts playback through `PlayerControllerProvider`.
 * @param header extra sections rendered between the cover header and the song list.
 * @param content replaces the built-in song list when non-`null` (pass `songs = emptyList()`).
 */
@Composable
fun BuiltInPlaylistScreen(
    builtIn: BuiltInPlaylist,
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
        item(key = "built_in_header") {
            ScreenCoverHeader(
                title = builtIn.title,
                artwork = {
                    BuiltInPlaylistThumbnail(
                        builtIn = builtIn,
                        modifier = Modifier.size(170.dp),
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

        item(key = "built_in_extra_header") {
            Column(Modifier.fillMaxWidth()) { header() }
        }

        when {
            isLoading && songs.isEmpty() ->
                item(key = "built_in_loading") {
                    ShimmerHost(Modifier.fillMaxWidth()) {
                        repeat(6) { ListItemPlaceholder(Modifier.fillMaxWidth()) }
                    }
                }

            error != null && songs.isEmpty() ->
                item(key = "built_in_error") {
                    ScreenError(onRetry = onRetry, message = error)
                }

            songs.isEmpty() ->
                item(key = "built_in_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextPlaceholder(text = "Nothing here yet")
                    }
                }

            else ->
                screenSongItems(
                    songs = songs,
                    playback = playback,
                    keyPrefix = "built_in",
                    onSongClick = handleClick,
                    onSongLongClick = onSongLongClick,
                    onSongMenu = onSongMenu,
                )
        }

        if (content != null) {
            item(key = "built_in_content") {
                Column(Modifier.fillMaxWidth()) { content() }
            }
        }

        if (error != null && songs.isNotEmpty()) {
            item(key = "built_in_error_footer") {
                ScreenError(onRetry = onRetry, message = error, compact = true)
            }
        }
    }
}

/**
 * Track list of a built-in playlist without any header: animated music bars while a row plays,
 * duration on the trailing side and the row menu slot.
 *
 * @param onSongClick row click; `null` plays that song (and the rest of the list) through the
 * frozen `PlayerControllerProvider`.
 */
@Composable
fun BuiltInPlaylistSongs(
    songs: List<MediaMetadata>,
    onSongClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    showIndex: Boolean = true,
    showDuration: Boolean = true,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
    onSongMenu: ((MediaMetadata) -> Unit)? = null,
    emptyText: String = "Nothing here yet",
) {
    val playback = rememberScreenPlayback()
    val handleClick = songClickHandler(songs, onSongClick)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = rememberLazyListState(),
    ) {
        if (songs.isEmpty()) {
            item(key = "built_in_list_empty") {
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
                keyPrefix = "built_in_list",
                onSongClick = handleClick,
                showIndex = showIndex,
                showDuration = showDuration,
                onSongLongClick = onSongLongClick,
                onSongMenu = onSongMenu,
            )
        }
    }
}

/**
 * Distinctive artwork tile for a [BuiltInPlaylist]: every value gets its own tonal colour, an
 * expressive shape (cookie / oval / squircle / bun / gem / pill) and its matching glyph, so the
 * system lists stay recognisable at any size — as a 48 dp library row icon or the 170 dp cover
 * used by [BuiltInPlaylistScreen].
 *
 * @param shape override for the tile shape; `null` picks the value's own expressive shape.
 */
@Composable
fun BuiltInPlaylistThumbnail(
    builtIn: BuiltInPlaylist,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
) {
    val (container: Color, content: Color) = when (builtIn) {
        BuiltInPlaylist.FAVORITES ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer

        BuiltInPlaylist.OFFLINE ->
            MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant

        BuiltInPlaylist.DOWNLOADED ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer

        BuiltInPlaylist.HISTORY ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer

        BuiltInPlaylist.TOP ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer

        BuiltInPlaylist.CACHE ->
            MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val resolvedShape = shape ?: when (builtIn) {
        BuiltInPlaylist.FAVORITES -> MaterialTheme.expressiveShapes.cookie
        BuiltInPlaylist.OFFLINE -> MaterialTheme.expressiveShapes.oval
        BuiltInPlaylist.DOWNLOADED -> MaterialTheme.expressiveShapes.squircle
        BuiltInPlaylist.HISTORY -> MaterialTheme.expressiveShapes.bun
        BuiltInPlaylist.TOP -> MaterialTheme.expressiveShapes.gem
        BuiltInPlaylist.CACHE -> MaterialTheme.expressiveShapes.pill
    }

    Surface(
        shape = resolvedShape,
        color = container,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Soft halo behind the glyph for a little extra depth.
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .aspectRatio(1f)
                    .background(content.copy(alpha = 0.14f), CircleShape),
            )
            Icon(
                imageVector = builtIn.icon(),
                contentDescription = builtIn.title,
                tint = content,
                modifier = Modifier
                    .fillMaxWidth(0.40f)
                    .aspectRatio(1f),
            )
        }
    }
}

/** The glyph shown inside a built-in playlist tile. */
private fun BuiltInPlaylist.icon(): ImageVector =
    when (this) {
        BuiltInPlaylist.FAVORITES -> Icons.Rounded.Favorite
        BuiltInPlaylist.OFFLINE -> Icons.Rounded.CloudOff
        BuiltInPlaylist.DOWNLOADED -> Icons.Rounded.Download
        BuiltInPlaylist.HISTORY -> Icons.Rounded.History
        BuiltInPlaylist.TOP -> Icons.Rounded.TrendingUp
        BuiltInPlaylist.CACHE -> Icons.Rounded.Storage
    }
