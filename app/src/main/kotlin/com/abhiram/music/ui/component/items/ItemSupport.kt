package com.abhiram.music.ui.component.items

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.abhiram.music.ui.component.MusicBars
import java.util.Locale

/* ---------------------------------------------------------------------------------------------
 * Shared building blocks for the item components (grid cells + list rows).
 * ------------------------------------------------------------------------------------------ */

/** Width/height of the artwork shown at the start of every list row. */
internal val ListThumbnailSize: Dp = 48.dp

/** Artwork corner radius while the hosting cell/row is at rest. */
private val ArtworkBaseRadius: Dp = 12.dp

/** Artwork corner radius while the hosting cell/row is pressed (round-square → circle morph). */
private val ArtworkPressRadius: Dp = 24.dp

/** How strongly the press scrim darkens a cell/row while it is held. */
private const val PressDimAlpha: Float = 0.06f

/** M3 disabled content alpha, applied to disabled cells/rows. */
private const val DisabledAlpha: Float = 0.38f

/** Spring scale target while a grid cell is held down. */
internal const val GridPressScale: Float = 0.96f

/** Spring scale target while a list row is held down (subtler: rows are full width). */
internal const val RowPressScale: Float = 0.98f

/**
 * Publishes the pressed state of the hosting [ItemContainer] / [ListItemContainer] so nested
 * artwork (see [Artwork]) can morph its shape while the pointer is down.
 */
internal val LocalItemPressed = compositionLocalOf { false }

/**
 * Reads the platform animator scale and reports whether "reduce motion" is active.
 *
 * Used until the theme package exposes its own `LocalReduceMotion`; animations switch to
 * instant (`snap()`) specs when this returns `true`.
 */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/** Interaction state + full press modifier chain (dim → scale → shape → ripple) of a cell/row. */
internal class ItemPress(
    val interactionSource: MutableInteractionSource,
    val pressed: Boolean,
    val modifier: Modifier,
)

/**
 * Builds the press feedback shared by every container: a bounded ripple, a spring scale-down and
 * a subtle brightness (scrim) reduction, all skipped when reduce-motion is enabled.
 *
 * The returned modifier already contains clip + background so the ripple is clipped to [shape]
 * and the highlight sits underneath the content.
 */
@Composable
internal fun rememberItemPressState(
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    pressScale: Float,
    shape: Shape,
    backgroundColor: Color,
): ItemPress {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = rememberReduceMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressScale else 1f,
        animationSpec = if (reduceMotion) {
            snap()
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        },
        label = "itemPressScale",
    )
    val dim by animateFloatAsState(
        targetValue = if (pressed) PressDimAlpha else 0f,
        animationSpec = if (reduceMotion) {
            snap()
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "itemPressDim",
    )
    val pressModifier = Modifier
        .alpha(if (enabled) 1f else DisabledAlpha)
        .drawWithContent {
            drawContent()
            if (dim > 0f) drawRect(Color.Black.copy(alpha = dim))
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .background(backgroundColor)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    return ItemPress(
        interactionSource = interactionSource,
        pressed = pressed,
        modifier = pressModifier,
    )
}

/**
 * Animated selection/active highlight: [androidx.compose.material3.ColorScheme.primaryContainer]
 * when [selected], fully transparent otherwise (spring in, instant out under reduce-motion).
 */
@Composable
internal fun animateHighlight(selected: Boolean): Color {
    val reduceMotion = rememberReduceMotion()
    return animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = if (reduceMotion) {
            snap()
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        },
        label = "itemHighlight",
    ).value
}

/** Title/content colour for a container: swaps to `onPrimaryContainer` when highlighted. */
@Composable
internal fun itemTitleColor(selected: Boolean): Color =
    if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

/* ---------------------------------------------------------------------------------------------
 * Artwork
 * ------------------------------------------------------------------------------------------ */

/**
 * Round-square (or circular) artwork rendered with coil.
 *
 * Draws a music-note fallback when [url] is missing, passes the same note vector to coil as
 * placeholder/error painter, and morphs its corners toward a circle while the hosting
 * container is pressed (honouring reduce-motion).
 */
