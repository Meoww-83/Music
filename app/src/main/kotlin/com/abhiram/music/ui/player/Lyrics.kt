package com.abhiram.music.ui.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.playback.PlayerState
import com.abhiram.music.ui.component.ShimmerHost
import com.abhiram.music.ui.component.TextPlaceholder
import com.abhiram.music.ui.component.TooltipIconButton

/**
 * Lyrics panel of the player.
 *
 * * **Synchronized LRC lyrics** — timestamps are parsed (`[mm:ss.xx]` / `[m:ss.xxx]`, multiple
 *   tags per line, metadata tags skipped), the active line is highlighted with a bouncy spring
 *   scale + colour transition, the list auto-scrolls (anchored ~1/3 from the top) and **tapping a
 *   line seeks** through [onSeek] (falling back to the controller).
 * * **Plain lyrics** — rendered as big, expressive, scrollable type.
 * * Empty state uses [TextPlaceholder], loading state shimmers via [ShimmerHost], a refresh button
 *   re-runs the fetch and the source credit line names the lyrics provider.
 *
 * Fetching goes through `ServiceLocator.container.musicRepository.lyrics(videoId)` (§3.4,
 * cache-first) inside `produceState`, keyed by the current song — playback state itself is only
 * read from [playerState].
 *
 * @param playerState snapshot of the player; the current track's id selects the lyrics.
 * @param modifier sizing modifier.
 * @param onSeek invoked with a timestamp in ms when a synchronized line is tapped; defaults to
 * `PlayerController.seekTo`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Lyrics(
    playerState: PlayerState,
    modifier: Modifier = Modifier,
    onSeek: ((Long) -> Unit)? = null,
) {
    val controller = rememberPlayerController()
    val reduceMotion = rememberReduceMotion()
    val videoId = playerState.current?.id.orEmpty()
    var revision by remember(videoId) { mutableIntStateOf(0) }

    val ui by produceState<LyricsUi>(initialValue = LyricsUi.Loading, videoId, revision) {
        value = LyricsUi.Loading
        if (videoId.isEmpty()) {
            value = LyricsUi.Empty
            return@produceState
        }
        val entity = runCatching {
            ServiceLocator.container.musicRepository.lyrics(videoId)
        }.getOrNull()
        value = if (entity == null ||
            (entity.plainLyrics.isNullOrBlank() && entity.syncedLyrics.isNullOrBlank())
        ) {
            LyricsUi.Empty
        } else {
            LyricsUi.Loaded(
                plain = entity.plainLyrics,
                synced = entity.syncedLyrics,
                source = entity.source,
            )
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        when (val current = ui) {
            LyricsUi.Loading -> {
                ShimmerHost(modifier = Modifier.fillMaxSize()) {
                    Column {
                        Spacer(Modifier.height(72.dp))
                        repeat(8) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 10.dp),
                            ) {
                                TextPlaceholder(text = "…")
                            }
                        }
                    }
                }
            }

            LyricsUi.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TextPlaceholder(text = "No lyrics found for this song")
                }
            }

            is LyricsUi.Loaded -> {
                val syncedLines = remember(current.synced) {
                    current.synced?.let { parseSyncedLyrics(it) }.orEmpty()
                }
                val plainLines = remember(current.plain) {
                    current.plain?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
                }

                Box(Modifier.fillMaxSize()) {
                    if (syncedLines.isNotEmpty()) {
                        SyncedLyrics(
                            lines = syncedLines,
                            positionMs = playerState.positionMs,
                            controllerSeek = { target -> controller.seekTo(target) },
                            onSeek = onSeek,
                            reduceMotion = reduceMotion,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (plainLines.isNotEmpty()) {
                        PlainLyrics(lines = plainLines, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            TextPlaceholder(text = "No lyrics found for this song")
                        }
                    }

                    // Refresh: bump the revision so `produceState` re-runs the fetch.
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    ) {
                        TooltipIconButton(
                            contentDescription = "Refresh lyrics",
                            onClick = { revision++ },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    val source = current.source
                    if (!source.isNullOrBlank()) {
                        Text(
                            text = "Lyrics from $source",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------------------------
 * Rendering
 * ---------------------------------------------------------------------------------------- */

