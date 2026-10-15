package com.abhiram.music.ui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.ui.glass.LiquidGlassSurface
import com.abhiram.music.utils.GitHubUpdateChecker
import com.abhiram.music.utils.UpdateInfo
import kotlinx.coroutines.flow.first

/**
 * State of the automatic update check performed when the app opens.
 */
class UpdateCheckState internal constructor() {
    /** Non-null when a newer GitHub release is available and the user has not skipped it. */
    var info by mutableStateOf<UpdateInfo?>(null)
        internal set

    var visible by mutableStateOf(false)
        internal set

    /** Invoked by the dialog buttons. */
    var onDismiss: () -> Unit = {}
}

/**
 * Runs the GitHub release check once when the host enters composition.
 *
 * A release whose tag equals the version the user previously chose to skip ("Remind me later") is
 * not shown again until a newer version appears. Network failures are silent.
 */
@Composable
fun rememberUpdateCheckState(): UpdateCheckState {
    val state = remember { UpdateCheckState() }
    LaunchedEffect(Unit) {
        val preferences = ServiceLocator.container.preferences
        val skipped = runCatching { preferences.lastUpdateCheckSkippedVersion.first() }.getOrNull()
        val update = GitHubUpdateChecker.checkForUpdate()
        if (update != null && update.versionName != skipped) {
            state.info = update
            state.visible = true
        }
    }
    state.onDismiss = {
        state.visible = false
    }
    return state
}

/**
 * Renders the update notice described by [state].
 *
 * Design: a liquid-glass pop-up card (4-property formula — backdrop blur, translucent tint,
 * concentric border, deep shadow) carrying expressive, pill-shaped primary/secondary actions.
 * On API < 33 [LiquidGlassSurface] automatically falls back to the tinted surface variant.
 */
@Composable
fun UpdateDialog(
    state: UpdateCheckState,
    modifier: Modifier = Modifier,
) {
    val info = state.info ?: return
    AnimatedVisibility(
        visible = state.visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
            scaleIn(
                initialScale = 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier,
    ) {
        Dialog(
            onDismissRequest = { state.onDismiss() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            UpdateCardContent(
                info = info,
                onUpdate = { openUrl(info.htmlUrl) },
                onLater = {
                    // Persist the skip so the same release is not pushed again on next launch.
                    runCatching {
                        val preferences = ServiceLocator.container.preferences
                        preferences.setLastUpdateCheckSkippedVersion(info.versionName)
                    }
                    state.onDismiss()
                },
            )
        }
    }
}

@Composable
private fun UpdateCardContent(
    info: UpdateInfo,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        LiquidGlassSurface(
            shape = MaterialTheme.shapes.extraLarge,
            tint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
            borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f),
            elevation = 24.dp,
            blurRadius = 26.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.NewReleases,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "A new version is available",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Version ${info.versionName} is ready to install. You have ${com.abhiram.music.BuildConfig.VERSION_NAME} right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                if (!info.body.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = info.body.lines().take(4).joinToString("\n").take(320),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        textAlign = TextAlign.Start,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f))
                            .padding(12.dp),
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Expressive pill: secondary action — quiet, but ≥48dp.
                    ExpressivePillButton(
                        text = "Remind me later",
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        onClick = onLater,
                        modifier = Modifier.weight(1f),
                    )
                    // Expressive pill: primary action — high chroma hero moment.
                    ExpressivePillButton(
                        text = "Update",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = onUpdate,
                        icon = Icons.Rounded.SystemUpdate,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * Pill-shaped button with a spring "warp" press: the label/icon scales past its rest position and
 * eases back, giving the tactile overshoot required by the expressive motion rules.
 */
@Composable
private fun ExpressivePillButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pillPress",
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = border,
        interactionSource = interactionSource,
        modifier = modifier
            .height(52.dp)
            .scale(scale),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

private fun openUrl(url: String) {
    runCatching {
        // Context is obtained lazily so the dialog never needs a CompositionLocal provider.
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        UpdateHostContext?.startActivity(intent)
    }
}

/**
 * Set once by [com.abhiram.music.MainActivity] so update links can open a browser without a
 * CompositionLocal. Cleared when the activity is destroyed.
 */
internal var UpdateHostContext: Context? = null