@Composable
internal fun Artwork(
    url: String?,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.MusicNote,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    circle: Boolean = false,
) {
    val pressed = LocalItemPressed.current
    val reduceMotion = rememberReduceMotion()
    val radius by animateDpAsState(
        targetValue = if (pressed) ArtworkPressRadius else ArtworkBaseRadius,
        animationSpec = if (reduceMotion) {
            snap()
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        },
        label = "artworkRadius",
    )
    val shape: Shape = if (circle) CircleShape else RoundedCornerShape(radius)
    val notePainter = rememberVectorPainter(Icons.Rounded.MusicNote)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .aspectRatio(1f),
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = notePainter,
                error = notePainter,
                fallback = notePainter,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/* ---------------------------------------------------------------------------------------------
 * Subtitle line helpers
 * ------------------------------------------------------------------------------------------ */

/** Ellipsised, weight-consuming label used inside a container's subtitle row. */
@Composable
internal fun RowScope.SubtitleLabel(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodySmall,
) {
    Text(
        text = text,
        style = style,
        color = LocalContentColor.current,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
    )
}

/** Compact "E" badge shown in front of the subtitle line of explicit tracks. */
@Composable
internal fun ExplicitBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(LocalContentColor.current.copy(alpha = 0.14f))
            .padding(horizontal = 3.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "E",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = LocalContentColor.current,
            maxLines = 1,
        )
    }
}

/** Compact "Video" chip used in the subtitle line of [VideoItem]. */
@Composable
internal fun VideoLabelChip() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(LocalContentColor.current.copy(alpha = 0.14f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Video",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = LocalContentColor.current,
            maxLines = 1,
        )
    }
}

/** "▶ Video" pill overlaid on a video thumbnail (grid). */
@Composable
internal fun VideoPill(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.Black.copy(alpha = 0.68f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(10.dp),
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "Video",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
        )
    }
}

/** Dark duration chip overlaid on a video thumbnail (grid). */
@Composable
internal fun DurationChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
        )
    }
}

/** Trailing duration label (renders nothing when the duration is unknown). */
@Composable
internal fun RowScope.DurationLabel(durationSeconds: Int) {
    val text = remember(durationSeconds) { formatDuration(durationSeconds) }
    if (text.isNotEmpty()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(alpha = 0.72f),
            maxLines = 1,
        )
    }
}

/**
 * Dark pill with animated [MusicBars] used as the "now playing" indicator overlaid on grid
 * artwork (top-start corner).
 */
@Composable
internal fun PlayingIndicatorPill(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        MusicBars(isPlaying = true, color = Color.White)
    }
}

/** Formats seconds as `m:ss` (or `h:mm:ss`); empty string when unknown/non-positive. */
internal fun formatDuration(totalSeconds: Int): String {
    if (totalSeconds <= 0) return ""
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/** Joins the non-blank parts of a subtitle with ` • `; `null` when nothing is left. */
internal fun joinSubtitles(vararg parts: String?): String? =
    parts
        .filterNotNull()
        .filter { it.isNotBlank() }
        .joinToString(" • ")
        .ifEmpty { null }

/** Human readable song count label (`null` when [count] is unknown/empty). */
internal fun songCountLabel(count: Int): String? =
    when {
        count <= 0 -> null
        count == 1 -> "1 song"
        else -> "$count songs"
    }

/**
 * Builds a subtitle slot containing an optional explicit badge plus the ellipsised [text].
 * Returns `null` when there is nothing to show.
 */
internal fun songSubtitleContent(
    explicit: Boolean,
    text: String?,
    style: TextStyle,
): (@Composable RowScope.() -> Unit)? {
    if (!explicit && text.isNullOrEmpty()) return null
    return {
        if (explicit) {
            ExplicitBadge()
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (!text.isNullOrEmpty()) {
            SubtitleLabel(text, style)
        }
    }
}

/** [songSubtitleContent] without the explicit badge. */
internal fun plainSubtitleContent(
    text: String?,
    style: TextStyle,
): (@Composable RowScope.() -> Unit)? = songSubtitleContent(explicit = false, text = text, style = style)

/**
 * Adapts a caller supplied trailing/menu slot (row scoped) into the non-scoped `icon` overlay
 * slot of a grid cell.
 */
internal fun menuIconSlot(slotContent: (@Composable RowScope.() -> Unit)?): (@Composable () -> Unit)? {
    val content = slotContent ?: return null
    return {
        Row(verticalAlignment = Alignment.CenterVertically) { content() }
    }
}