/** Big expressive type for unsynchronized lyrics; the whole block is scrollable. */
@Composable
private fun PlainLyrics(
    lines: List<String>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        state = rememberLazyListState(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 64.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(lines) { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.headlineSmall,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Synchronized lyrics: highlight + auto-scroll + tap-to-seek. */
@Composable
private fun SyncedLyrics(
    lines: List<LyricLine>,
    positionMs: Long,
    controllerSeek: (Long) -> Unit,
    onSeek: ((Long) -> Unit)?,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val activeIndex = remember(positionMs, lines) {
        lines.indexOfLast { it.timeMs <= positionMs }
    }

    // Auto-scroll follows the active line, but never fights a scroll the user is performing.
    LaunchedEffect(activeIndex, lines) {
        if (activeIndex >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        // Top padding anchors the active line about a third down the panel.
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 120.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(
            items = lines,
            key = { index, line -> "$index|${line.timeMs}|${line.text}" },
        ) { index, line ->
            val isActive = index == activeIndex
            val scale by animateFloatAsState(
                targetValue = if (isActive && !reduceMotion) 1.06f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "lyricsLineScale",
            )
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.42f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "lyricsLineAlpha",
            )
            Text(
                text = line.text,
                style = MaterialTheme.typography.headlineSmall,
                lineHeight = 30.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .clickable {
                        val target = line.timeMs.coerceAtLeast(0L)
                        if (onSeek != null) onSeek(target) else controllerSeek(target)
                    }
                    .padding(vertical = 4.dp),
            )
        }
    }
}

/* -------------------------------------------------------------------------------------------
 * Loading + LRC parsing
 * ---------------------------------------------------------------------------------------- */

/** One timed (or plain) lyric line. */
private data class LyricLine(val timeMs: Long, val text: String)

/** State machine of the lyrics fetch. */
private sealed interface LyricsUi {
    data object Loading : LyricsUi

    data object Empty : LyricsUi

    data class Loaded(val plain: String?, val synced: String?, val source: String?) : LyricsUi
}

/** Matches the numeric content of an LRC timestamp tag: `mm`, `ss`, optional `[.:]fff`. */
private val LrcTimestamp = Regex("""(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?""")

/**
 * Parses synchronized LRC lyrics into a time-ordered list.
 *
 * Handles multiple timestamps per line (`[00:12.00][00:48.5]chorus`), 2-digit centiseconds and
 * 3-digit milliseconds, and skips metadata tags such as `[ar:Artist]`. Unparsable input yields an
 * empty list, which makes the panel fall back to plain lyrics.
 */
private fun parseSyncedLyrics(raw: String): List<LyricLine> {
    val result = ArrayList<LyricLine>()
    for (rawLine in raw.lineSequence()) {
        var rest = rawLine.trim()
        if (!rest.startsWith("[")) continue
        val stamps = ArrayList<Long>()
        while (rest.startsWith("[")) {
            val end = rest.indexOf(']')
            if (end <= 1) break
            val match = LrcTimestamp.matchEntire(rest.substring(1, end)) ?: break
            val minutes = match.groupValues[1].toLongOrNull() ?: break
            val seconds = match.groupValues[2].toLongOrNull() ?: break
            val fraction = match.groupValues[3]
            val millis = when {
                fraction.isEmpty() -> 0L
                fraction.length == 1 -> (fraction.toLongOrNull() ?: 0L) * 100L
                fraction.length == 2 -> (fraction.toLongOrNull() ?: 0L) * 10L
                else -> fraction.substring(0, 3).toLongOrNull() ?: 0L
            }
            stamps += minutes * 60_000L + seconds * 1_000L + millis
            rest = rest.substring(end + 1)
        }
        if (stamps.isEmpty()) continue
        val text = rest.trim()
        if (text.isEmpty()) continue
        for (stamp in stamps) {
            result += LyricLine(timeMs = stamp, text = text)
        }
    }
    return result.sortedBy { it.timeMs }
}
