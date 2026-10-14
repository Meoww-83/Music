package com.abhiram.music.ui.menu

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A single row inside a media-item menu (owner CMP-B).
 *
 * Every entry is at least 56 dp tall (well above the 48 dp minimum target) and carries an
 * expressive press state: the row scales down on a medium-bouncy spring while a translucent
 * highlight brightens behind it, then springs back on release. Entries also spring *in* when
 * their sheet opens — staggered by their composition order via [LocalMenuStagger] so a long
 * menu cascades into place like the rest of the Expressive motion language. Both animations
 * collapse to instant snaps when the system animator scale is 0 (global reduce-motion rule).
 *
 * @param title Primary label of the entry. Rendered in high-contrast `onSurface`.
 * @param modifier Modifier applied to the row.
 * @param icon Optional leading icon (24 dp), tinted with the entry's content colour.
 * @param subtitle Optional secondary line (artist list, description...).
 * @param trailing Optional trailing slot (duration, badge, chevron...).
 * @param enabled False renders the entry dimmed and non-clickable.
 * @param destructive True tints the entry with [androidx.compose.material3.ColorScheme.error]
 * for irreversible actions (remove, clear, delete).
 * @param onClick Invoked on tap.
 */
@Composable
fun MenuEntry(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Expressive press state: springy scale + a highlight that brightens the row while held.
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "menuEntryPress",
    )
    val highlight by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "menuEntryHighlight",
    )

    // Staggered spring-in: newly composed entries fade + slide up in cascade order.
    val staggerIndex = LocalMenuStagger.current?.let { clock -> remember { clock.claimIndex() } } ?: 0
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(staggerIndex) {
        delay(staggerIndex * STAGGER_STEP_MS)
        appeared = true
    }
    val enterProgress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "menuEntryEnter",
    )

    val contentColor = when {
        !enabled -> LocalContentColor.current.copy(alpha = 0.38f)
        destructive -> MaterialTheme.colorScheme.error
        else -> LocalContentColor.current
    }
    val highlightColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val background = remember(highlightColor) { highlightColor.copy(alpha = 0.10f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(background.copy(alpha = background.alpha * highlight))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .graphicsLayer {
                val enter = enterProgress.coerceIn(0f, 1f)
                alpha = enter
                translationY = (1f - enter) * 36f
                scaleX = pressScale
                scaleY = pressScale
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.let {
            Spacer(modifier = Modifier.width(8.dp))
            it()
        }
    }
}

/**
 * Claim-order clock used to stagger menu entries as they spring in. Owned (and reset) by
 * [BaseMediaItemMenu]; [MenuEntry] claims one index per instance, clamped so late panes never
 * wait more than a few hundred milliseconds.
 */
internal class MenuStaggerClock {
    private var next: Int = 0

    fun claimIndex(): Int = next++.coerceAtMost(MAX_STAGGER_INDEX)
}

/** Longest cascade delay = [MAX_STAGGER_INDEX] * [STAGGER_STEP_MS] ≈ 210 ms. */
private const val MAX_STAGGER_INDEX: Int = 6

private const val STAGGER_STEP_MS: Long = 35L

/** Current sheet's stagger clock; `null` outside a [BaseMediaItemMenu] (entries appear instantly). */
internal val LocalMenuStagger = androidx.compose.runtime.staticCompositionLocalOf<MenuStaggerClock?> { null }
