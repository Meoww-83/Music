package com.abhiram.music.ui.component.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.component.MusicBars
import com.abhiram.music.ui.component.ShimmerHost
import com.abhiram.music.ui.component.TextPlaceholder
import com.abhiram.music.ui.component.items.ItemPlaceholder
import com.abhiram.music.ui.theme.expressiveShapes

/** Width of one quick-pick chip inside the two row grid. */
private val QuickPickCellWidth = 220.dp

/** Height of one quick-pick chip (≥ 56 dp row minimum). */
private val QuickPickCellHeight = 64.dp

/**
 * The expressive "Quick picks" home module: a two-row horizontally scrolling grid of pill chips
 * (artwork + title + artist) where the currently playing chip is tinted and shows animated music
 * bars. Chips scale down and spring back on press (staying static under reduced motion).
 *
 * @param songs candidates (already mapped to [MediaMetadata]), typically the user's recents.
 * @param onSongClick chip click; `null` plays that song (and the rest of the picks) through the
 * frozen `PlayerControllerProvider`.
 * @param title section title, defaults to the spec's "Quick picks".
 */
@Composable
fun QuickPicks(
    songs: List<MediaMetadata>,
    onSongClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    title: String = "Quick picks",
    isLoading: Boolean = false,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
) {
    val playback = rememberScreenPlayback()
    val handleClick = songClickHandler(songs, onSongClick)

    Column(modifier = modifier.fillMaxWidth()) {
        ScreenSectionHeader(title = title)

        when {
            isLoading && songs.isEmpty() ->
                ShimmerHost(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(146.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        repeat(3) {
                            ItemPlaceholder(
                                modifier = Modifier
                                    .width(QuickPickCellWidth)
                                    .height(QuickPickCellHeight),
                            )
                        }
                    }
                }

            songs.isEmpty() ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    TextPlaceholder(text = "Nothing here yet")
                }

            else ->
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    state = rememberLazyGridState(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(146.dp),
                ) {
                    itemsIndexed(
                        items = songs,
                        key = { index, song -> "quick_pick:${song.id}#$index" },
                    ) { index, song ->
                        QuickPickCell(
                            song = song,
                            isCurrent = playback.songId == song.id,
                            isPlaying = playback.isPlaying,
                            onClick = { handleClick(index) },
                            onLongClick = onSongLongClick?.let { handler -> { handler(song) } },
                        )
                    }
                }
        }
    }
}

/** One quick-pick chip: tinted pill with 44 dp artwork, title, artist line and playing bars. */
@Composable
private fun QuickPickCell(
    song: MediaMetadata,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.expressiveShapes.pill,
        color = if (isCurrent) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = modifier
            .width(QuickPickCellWidth)
            .height(QuickPickCellHeight)
            .clip(MaterialTheme.expressiveShapes.pill)
            .pressClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.expressiveShapes.squircle,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(44.dp),
            ) {
                ArtworkImage(
                    url = song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    requestSize = 240,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artistLine(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isCurrent) {
                Spacer(Modifier.width(6.dp))
                MusicBars(
                    isPlaying = isPlaying,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
