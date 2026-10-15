package com.abhiram.music.ui.component.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhiram.innertube.models.ArtistItem as InnertubeArtistItem
import com.abhiram.music.database.entities.Artist as LocalArtist
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.component.ShimmerHost
import com.abhiram.music.ui.component.items.ListItemPlaceholder

/**
 * Artist detail body: banner image with the name docked at its foot, an optional subscriber /
 * monthly-listener line, a unified play/shuffle/menu action row, an optional pill tab strip and
 * a scrolling body assembled from [header], [content] and [songs].
 *
 * The component is a full-height list meant to sit inside the caller's `Scaffold`; the caller
 * owns the top bar, insets and navigation and decides which tab is selected (this component is
 * fully stateless). Tab bodies that are plain sections (biography, related artists, ...) belong
 * in [content]; full song lists belong in [songs] so they scroll as real lazy rows.
 *
 * @param artist online artist from InnerTube.
 * @param tabs tab labels rendered by the pill strip; empty hides the strip entirely.
 * @param songs rows rendered below [content] (e.g. the artist's top or local songs).
 * @param onPlayAll play-all hook; `null` starts playback through `PlayerControllerProvider`.
 */
@Composable
fun ArtistScreen(
    artist: InnertubeArtistItem,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    tabs: List<String> = emptyList(),
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    subtitle: String? = null,
    songs: List<MediaMetadata> = emptyList(),
    onPlayAll: ((List<MediaMetadata>, Int, Boolean) -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    onSongClick: ((Int) -> Unit)? = null,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
    onSongMenu: ((MediaMetadata) -> Unit)? = null,
    header: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    ArtistScreenLayout(
        name = artist.title,
        bannerUrl = artist.thumbnail,
        subtitle = subtitle,
        modifier = modifier,
        isLoading = isLoading,
        error = error,
        onRetry = onRetry,
        tabs = tabs,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        songs = songs,
        onPlayAll = onPlayAll,
        onMenu = onMenu,
        onSongClick = onSongClick,
        onSongLongClick = onSongLongClick,
        onSongMenu = onSongMenu,
        header = header,
        content = content,
    )
}

/**
 * Artist detail body for a locally stored artist (Room). Same layout as the online variant with
 * the subscriber count taken from the local entity.
 *
 * @param artist local artist with its cached songs (pass the rows via [songs]).
 */
@Composable
fun ArtistScreen(
    artist: LocalArtist,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    tabs: List<String> = emptyList(),
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    songs: List<MediaMetadata> = emptyList(),
    onPlayAll: ((List<MediaMetadata>, Int, Boolean) -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    onSongClick: ((Int) -> Unit)? = null,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
    onSongMenu: ((MediaMetadata) -> Unit)? = null,
    header: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    ArtistScreenLayout(
        name = artist.name,
        bannerUrl = artist.thumbnailUrl,
        subtitle = artist.subscriberCountText,
        modifier = modifier,
        isLoading = isLoading,
        error = error,
        onRetry = onRetry,
        tabs = tabs,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        songs = songs,
        onPlayAll = onPlayAll,
        onMenu = onMenu,
        onSongClick = onSongClick,
        onSongLongClick = onSongLongClick,
        onSongMenu = onSongMenu,
        header = header,
        content = content,
    )
}

/** Shared layout of both [ArtistScreen] overloads. */
@Composable
private fun ArtistScreenLayout(
    name: String,
    bannerUrl: String?,
    subtitle: String?,
    modifier: Modifier,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    songs: List<MediaMetadata>,
    onPlayAll: ((List<MediaMetadata>, Int, Boolean) -> Unit)?,
    onMenu: (() -> Unit)?,
    onSongClick: ((Int) -> Unit)?,
    onSongLongClick: ((MediaMetadata) -> Unit)?,
    onSongMenu: ((MediaMetadata) -> Unit)?,
    header: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val playback = rememberScreenPlayback()
    val handleClick = songClickHandler(songs, onSongClick)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = rememberLazyListState(),
    ) {
        item(key = "artist_banner") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
            ) {
                ArtworkImage(
                    url = bannerUrl,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.surface,
                            ),
                        ),
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
        }

        item(key = "artist_actions") {
            Column(Modifier.fillMaxWidth()) {
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
                ScreenActionRow(
                    onPlay = { playAllOrDefault(context, songs, onPlayAll) },
                    onShuffle = { playAllOrDefault(context, songs, onPlayAll, shuffle = true) },
                    onMenu = onMenu,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        if (tabs.isNotEmpty()) {
            item(key = "artist_tabs") {
                ScreenTabStrip(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                )
            }
        }

        item(key = "artist_header") {
            Column(Modifier.fillMaxWidth()) { header() }
        }

        item(key = "artist_content") {
            Column(Modifier.fillMaxWidth()) { content() }
        }

        when {
            isLoading && songs.isEmpty() ->
                item(key = "artist_loading") {
                    ShimmerHost(Modifier.fillMaxWidth()) {
                        repeat(5) { ListItemPlaceholder(Modifier.fillMaxWidth()) }
                    }
                }

            error != null && songs.isEmpty() ->
                item(key = "artist_error") {
                    ScreenError(onRetry = onRetry, message = error)
                }

            songs.isEmpty() ->
                Unit

            else ->
                screenSongItems(
                    songs = songs,
                    playback = playback,
                    keyPrefix = "artist",
                    onSongClick = handleClick,
                    onSongLongClick = onSongLongClick,
                    onSongMenu = onSongMenu,
                )
        }

        if (error != null && songs.isNotEmpty()) {
            item(key = "artist_error_footer") {
                ScreenError(onRetry = onRetry, message = error, compact = true)
            }
        }
    }
}
