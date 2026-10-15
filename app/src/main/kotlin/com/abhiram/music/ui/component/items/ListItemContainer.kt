package com.abhiram.music.ui.component.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhiram.music.ui.component.ShimmerHost
import com.abhiram.music.ui.component.TextPlaceholder

/**
 * Wide list row used by every linear item (songs, albums, artists, playlists…).
 *
 * Layout: optional [leadingContent] (track index, [com.abhiram.music.ui.component.MusicBars],
 * drag handle…), 48 dp artwork, title with a one-line [subtitle], then the [trailingContent]
 * slot (duration, overflow menu…). Rows are at least 56 dp tall; pressing springs the row
 * slightly down and dims it; [selected] fills it with `primaryContainer` and swaps the content
 * colour to `onPrimaryContainer`.
 *
 * @param title row title, single line.
 * @param onClick invoked on tap.
 * @param modifier root modifier for the row.
 * @param thumbnail artwork slot; rendered in a fixed 48 dp square.
 * @param subtitle secondary line (artists / year / count), single line.
 * @param leadingContent row-scoped slot rendered before the artwork.
 * @param trailingContent row-scoped slot rendered after the text column.
 * @param selected whether the row should render with the active/selected highlight.
 * @param onLongClick invoked on long press, or `null` to disable long press.
 * @param enabled toggles the disabled (reduced alpha, non-clickable) state.
 */
@Composable
fun ListItemContainer(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: @Composable () -> Unit,
    subtitle: String? = null,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    ListItemRow(
        title = title,
        onClick = onClick,
        modifier = modifier,
        thumbnail = thumbnail,
        subtitleContent = plainSubtitleContent(subtitle, MaterialTheme.typography.bodySmall),
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        selected = selected,
        onLongClick = onLongClick,
        enabled = enabled,
    )
}

/**
 * Shared list-row implementation. Identical to [ListItemContainer] but with a composable
 * subtitle slot so item components can render badges (explicit "E", "Video"…) inline.
 */
@Composable
internal fun ListItemRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: @Composable () -> Unit,
    subtitleContent: (@Composable RowScope.() -> Unit)? = null,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val highlight = animateHighlight(selected)
    val titleColor = itemTitleColor(selected)
    val press = rememberItemPressState(
        enabled = enabled,
        onClick = onClick,
        onLongClick = onLongClick,
        pressScale = RowPressScale,
        shape = MaterialTheme.shapes.medium,
        backgroundColor = highlight,
    )

    CompositionLocalProvider(LocalItemPressed provides press.pressed) {
        CompositionLocalProvider(LocalContentColor provides titleColor) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .defaultMinSize(minHeight = 56.dp)
                    .then(press.modifier)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                if (leadingContent != null) {
                    leadingContent()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier.size(ListThumbnailSize),
                    contentAlignment = Alignment.Center,
                ) {
                    thumbnail()
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitleContent != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CompositionLocalProvider(
                                LocalContentColor provides titleColor.copy(alpha = 0.72f),
                            ) {
                                subtitleContent()
                            }
                        }
                    }
                }
                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingContent()
                }
            }
        }
    }
}

/**
 * Shimmering list-row skeleton shown while a list is loading.
 *
 * Mirrors [ListItemContainer]'s layout: 48 dp artwork block, two text bars and a duration bar;
 * shimmer is disabled under reduce-motion.
 *
 * @param modifier root modifier for the skeleton row.
 */
@Composable
fun ListItemPlaceholder(modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    ShimmerHost(modifier = modifier, enabled = !reduceMotion) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                TextPlaceholder(text = "Placeholder title")
                Spacer(modifier = Modifier.height(4.dp))
                TextPlaceholder(text = "Placeholder subtitle")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
        }
    }
}
