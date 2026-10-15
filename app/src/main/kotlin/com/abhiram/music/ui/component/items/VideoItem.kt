package com.abhiram.music.ui.component.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abhiram.innertube.models.SongItem as SongItemModel

/**
 * Remote video item (a [SongItemModel] that plays a music video).
 *
 * Carries an explicit video badge: a "▶ Video" pill plus a duration chip over the 16:9 artwork
 * in grid mode, or a play-glyph badge on the 48 dp artwork with a "Video" chip in the subtitle
 * line for list rows. Press/selection behaviour matches the other containers.
 *
 * @param item InnerTube song model (video variant).
 * @param onClick invoked on tap.
 * @param modifier root modifier for the cell/row.
 * @param onLongClick invoked on long press, or `null`.
 * @param gridItem renders the 16:9 grid cell variant instead of the list row.
 */
@Composable
fun VideoItem(
    item: SongItemModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    gridItem: Boolean = false,
) {
    val artistsText = item.artists.joinToString(", ") { it.name }
    val durationText = formatDuration(item.duration ?: 0)

    if (gridItem) {
        GridCell(
            title = item.title,
            onClick = onClick,
            modifier = modifier,
            thumbnailRatio = 16f / 9f,
            thumbnail = {
                Box(modifier = Modifier.fillMaxSize()) {
                    Artwork(item.thumbnail, Modifier.fillMaxSize())
                    VideoPill(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    )
                    if (durationText.isNotEmpty()) {
                        DurationChip(
                            text = durationText,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp),
                        )
                    }
                }
            },
            subtitleContent = songSubtitleContent(
                explicit = item.explicit,
                text = artistsText,
                style = MaterialTheme.typography.bodyMedium,
            ),
            onLongClick = onLongClick,
        )
    } else {
        ListItemRow(
            title = item.title,
            onClick = onClick,
            modifier = modifier,
            thumbnail = {
                Box(modifier = Modifier.fillMaxSize()) {
                    Artwork(item.thumbnail, Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
            },
            subtitleContent = {
                VideoLabelChip()
                Spacer(modifier = Modifier.width(6.dp))
                if (item.explicit) {
                    ExplicitBadge()
                    Spacer(modifier = Modifier.width(6.dp))
                }
                SubtitleLabel(artistsText, MaterialTheme.typography.bodySmall)
            },
            trailingContent = { DurationLabel(item.duration ?: 0) },
            onLongClick = onLongClick,
        )
    }
}
