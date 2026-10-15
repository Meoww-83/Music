package com.abhiram.music.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Scale applied to the icon while the button is held down. */
private const val TooltipPressedScale = 0.88f

/**
 * A ≥48 dp [IconButton] that exposes a [contentDescription] and springs on press.
 *
 * Call sites draw their icon with `contentDescription = null` and provide the label here, so the
 * merged accessibility node of the button reads exactly [contentDescription] — no duplicate or
 * missing announcement. The pressed state is observed through the button's own
 * [interactionSource] (rather than a competing pointer handler), which keeps nested clickables
 * and list scrolling untouched.
 *
 * Motion: a bouncy spring drives the press-scale, so taps feel lively without a rigid easing;
 * [enabled] = `false` gives the standard disabled visuals and never reports a press.
 *
 * @param contentDescription accessibility label merged into the button's semantics.
 * @param onClick invoked on tap.
 * @param modifier sizing/positioning modifier (honoured first; e.g. `Modifier.size(48.dp)`).
 * @param enabled `false` renders the disabled state and suppresses the press feedback.
 * @param content the icon/visual, drawn unscaled at rest and scaled as a whole while pressed.
 */
@Composable
fun TooltipIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) TooltipPressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "tooltipIconButtonScale",
    )

    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .semantics { this.contentDescription = contentDescription },
        ) {
            content()
        }
    }
}
