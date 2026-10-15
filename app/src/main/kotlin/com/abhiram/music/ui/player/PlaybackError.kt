package com.abhiram.music.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhiram.music.BuildConfig
import com.abhiram.music.playback.PlaybackError as PlaybackFailure
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Playback failure card shown in place of the artwork on the full player.
 *
 * Every [com.abhiram.music.playback.PlaybackError] subtype gets its own icon, headline and
 * guidance text:
 *
 * | subtype | icon | headline |
 * |---|---|---|
 * | `Unplayable` | warning | “This track can’t be played” |
 * | `LoginRequired` | lock | “Sign in to play this track” |
 * | `VideoIdMismatch` | sync problem | “Track id mismatch” |
 * | `NoFormat` | error outline | “No playable format” |
 * | `Network` | wifi off | “No connection” |
 * | `Unknown` | error outline | “Playback failed” |
 *
 * Actions (each ≥48 dp, springs, no rigid easings):
 *
 * * **Retry** — calls [onRetry]; by default it re-prepares playback through
 *   `PlayerController.resume()` (which also clears the published error once the player leaves
 *   `STATE_IDLE`).
 * * **Skip** — calls [onNext]; by default `PlayerController.seekToNext()`.
 * * **Copy** — copies a short report (app/Android version, track id, subtype, raw message) so the
 *   user can paste it into a bug report.
 *
 * The track id (when [videoId] is present) is shown in monospace and is itself tappable to copy.
 *
 * @param error the frozen playback failure to present; defaults to a generic `Unknown(null)` so
 * the component can also be wired into `NavHost` (route `player/error`) with no arguments.
 * @param modifier sizing modifier — the player gives it `fillMaxWidth()` plus padding.
 * @param videoId id of the failing track, used in the report and shown under the message.
 * @param onRetry overrides the retry action (defaults to `PlayerController.resume()`).
 * @param onNext overrides the skip action (defaults to `PlayerController.seekToNext()`).
 */
@Composable
fun PlaybackError(
    error: PlaybackFailure = PlaybackFailure.Unknown(null),
    modifier: Modifier = Modifier,
    videoId: String? = null,
    onRetry: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
) {
    val controller = rememberPlayerController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    val headline = remember(error) { error.headline() }
    val guidance = remember(error) { error.guidance() }
    val detail = remember(error) { error.detail() }
    val report = remember(error, videoId) { buildErrorReport(error, videoId) }

    fun copyReport() {
        copyToClipboard(context, "Playback error", report)
        scope.launch {
            copied = true
            delay(1_400)
            copied = false
        }
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.08f),
        contentColor = Color.White,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Icon(
                imageVector = error.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(56.dp),
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = headline,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = guidance,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
            )

            if (detail != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    ),
                    color = Color.White.copy(alpha = 0.58f),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                )
            }

            if (!videoId.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                val idCopied = remember(videoId) { mutableStateOf(false) }
                val idScope = rememberCoroutineScope()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .springTap {
                            copyToClipboard(context, "Video id", videoId)
                            idScope.launch {
                                idCopied.value = true
                                delay(1_400)
                                idCopied.value = false
                            }
                        },
                ) {
                    Text(
                        text = if (idCopied.value) "Copied" else "ID  $videoId",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White.copy(alpha = 0.62f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ErrorAction(
                    label = "Retry",
                    icon = Icons.Rounded.Replay,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = { onRetry?.invoke() ?: controller.resume() },
                )
                ErrorAction(
                    label = "Skip",
                    icon = Icons.Rounded.SkipNext,
                    containerColor = Color.White.copy(alpha = 0.16f),
                    contentColor = Color.White,
                    onClick = { onNext?.invoke() ?: controller.seekToNext() },
                )
                ErrorAction(
                    label = if (copied) "Copied" else "Copy",
                    icon = Icons.Rounded.ContentCopy,
                    containerColor = Color.Transparent,
                    contentColor = Color.White.copy(alpha = 0.85f),
                    outlined = true,
                    onClick = { copyReport() },
                )
            }
        }
    }
}

