package com.abhiram.music.ui.component.screen

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.playback.PlayerControllerProvider
import com.abhiram.music.ui.component.TooltipIconButton
import com.abhiram.music.ui.theme.expressiveShapes
import com.abhiram.music.utils.formatDuration
import com.abhiram.music.utils.resize

/*
 * Shared, package-private plumbing for the album / artist / playlist / home screen components
 * (owner CMP-D). Nothing here owns data or navigation: every helper is a pure rendering concern
 * fed by the caller, and playback state is only read from the frozen `PlayerControllerProvider`
 * to draw the "now playing" indicators.
 */

/** Snapshot of the player used to highlight the current row and animate the music bars. */
internal data class ScreenPlayback(
    val songId: String?,
    val isPlaying: Boolean,
)

/**
 * Reads the process-wide player state (frozen [PlayerControllerProvider]) for row indicators.
 * Falls back to "nothing playing" while the playback service is still starting.
 */
@Composable
internal fun rememberScreenPlayback(): ScreenPlayback {
    val controller by PlayerControllerProvider.controller.collectAsState()
    val state = controller?.state?.collectAsState()?.value
    return ScreenPlayback(
        songId = state?.current?.id,
        isPlaying = state?.isPlaying == true,
    )
}

/**
 * True when the system animator duration scale is 0 (reduced motion / accessibility).
 *
 * THM may later publish `com.abhiram.music.ui.theme.LocalReduceMotion`; that name is not part of
 * the frozen contract (§5), so this local fallback — identical to `ExpressiveTheme`'s default
 * source — keeps the components compiling either way and is the single place to re-point if the
 * theme package grows its own composition local.
 */
@Composable
internal fun rememberScreenReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Resolves the play-all behaviour of the action rows: [override] when the caller supplied one,
 * otherwise the frozen provider which cold-starts the playback service when needed (§3.2).
 */
internal fun playAllOrDefault(
    context: Context,
    items: List<MediaMetadata>,
    override: ((List<MediaMetadata>, Int, Boolean) -> Unit)?,
    startIndex: Int = 0,
    shuffle: Boolean = false,
) {
    if (items.isEmpty()) return
    val handler = override
    if (handler != null) {
        handler(items, startIndex, shuffle)
    } else {
        PlayerControllerProvider.playAll(context, items, startIndex, shuffle)
    }
}

/**
 * Builds the row-click handler: [onSongClick] when provided, otherwise tapping a row starts
 * playback of that song through the frozen provider (navigation always stays with the caller).
 */
@Composable
internal fun songClickHandler(
    songs: List<MediaMetadata>,
    onSongClick: ((Int) -> Unit)?,
): (Int) -> Unit {
    val context = LocalContext.current
    return onSongClick ?: { index -> PlayerControllerProvider.playAll(context, songs, index) }
}

/* -------------------------------------------------------------------------------------------
 * Artwork + shared building blocks
 * ---------------------------------------------------------------------------------------- */

/**
 * Artwork through Coil 3, resized with the shared `utils.resize` rules. Renders a neutral
 * music-note tile while [url] is blank so headers never collapse while images load.
 */
@Composable
internal fun ArtworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    requestSize: Int = 1080,
) {
    val model = url?.takeIf { it.isNotBlank() }?.resize(requestSize, requestSize)
    if (model == null) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxSize(0.4f),
            )
        }
    } else {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

/** Expressive failure block with a Retry action; used by every screen component. */
@Composable
internal fun ScreenError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = if (compact) 16.dp else 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(if (compact) 44.dp else 64.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(if (compact) 11.dp else 16.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onRetry) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

/** Section title used inside the screen headers and the home rows (with an optional action). */
@Composable
internal fun ScreenSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionText ?: "Show all")
            }
        }
    }
}

