package com.abhiram.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.abhiram.music.ui.theme.ExpressiveShapes
import com.abhiram.music.ui.theme.expressiveShapes
import com.abhiram.music.utils.resize

/** Width/height requested from `resize` for every collage cell (matches the app's art size). */
private const val PlaylistArtworkSize = 1080

/** Hairline gutter between collage cells so a multi-art playlist reads as a quad. */
private val CollageGap = 2.dp

/** Horizontal padding of the song-count badge. */
private val BadgePaddingHorizontal = 8.dp

/** Vertical padding of the song-count badge. */
private val BadgePaddingVertical = 4.dp

/**
 * Playlist artwork, optionally a collage of up to four covers with a song-count badge.
 *
 * Up to four of the [thumbnails] are laid out as a 2×2 collage (1 → full bleed, 2 → two halves,
 * 3 → one half plus a stacked pair, 4 → quad), each request upgraded through
 * [resize] so low-resolution library URLs are fetched at full artwork size. Every cell shares
 * [shape] — clipped once on the root — so the collage silhouette stays expressive.
 *
 * @param thumbnails cover URLs in priority order; blanks are ignored and at most four are used.
 *   An empty list falls back to a neutral music-note glyph on the container colour.
 * @param modifier sizing/positioning modifier (e.g. `Modifier.size(170.dp)`).
 * @param shape silhouette of the artwork; defaults to the expressive `cookie`.
 * @param songCount when non-null, draws a pill badge (`"12 songs"`) in the bottom-end corner.
 */
@Composable
fun PlaylistThumbnail(
    thumbnails: List<String>,
    modifier: Modifier = Modifier,
    shape: Shape = ExpressiveShapes.cookie,
    songCount: Int? = null,
) {
    val urls = remember(thumbnails) { thumbnails.filter { it.isNotBlank() }.take(4) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        when (urls.size) {
            0 -> Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.4f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )

            1 -> CollageCell(
                url = urls[0],
                modifier = Modifier.fillMaxSize(),
            )

            2 -> Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(CollageGap),
            ) {
                CollageCell(url = urls[0], modifier = Modifier.weight(1f).fillMaxHeight())
                CollageCell(url = urls[1], modifier = Modifier.weight(1f).fillMaxHeight())
            }

            3 -> Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(CollageGap),
            ) {
                CollageCell(url = urls[0], modifier = Modifier.weight(1f).fillMaxHeight())
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(CollageGap),
                ) {
                    CollageCell(url = urls[1], modifier = Modifier.weight(1f).fillMaxWidth())
                    CollageCell(url = urls[2], modifier = Modifier.weight(1f).fillMaxWidth())
                }
            }

            else -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(CollageGap),
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CollageGap),
                ) {
                    CollageCell(url = urls[0], modifier = Modifier.weight(1f).fillMaxHeight())
                    CollageCell(url = urls[1], modifier = Modifier.weight(1f).fillMaxHeight())
                }
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CollageGap),
                ) {
                    CollageCell(url = urls[2], modifier = Modifier.weight(1f).fillMaxHeight())
                    CollageCell(url = urls[3], modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }

        if (songCount != null) {
            Text(
                text = if (songCount == 1) "1 song" else "$songCount songs",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        start = BadgePaddingHorizontal,
                        top = BadgePaddingVertical,
                        end = BadgePaddingHorizontal,
                        bottom = BadgePaddingVertical,
                    )
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.94f),
                        MaterialTheme.expressiveShapes.pill,
                    )
                    .padding(horizontal = BadgePaddingHorizontal, vertical = BadgePaddingVertical),
            )
        }
    }
}

/**
 * One cover of the collage: a container-coloured cell with the (upgraded) artwork on top.
 *
 * The cell background shows while Coil loads, so gapped collages never flash transparent.
 */
@Composable
private fun CollageCell(url: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        AsyncImage(
            model = url.resize(PlaylistArtworkSize, PlaylistArtworkSize),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