/**
 * One ≥48 dp action of the error card: filled, translucent or outlined, all with a spring press.
 *
 * @param label short action label.
 * @param icon glyph shown before the label.
 * @param containerColor background colour.
 * @param contentColor label/glyph colour.
 * @param onClick action handler.
 * @param outlined draws a hairline border instead of a filled background.
 */
@Composable
private fun ErrorAction(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val reduceMotion = rememberReduceMotion()
    val shape = RoundedCornerShape(50)
    val borderModifier = if (outlined) {
        Modifier.border(width = 1.dp, color = Color.White.copy(alpha = 0.35f), shape = shape)
    } else {
        Modifier
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(containerColor)
            .then(borderModifier)
            .springPress(interactionSource = interactionSource, reduceMotion = reduceMotion)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

/* -------------------------------------------------------------------------------------------
 * Subtype presentation
 * ---------------------------------------------------------------------------------------- */

/** Headline of the failure card. */
private fun PlaybackFailure.headline(): String = when (this) {
    PlaybackFailure.Unplayable -> "This track can’t be played"
    PlaybackFailure.LoginRequired -> "Sign in to play this track"
    PlaybackFailure.VideoIdMismatch -> "Track id mismatch"
    PlaybackFailure.NoFormat -> "No playable format"
    is PlaybackFailure.Network -> "No connection"
    is PlaybackFailure.Unknown -> "Playback failed"
}

/** One line of actionable guidance shown under the headline. */
private fun PlaybackFailure.guidance(): String = when (this) {
    PlaybackFailure.Unplayable -> "The stream is unavailable right now. Skipping usually helps."
    PlaybackFailure.LoginRequired -> "Some tracks need an account. Try another version of this song."
    PlaybackFailure.VideoIdMismatch -> "The resolved video is not the one you picked — retry to re-resolve it."
    PlaybackFailure.NoFormat -> "No audio format could be resolved for this track."
    is PlaybackFailure.Network -> "Check your connection, then retry — playback resumes where it stopped."
    is PlaybackFailure.Unknown -> "Something went wrong while decoding this track."
}

/** Raw message of the failure, `null` when the subtype carries none. */
private fun PlaybackFailure.detail(): String? = when (this) {
    PlaybackFailure.Unplayable,
    PlaybackFailure.LoginRequired,
    PlaybackFailure.VideoIdMismatch,
    PlaybackFailure.NoFormat,
    -> null
    is PlaybackFailure.Network -> message?.takeIf { it.isNotBlank() }
    is PlaybackFailure.Unknown -> message?.takeIf { it.isNotBlank() }
}

/** Icon of the failure card. */
private fun PlaybackFailure.icon(): ImageVector = when (this) {
    PlaybackFailure.Unplayable -> Icons.Rounded.Warning
    PlaybackFailure.LoginRequired -> Icons.Rounded.Lock
    PlaybackFailure.VideoIdMismatch -> Icons.Rounded.SyncProblem
    PlaybackFailure.NoFormat -> Icons.Rounded.ErrorOutline
    is PlaybackFailure.Network -> Icons.Rounded.WifiOff
    is PlaybackFailure.Unknown -> Icons.Rounded.ErrorOutline
}

/* -------------------------------------------------------------------------------------------
 * Clipboard
 * ---------------------------------------------------------------------------------------- */

/** Plain-text clipboard write (platform API — no compose clipboard deprecations). */
internal fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

/** Short, shareable description of a playback failure for bug reports. */
private fun buildErrorReport(error: PlaybackFailure, videoId: String?): String =
    buildString {
        appendLine("Playback error report")
        appendLine("----------------------")
        appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine(
            "Error: ${error::class.simpleName ?: "Unknown"}" +
                (error.detail()?.let { " — $it" } ?: ""),
        )
        appendLine("Video id: ${videoId ?: "unknown"}")
        videoId?.let { appendLine("Link: https://music.youtube.com/watch?v=$it") }
    }
