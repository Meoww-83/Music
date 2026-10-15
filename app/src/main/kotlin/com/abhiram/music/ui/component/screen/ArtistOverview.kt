package com.abhiram.music.ui.component.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhiram.innertube.models.ArtistItem as InnertubeArtistItem
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.component.MusicBars
import com.abhiram.music.ui.component.items.ArtistItem
import com.abhiram.music.ui.theme.expressiveShapes

/**
 * The expressive "Overview" tab body of [ArtistScreen]: listener/subscriber stat pills, an
 * expandable biography, a horizontal carousel of top songs with rank badges, a related-artist
 * carousel and a share action (which copies [shareLink] to the clipboard unless [onShare] is
 * supplied).
 *
 * This component is a plain column (no vertical scrolling of its own) so it can be embedded in
 * [ArtistScreen]'s content slot or any other scrolling container.
 *
 * @param biography plain-text artist biography; blank hides the About section.
 * @param topSongs carousel items — tapping one plays it through `PlayerControllerProvider`
 * unless [onSongClick] is supplied.
 * @param relatedArtists "Fans might also like" carousel.
 * @param shareLink share target copied to the clipboard by the built-in share action.
 */
@Composable
fun ArtistOverview(
    biography: String?,
    topSongs: List<MediaMetadata>,
    relatedArtists: List<InnertubeArtistItem>,
    shareLink: String?,
    modifier: Modifier = Modifier,
    subscriberCount: String? = null,
    monthlyListeners: String? = null,
    onSongClick: ((Int) -> Unit)? = null,
    onSongLongClick: ((MediaMetadata) -> Unit)? = null,
    onArtistClick: ((InnertubeArtistItem) -> Unit)? = null,
    onShowAllTopSongs: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val playback = rememberScreenPlayback()
    val handleClick = songClickHandler(topSongs, onSongClick)
    val stats = listOfNotNull(subscriberCount, monthlyListeners)

    Column(modifier = modifier.fillMaxWidth()) {
        if (stats.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                stats.forEach { stat -> StatPill(text = stat) }
            }
        }

        if (!biography.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                ExpandableText(
                    text = biography,
                    collapsedMaxLines = 4,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (topSongs.isNotEmpty()) {
            ScreenSectionHeader(
                title = "Top songs",
                actionText = "Show all",
                onAction = onShowAllTopSongs,
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(
                    items = topSongs,
                    key = { index, song -> "top_song:${song.id}#$index" },
                ) { index, song ->
                    TopSongCard(
                        song = song,
                        rank = index + 1,
                        isCurrent = playback.songId == song.id,
                        isPlaying = playback.isPlaying,
                        onClick = { handleClick(index) },
                        onLongClick = onSongLongClick?.let { handler -> { handler(song) } },
                    )
                }
            }
        }

        if (relatedArtists.isNotEmpty()) {
            ScreenSectionHeader(title = "Fans might also like")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(
                    items = relatedArtists,
                    key = { index, artist -> "related:${artist.id}#$index" },
                ) { _, artist ->
                    ArtistItem(
                        item = artist,
                        onClick = { onArtistClick?.invoke(artist) },
                        gridItem = true,
                        modifier = Modifier.width(120.dp),
                    )
                }
            }
        }

        if (shareLink != null || onShare != null) {
            val shareHandler = onShare
            Surface(
                onClick = {
                    val handler = shareHandler
                    if (handler != null) {
                        handler()
                    } else {
                        copyLinkToClipboard(context, shareLink)
                    }
                },
                shape = MaterialTheme.expressiveShapes.pill,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Compact tonal pill used for the listener/subscriber counts. */
@Composable
private fun StatPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.expressiveShapes.pill,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/** Top-song carousel card: square artwork with a rank badge (and music bars while playing). */
@Composable
private fun TopSongCard(
    song: MediaMetadata,
    rank: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .pressClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box {
            Surface(
                shape = MaterialTheme.expressiveShapes.squircle,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                ArtworkImage(
                    url = song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    requestSize = 480,
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(26.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = rank.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (isCurrent) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(26.dp),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MusicBars(
                            isPlaying = isPlaying,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = song.artistLine(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Copies [link] to the clipboard, with a confirmation toast below API 33 (which shows its own). */
private fun copyLinkToClipboard(
    context: Context,
    link: String?,
) {
    if (link.isNullOrBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Music link", link))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
    }
}
