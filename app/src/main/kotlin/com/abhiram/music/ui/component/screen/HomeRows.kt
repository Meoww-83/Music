package com.abhiram.music.ui.component.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhiram.innertube.models.AlbumItem as InnertubeAlbumItem
import com.abhiram.innertube.models.ArtistItem as InnertubeArtistItem
import com.abhiram.innertube.models.PlaylistItem as InnertubePlaylistItem
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.component.MusicBars
import com.abhiram.music.ui.component.ShimmerHost
import com.abhiram.music.ui.component.items.AlbumItem
import com.abhiram.music.ui.component.items.ItemPlaceholder
import com.abhiram.music.ui.component.items.ArtistItem
import com.abhiram.music.ui.component.items.PlaylistItem
import com.abhiram.music.ui.theme.expressiveShapes

/*
 * The horizontally scrolling home rows (owner CMP-D). Every row pairs a bold section header with
 * a "Show all" affordance and a paging card carousel: pages snap card-to-card while the next
 * card always peeks from the edge, so the row invites horizontal exploration (§9).
 */

/**
 * Shared shell of the home rows: section header + snapping carousel of fixed width cards.
 *
 * Renders nothing when the row is empty and not loading so the home screen can simply hand over
 * every section it has.
 *
 * @param itemCount number of backing items; [content] receives the item indices of one page.
 * @param cardWidth width of one card; also the pager's [PageSize.Fixed].
 * @param rowHeight total height reserved for the carousel (cards + their labels).
 * @param content renders the cards of one page inside a horizontal [Row].
 */
@Composable
internal fun HomeRow(
    title: String,
    itemCount: Int,
    cardWidth: Dp,
    rowHeight: Dp,
    modifier: Modifier = Modifier,
    onShowAll: (() -> Unit)? = null,
    isLoading: Boolean = false,
    content: @Composable RowScope.(page: List<Int>) -> Unit,
) {
    if (itemCount <= 0 && !isLoading) return

    Column(modifier = modifier.fillMaxWidth()) {
        ScreenSectionHeader(
            title = title,
            actionText = "Show all",
            onAction = onShowAll,
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
        ) {
            val spacing = 12.dp
            // How many cards fit in the padded viewport — pages are chunked by the same number.
            val visibleCards = ((maxWidth - 40.dp + spacing) / (cardWidth + spacing))
                .toInt()
                .coerceAtLeast(1)

            if (isLoading) {
                ShimmerHost(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        repeat(visibleCards) {
                            ItemPlaceholder(
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(rowHeight - 8.dp),
                            )
                        }
                    }
                }
            } else {
                val pages = (0 until itemCount).chunked(visibleCards)
                val pagerState = rememberPagerState(pageCount = { pages.size })
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    pageSize = PageSize.Fixed(cardWidth),
                    pageSpacing = spacing,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        content(pages[page])
                    }
                }
            }
        }
    }
}

/**
 * Home row of song cards (square artwork, title, artist line). The currently playing card is
 * tinted and shows animated music bars.
 *
 * @param onSongClick card click; `null` plays that song (and the rest of the row) through the
 * frozen `PlayerControllerProvider`.
 * @param onShowAll wired to the header's "Show all" affordance (hidden when `null`).
 */
@Composable
fun HomeSongs(
    songs: List<MediaMetadata>,
    onSongClick: ((Int) -> Unit)? = null,
    onShowAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
    title: String = "Songs",
) {
    val playback = rememberScreenPlayback()
    val handleClick = songClickHandler(songs, onSongClick)

    HomeRow(
        title = title,
        itemCount = songs.size,
        cardWidth = 150.dp,
        rowHeight = 236.dp,
        modifier = modifier,
        onShowAll = onShowAll,
        isLoading = isLoading,
    ) { page ->
        page.forEach { index ->
            val song = songs[index]
            HomeSongCard(
                song = song,
                isCurrent = playback.songId == song.id,
                isPlaying = playback.isPlaying,
                onClick = { handleClick(index) },
                onLongClick = onSongLongClick?.let { handler -> { handler(song) } },
                modifier = Modifier.width(150.dp),
            )
        }
    }
}