/**
 * Big centred cover header shared by the album / playlist screens: artwork, title, an optional
 * subtitle slot (artists / author), a stats line and the action row. Sizes itself with
 * [Modifier.animateContentSize] so the header morphs into place when loading finishes.
 */
@Composable
internal fun ScreenCoverHeader(
    title: String,
    modifier: Modifier = Modifier,
    artwork: (@Composable () -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    meta: String? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        artwork?.invoke()
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Box(Modifier.padding(horizontal = 24.dp)) { subtitle() }
        }
        if (meta != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        if (actions != null) {
            Spacer(Modifier.height(16.dp))
            actions()
        }
    }
}

/**
 * Unified pill-shaped control cluster for the detail headers: a big play button, a shuffle
 * button and — when the caller wires them — library/menu affordances. Every control is at least
 * 48 dp and the whole row sits in one expressive container (§9 "grouped controls").
 */
@Composable
internal fun ScreenActionRow(
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    onAddToLibrary: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    extra: (@Composable RowScope.() -> Unit)? = null,
) {
    Surface(
        shape = MaterialTheme.expressiveShapes.pill,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                onClick = onPlay,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onShuffle,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            extra?.invoke(this)
            if (onAddToLibrary != null) {
                TooltipIconButton(
                    contentDescription = "Save to library",
                    onClick = onAddToLibrary,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            if (onMenu != null) {
                TooltipIconButton(
                    contentDescription = "More options",
                    onClick = onMenu,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/**
 * Multi-line body text with an expressive "Read more / Show less" toggle; the height animates
 * with a spring via `animateContentSize()` and collapses to [collapsedMaxLines] lines.
 */
@Composable
internal fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 3,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (text.isBlank()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = if (collapsedMaxLines > 0 && !expanded) collapsedMaxLines else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.animateContentSize(
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ),
        )
        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Text(if (expanded) "Show less" else "Read more")
        }
    }
}

/**
 * Expressive press feedback for grid/home cells: the cell scales down slightly while pressed and
 * springs back; long-press opens the menu slot. Stays completely static under reduced motion.
 */
@Composable
internal fun Modifier.pressClickable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = rememberScreenReduceMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduceMotion) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pressScale",
    )
    return this
        .then(modifier)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}

/** Pill tab strip for [ArtistScreen].
 *
 * NOTE (integration): CMP-A owns `TabGroup`/`TabScaffold`, but their signatures are not part of
 * the frozen contract (§6 only lists them as "free"), so this adapter renders a self-contained
 * expressive pill strip. It is the single place to re-point if the screens should instead use
 * CMP-A's tab host.
 */
@Composable
internal fun ScreenTabStrip(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = index == selectedTab
            val container by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                label = "tabContainer",
            )
            val content by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "tabContent",
            )
            Surface(
                onClick = { onTabSelected(index) },
                shape = MaterialTheme.expressiveShapes.pill,
                color = container,
                modifier = Modifier.height(48.dp),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = content,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** "1973 · 12 songs · 48:12" style meta line built from an optional extra + the song list. */
internal fun songStats(
    songs: List<MediaMetadata>,
    extra: String? = null,
): String {
    val parts = ArrayList<String>(3)
    if (!extra.isNullOrBlank()) parts += extra
    parts += if (songs.size == 1) "1 song" else "${songs.size} songs"
    val totalSeconds = songs.sumOf { it.duration }
    if (totalSeconds > 0) parts += totalSeconds.formatDuration()
    return parts.joinToString("  ·  ")
}

/** Comma separated artist names of a song, used as a default list subtitle. */
internal fun MediaMetadata.artistLine(maxItems: Int = 3): String {
    val names = artists.take(maxItems).map { it.name }
    val line = names.joinToString(", ")
    return if (artists.size > maxItems) "$line +${artists.size - maxItems}" else line
}

/** Keeps a centred artist line inside a bounded, scrollable width in the cover headers. */
@Composable
internal fun CenteredSubtitleLine(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).widthIn(max = 340.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
