package com.abhiram.music.ui.component.items

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
 * Square grid cell used by every card-style item (album, playlist, artist, song…).
 *
 * Renders the [thumbnail] as a square block (1:1 artwork area), a two-line marquee [title] and
 * an optional one-line [subtitle]. [icon] is drawn as an overlay at the bottom-end corner of the
 * artwork (e.g. a menu button), [sortIndex] as a 1-based rank chip at the top-start corner.
 * Pressing the cell springs it slightly down and dims it; long press invokes [onLongClick] when
 * provided; [selected] fills the cell with `primaryContainer`.
 *
 * @param title cell title, capped at two lines and marquee-scrolled when too long.
 * @param onClick invoked on tap.
 * @param modifier root modifier for the cell.
 * @param thumbnail artwork slot; fills the square artwork area.
 * @param subtitle secondary line (artist / year / count), single line.
 * @param icon optional overlay composited above the artwork's bottom-end corner.
 * @param selected whether the cell should render with the active/selected highlight.
 * @param onLongClick invoked on long press, or `null` to disable long press.
 * @param enabled toggles the disabled (reduced alpha, non-clickable) state.
 * @param sortIndex zero-based sort position; rendered as a 1-based rank chip.
 */
@Composable
fun ItemContainer(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: @Composable () -> Unit,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    sortIndex: Int? = null,
) {
    GridCell(
        title = title,
        onClick = onClick,
        modifier = modifier,
        thumbnailRatio = 1f,
        thumbnail = thumbnail,
        subtitleContent = plainSubtitleContent(subtitle, MaterialTheme.typography.bodyMedium),
        icon = icon,
        selected = selected,
        onLongClick = onLongClick,
        enabled = enabled,
        sortIndex = sortIndex,
    )
}

/**
 * Shared grid-cell implementation. Identical to [ItemContainer] but with a composable subtitle
 * slot (badges), a configurable artwork aspect ratio (16:9 video cells) and a row-scoped leading
 * overlay used by [MediaSongItem].
 */
@Composable
internal fun GridCell(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailRatio: Float = 1f,
    thumbnail: @Composable () -> Unit,
    subtitleContent: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    leadingOverlay: (@Composable RowScope.() -> Unit)? = null,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    sortIndex: Int? = null,
) {
    val highlight = animateHighlight(selected)
    val titleColor = itemTitleColor(selected)
    val press = rememberItemPressState(
        enabled = enabled,
        onClick = onClick,
        onLongClick = onLongClick,
        pressScale = GridPressScale,
        shape = MaterialTheme.shapes.medium,
        backgroundColor = highlight,
    )

    CompositionLocalProvider(LocalItemPressed provides press.pressed) {
        CompositionLocalProvider(LocalContentColor provides titleColor) {
            Column(
                modifier = modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .then(press.modifier)
                    .padding(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(thumbnailRatio),
                    contentAlignment = Alignment.Center,
                ) {
                    thumbnail()
                    if (sortIndex != null || leadingOverlay != null) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (sortIndex != null) {
                                RankBadge(sortIndex)
                                if (leadingOverlay != null) Spacer(modifier = Modifier.width(6.dp))
                            }
                            if (leadingOverlay != null) leadingOverlay()
                        }
                    }
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp),
                        ) {
                            icon()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
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
        }
    }
}

/** Solid rank chip shown at the top-start of a cell when [ItemContainer.sortIndex] is set. */
@Composable
private fun RankBadge(index: Int) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
        )
    }
}

/**
 * Shimmering grid-cell skeleton shown while a grid section is loading.
 *
 * Mirrors [ItemContainer]'s layout: square artwork block plus two text bars; shimmer is disabled
 * under reduce-motion.
 *
 * @param modifier root modifier for the skeleton cell.
 */
@Composable
fun ItemPlaceholder(modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    ShimmerHost(modifier = modifier, enabled = !reduceMotion) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextPlaceholder(text = "Placeholder title")
            Spacer(modifier = Modifier.height(4.dp))
            TextPlaceholder(text = "Placeholder subtitle")
        }
    }
}