/**
 * Home row of online albums rendered with CMP-C's grid [AlbumItem].
 *
 * @param onAlbumClick navigation to the album page (required — navigation stays with the caller).
 * @param onShowAll wired to the header's "Show all" affordance (hidden when `null`).
 */
@Composable
fun HomeAlbums(
    albums: List<InnertubeAlbumItem>,
    onAlbumClick: (InnertubeAlbumItem) -> Unit,
    onShowAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onAlbumLongClick: ((InnertubeAlbumItem) -> Unit)? = null,
    title: String = "Albums",
) {
    HomeRow(
        title = title,
        itemCount = albums.size,
        cardWidth = 150.dp,
        rowHeight = 216.dp,
        modifier = modifier,
        onShowAll = onShowAll,
        isLoading = isLoading,
    ) { page ->
        page.forEach { index ->
            val album = albums[index]
            AlbumItem(
                item = album,
                onClick = { onAlbumClick(album) },
                onLongClick = onAlbumLongClick?.let { handler -> { handler(album) } },
                gridItem = true,
                modifier = Modifier.width(150.dp),
            )
        }
    }
}

/**
 * Home row of online artists rendered with CMP-C's grid [ArtistItem] (circular artwork).
 *
 * @param onArtistClick navigation to the artist page (required — navigation stays with the caller).
 * @param onShowAll wired to the header's "Show all" affordance (hidden when `null`).
 */
@Composable
fun HomeArtistList(
    artists: List<InnertubeArtistItem>,
    onArtistClick: (InnertubeArtistItem) -> Unit,
    onShowAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onArtistLongClick: ((InnertubeArtistItem) -> Unit)? = null,
    title: String = "Artists",
) {
    HomeRow(
        title = title,
        itemCount = artists.size,
        cardWidth = 120.dp,
        rowHeight = 188.dp,
        modifier = modifier,
        onShowAll = onShowAll,
        isLoading = isLoading,
    ) { page ->
        page.forEach { index ->
            val artist = artists[index]
            ArtistItem(
                item = artist,
                onClick = { onArtistClick(artist) },
                onLongClick = onArtistLongClick?.let { handler -> { handler(artist) } },
                gridItem = true,
                modifier = Modifier.width(120.dp),
            )
        }
    }
}

/**
 * Home row of online playlists rendered with CMP-C's grid [PlaylistItem].
 *
 * @param onPlaylistClick navigation to the playlist page (required — navigation stays with the
 * caller).
 * @param onShowAll wired to the header's "Show all" affordance (hidden when `null`).
 */
@Composable
fun HomePlaylists(
    playlists: List<InnertubePlaylistItem>,
    onPlaylistClick: (InnertubePlaylistItem) -> Unit,
    onShowAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onPlaylistLongClick: ((InnertubePlaylistItem) -> Unit)? = null,
    title: String = "Playlists",
) {
    HomeRow(
        title = title,
        itemCount = playlists.size,
        cardWidth = 150.dp,
        rowHeight = 216.dp,
        modifier = modifier,
        onShowAll = onShowAll,
        isLoading = isLoading,
    ) { page ->
        page.forEach { index ->
            val playlist = playlists[index]
            PlaylistItem(
                item = playlist,
                onClick = { onPlaylistClick(playlist) },
                onLongClick = onPlaylistLongClick?.let { handler -> { handler(playlist) } },
                gridItem = true,
                modifier = Modifier.width(150.dp),
            )
        }
    }
}

/** Song card used by [HomeSongs]: square artwork, title, artist line and playing bars. */
@Composable
private fun HomeSongCard(
    song: MediaMetadata,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Surface(
            shape = MaterialTheme.expressiveShapes.squircle,
            color = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pressClickable(onClick = onClick, onLongClick = onLongClick),
        ) {
            ArtworkImage(
                url = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                requestSize = 480,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isCurrent) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = song.artistLine(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isCurrent) {
                MusicBars(
                    isPlaying = isPlaying,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
