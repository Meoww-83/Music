package com.abhiram.music.ui.player

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhiram.music.BuildConfig
import com.abhiram.music.database.entities.FormatEntity
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.playback.PlayerState
import com.abhiram.music.utils.formatBytes
import com.abhiram.music.utils.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * “Stats for nerds” diagnostics panel of the player.
 *
 * Renders one **tappable row per fact** — the label on the left, the monospace value on the right
 * — and copies that single value when the row is pressed (the label flips to “Copied” for a
 * moment). The rows are:
 *
 * * **track**: title, artists, album, video id + `music.youtube.com` link;
 * * **playback**: position, duration, progress %, queue index / size, repeat, shuffle, sleep timer;
 * * **format** (resolved lazily from Room via `DatabaseDao.formatOf`): itag, mime type, bitrate,
 *   loudness, content length;
 * * **runtime**: cache bytes on disk (`cacheDir/exoplayer`), app version, Android release/API and
 *   device model.
 *
 * A **Copy report** button at the bottom puts every row (plus the raw format row) on the clipboard
 * as plain text. Nothing here owns playback: everything is read from [playerState], and the only
 * side effect is the clipboard.
 *
 * @param playerState snapshot of the player; the current track selects which format is shown.
 * When `null` (the default, so the component can be wired into `NavHost` with no arguments) the
 * live state of the frozen controller is used.
 * @param modifier sizing modifier — the host typically gives it `fillMaxWidth()` plus a max height.
 * @param onDismiss optional close callback (used when hosted in a sheet).
 */
@Composable
fun StatsForNerds(
    playerState: PlayerState? = null,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val state = playerState ?: rememberPlayerState()
    val context = LocalContext.current
    val current = state.current
    val songId = current?.id

    // Format row: resolved from the local DB (the resolver persists it when it picks a stream).
    val format by produceState<FormatEntity?>(initialValue = null, key1 = songId) {
        value = songId
            ?.takeIf { it.isNotEmpty() }
            ?.let { id -> runCatching { ServiceLocator.container.dao.formatOf(id) }.getOrNull() }
    }

    // Cache size on disk: measured off the main thread, only once per panel.
    val cacheBytes by produceState(initialValue = 0L) {
        value = runCatching {
            withContext(Dispatchers.IO) { directorySize(File(context.cacheDir, "exoplayer")) }
        }.getOrDefault(0L)
    }

    val rows = remember(current, state, format, cacheBytes) {
        buildRows(current, state, format, cacheBytes)
    }
    val report = remember(rows) {
        buildString {
            appendLine("Player stats")
            appendLine("------------")
            rows.forEach { appendLine("${it.label}: ${it.value}") }
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        /* --- header ------------------------------------------------------------- */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stats for nerds",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = "Tap any row to copy it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (onDismiss != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        /* --- rows --------------------------------------------------------------- */
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
        ) {
            rows.forEach { row ->
                StatRow(
                    label = row.label,
                    value = row.value,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        /* --- copy report -------------------------------------------------------- */
        Spacer(Modifier.height(10.dp))
        var reportCopied by remember { mutableStateOf(false) }
        LaunchedEffect(reportCopied) {
            if (reportCopied) {
                delay(1_400)
                reportCopied = false
            }
        }
        Button(
            onClick = {
                copyToClipboard(context, "Player stats", report)
                reportCopied = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 20.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (reportCopied) "Copied to clipboard" else "Copy report",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}

/* -------------------------------------------------------------------------------------------
 * Rows
 * ---------------------------------------------------------------------------------------- */

/** One diagnostic row: a short label and the value shown (and copied) for it. */
private data class StatRowData(val label: String, val value: String)

/** One tappable diagnostics row; pressing it copies [value] and flips the label to “Copied”. */
@Composable
private fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1_200)
            copied = false
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                copyToClipboard(context, label, value)
                copied = true
            }
            .padding(horizontal = 8.dp, vertical = 14.dp),
    ) {
        Text(
            text = if (copied) "Copied" else label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (copied) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(132.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Collects every diagnostic row shown in the panel (and copied by “Copy report”). */
private fun buildRows(
    current: MediaMetadata?,
    state: PlayerState,
    format: FormatEntity?,
    cacheBytes: Long,
): List<StatRowData> {
    val unknown = "—"
    val progress =
        if (state.durationMs > 0L) {
            ((state.positionMs * 100) / state.durationMs).toInt()
        } else {
            0
        }
    return listOfNotNull(
        StatRowData("Title", current?.title ?: unknown),
        StatRowData(
            "Artists",
            current?.artists?.joinToString(", ") { it.name }?.takeIf { it.isNotBlank() } ?: unknown,
        ),
        StatRowData("Album", current?.album?.title?.takeIf { it.isNotBlank() } ?: unknown),
        StatRowData("Video ID", current?.id ?: unknown),
        StatRowData("Link", current?.let { "https://music.youtube.com/watch?v=${it.id}" } ?: unknown),
        StatRowData("Position", state.positionMs.formatDuration()),
        StatRowData("Duration", state.durationMs.formatDuration()),
        StatRowData("Progress", "$progress %"),
        StatRowData(
            "Queue",
            if (state.queue.isEmpty()) unknown
            else "${state.currentIndex + 1} / ${state.queue.size}",
        ),
        StatRowData("Repeat", state.repeatMode.name),
        StatRowData("Shuffle", if (state.shuffleEnabled) "On" else "Off"),
        StatRowData(
            "Sleep timer",
            state.sleepTimerProgress?.let { "${(it * 100).toInt()} %" } ?: "Off",
        ),
        StatRowData("Itag", format?.itag?.toString() ?: unknown),
        StatRowData("Mime type", format?.mimeType ?: unknown),
        StatRowData("Bitrate", format?.bitrate?.let { "${it / 1000} kbps" } ?: unknown),
        StatRowData(
            "Loudness",
            format?.loudnessDb?.let { "%.1f dB".format(it) } ?: unknown,
        ),
        StatRowData("Content length", format?.contentLength?.formatBytes() ?: unknown),
        StatRowData("Cache on disk", if (cacheBytes > 0L) cacheBytes.formatBytes() else unknown),
        StatRowData("App version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
        StatRowData("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"),
        StatRowData("Device", "${Build.MANUFACTURER} ${Build.MODEL}"),
    )
}

/** Total size of a (possibly missing) directory tree, in bytes. */
private fun directorySize(directory: File): Long {
    if (!directory.exists()) return 0L
    val files = directory.listFiles() ?: return directory.length()
    var total = 0L
    for (file in files) {
        total += if (file.isDirectory) directorySize(file) else file.length()
    }
    return total
}
