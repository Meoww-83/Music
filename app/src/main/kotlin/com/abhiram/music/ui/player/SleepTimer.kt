package com.abhiram.music.ui.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.playback.PlayerState
import com.abhiram.music.ui.component.TextFieldDialog
import com.abhiram.music.utils.formatDuration

/**
 * Sleep-timer sheet/dialog content.
 *
 * Offers preset chips **15 / 30 / 45 / 60 minutes** plus a **custom** minute value
 * ([TextFieldDialog], clamped to 1–720), a start button that arms the timer through the frozen
 * `PlayerController.setSleepTimer(ms)`, and — while a timer is armed — a **live countdown** ring
 * driven by `PlayerState.sleepTimerProgress` (0f..1f remaining) with a cancel action.
 *
 * @param playerState snapshot of the player; `sleepTimerProgress` drives the countdown.
 * @param modifier sizing modifier.
 * @param onDismiss optional close callback (used when hosted in a sheet).
 */
@Composable
fun SleepTimer(
    playerState: PlayerState,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val controller = rememberPlayerController()
    val preferences = remember { ServiceLocator.container.preferences }
    val defaultMinutes by preferences.defaultSleepTimerMinutes.collectAsState(initial = 30)
    val persistedRemainingMs by preferences.sleepTimerRemainingMs.collectAsState(initial = null)

    val progress = playerState.sleepTimerProgress
    val armed = progress != null

    var selectedMinutes by remember { mutableIntStateOf(defaultMinutes) }
    var armedDurationMs by rememberSaveable { mutableLongStateOf(0L) }
    var showCustomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(defaultMinutes, armed) {
        if (!armed) selectedMinutes = defaultMinutes
    }

    val remainingMs: Long? = when {
        progress == null -> null
        persistedRemainingMs != null -> persistedRemainingMs
        armedDurationMs > 0L -> (progress * armedDurationMs).toLong()
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Bedtime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Sleep timer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            if (onDismiss != null) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }

        if (armed) {
            /* --- live countdown ---------------------------------------------------- */
            val animatedProgress by animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
                label = "sleepTimerRing",
            )
            // Resolved in composition: MaterialTheme is only readable from a composable scope.
            val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            val ringColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier.size(168.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val strokeWidth = 10.dp.toPx()
                    val inset = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = remainingMs?.formatDuration() ?: "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "Playback stops when the timer runs out.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Button(
                onClick = {
                    controller.setSleepTimer(null)
                    armedDurationMs = 0L
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Cancel timer", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            /* --- preset chips ------------------------------------------------------- */
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(15, 30, 45, 60).forEach { minutes ->
                    TimerChip(
                        label = "${minutes}m",
                        selected = selectedMinutes == minutes && !showCustomDialog,
                        onClick = { selectedMinutes = minutes },
                        modifier = Modifier.weight(1f),
                    )
                }
                TimerChip(
                    label = "Custom",
                    selected = showCustomDialog,
                    onClick = { showCustomDialog = true },
                    modifier = Modifier.weight(1.3f),
                )
            }

            Text(
                text = "Stop playback after $selectedMinutes minutes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Button(
                onClick = {
                    val minutes = selectedMinutes.coerceIn(1, 720)
                    val millis = minutes * 60_000L
                    controller.setSleepTimer(millis)
                    armedDurationMs = millis
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape,
            ) {
                Text("Start timer", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showCustomDialog) {
        TextFieldDialog(
            title = "Custom duration",
            value = selectedMinutes.toString(),
            onDismiss = { showCustomDialog = false },
            onConfirm = { input ->
                input.filter { it.isDigit() }.toIntOrNull()
                    ?.coerceIn(1, 720)
                    ?.let { selectedMinutes = it }
                showCustomDialog = false
            },
            placeholder = "Minutes (1–720)",
            label = "Minutes",
            singleLine = true,
        )
    }
}

/** Expressive pill chip used for the sleep-timer presets; ≥48 dp with a spring press. */
@Composable
private fun TimerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        interactionSource = interactionSource,
        modifier = modifier
            .height(48.dp)
            .springPress(interactionSource = interactionSource),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}
