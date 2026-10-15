package com.abhiram.music.ui.glass

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.abhiram.music.ui.theme.ExpressiveShapes
import kotlin.math.ceil
import kotlin.math.roundToInt

/* *********************************************************************************************
 * Press interaction
 * **************************************************************************************** */

/**
 * Spring-animated press feedback of a glass pane: the whole pane warps down by
 * [GlassPressScale] and a white highlight fades in while the pointer is held anywhere inside it
 * (including on interactive children). Everything snaps to rest under reduce motion.
 */
internal class GlassPressState internal constructor(
    /** Animated press amount: `0f` at rest, `1f` while held down. */
    val press: State<Float>,
    /** Tracker chain — gesture detection plus the press-warp graphics layer. */
    val modifier: Modifier,
)

/**
 * Remembers the shared press state of a glass pane. The pointer is observed on the pane itself so
 * that nested clickable children ([com.abhiram.music.ui.player.ControlCircle] etc.) still light
 * the glass up around them.
 */
@Composable
internal fun rememberGlassPress(): GlassPressState {
    val reduceMotion = rememberGlassReduceMotion()
    var held by remember { mutableStateOf(false) }
    val press = animateFloatAsState(
        targetValue = if (held && !reduceMotion) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "liquidGlassPress",
    )
    val tracker = Modifier
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                held = true
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.isEmpty() || event.changes.none { it.pressed }) break
                    }
                } finally {
                    held = false
                }
            }
        }
        .graphicsLayer {
            val amount = press.value
            scaleX = 1f - GlassPressScale * amount
            scaleY = 1f - GlassPressScale * amount
        }
    return remember(press, tracker) { GlassPressState(press, tracker) }
}

/* *********************************************************************************************
 * The liquid-glass surface
 * **************************************************************************************** */

/**
 * A floating pane of liquid glass — the only place the glass treatment is allowed to appear.
 *
 * The pane is assembled from the strict four-property formula:
 *
 *  1. **Deep backdrop blur** — on API 33+, when a [GlassBackdrop] is hosted and [enabled], the
 *     backdrop content is re-rendered inside the pane, aligned window-for-window, clipped to
 *     [shape] and frosted with a platform blur RenderEffect (rendered oversized so the blur
 *     smears outside the visible outline).
 *  2. **Translucent tint** — [glassTintBrush] plus the [GlassGlossBrush] specular rim.
 *  3. **Concentric border** — [borderColor]/[borderWidth] drawn inset by one border width
 *     ([drawGlassBorder]).
 *  4. **Deep shadow** — [elevation] elevation with [GlassShadowAmbient]/[GlassShadowSpot].
 *
 * On API < 33, with animations disabled (reduce motion), with [enabled] `false`, or when no
 * [GlassBackdrop] is present, property 1 degrades to the plain tinted pane — the app remains
 * fully usable on Android 8. Pressing anywhere in the pane warps and brightens it with a spring.
 *
 * @param modifier sizing/positioning modifier (`fillMaxWidth().height(64.dp)` style).
 * @param shape outline of the pane; also drives clip, border and shadow.
 * @param tint translucent pane tint (property 2).
 * @param borderColor colour of the concentric border (property 3).
 * @param borderWidth width of the concentric border, also its inset.
 * @param elevation depth of the floating shadow (property 4).
 * @param blurRadius radius of the backdrop blur (property 1).
 * @param enabled set `false` to force the tinted-pane fallback regardless of device capability.
 * @param contentAlignment alignment of [content] inside the pane.
 * @param content the pane's foreground; kept crisp above the blurred backdrop sample.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = ExpressiveShapes.pill,
    tint: Color = Color.White.copy(alpha = 0.10f),
    borderColor: Color = Color.White.copy(alpha = 0.20f),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 18.dp,
    blurRadius: Dp = 24.dp,
    enabled: Boolean = GlassBackdropSupported,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdropContent = LocalGlassBackdrop.current
    val backdropMetrics = LocalGlassBackdropMetrics.current
    val sampling = LocalGlassSampling.current
    val press = rememberGlassPress()

    // Property 1 needs a registered backdrop, a capable device, and must not re-enter its own
    // sample (recursion guard) — everything else works everywhere.
    val glassActive = enabled && GlassBackdropSupported && !sampling &&
        backdropContent != null && backdropMetrics != null
    val sampleContent = if (glassActive) backdropContent else null

    val blurPx = with(LocalDensity.current) { blurRadius.toPx() }
    val blurEffect: RenderEffect? = remember(glassActive, blurPx) {
        if (glassActive && blurPx > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.graphics.RenderEffect
                .createBlurEffect(blurPx, blurPx, android.graphics.Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        } else {
            null
        }
    }
    var glassPosition by remember { mutableStateOf(Offset.Zero) }
    var glassSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { glassPosition = it.positionInWindow() }
            .onSizeChanged { glassSize = it }
            .then(press.modifier) // press warp (property interaction)
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = GlassShadowAmbient,
                spotColor = GlassShadowSpot,
            ) // property 4: deep shadow
            .clip(shape), // bounds every following draw (sample, tint, border) to the outline
    ) {
        // ── Property 1: deep backdrop blur ──────────────────────────────────────────────────
        if (blurEffect != null && sampleContent != null && backdropMetrics != null) {
            val backdropSize = backdropMetrics.sizePx
            if (backdropSize != IntSize.Zero && glassSize != IntSize.Zero) {
                val padPx = ceil(blurPx * GlassBlurPadFactor)
                val padInt = padPx.roundToInt()
                val origin = backdropMetrics.originPx
                val backdrop = sampleContent
                Box(
                    // The blur layer is pad-px larger than the pane on every side, so CLAMP
                    // smearing lands outside the clip and the outline stays razor sharp.
                    Modifier
                        .offset { IntOffset(-padInt, -padInt) }
                        .size(IntSize(glassSize.width + 2 * padInt, glassSize.height + 2 * padInt))
                        .graphicsLayer {
                            clipToBounds = true
                            renderEffect = blurEffect
                        },
                ) {
                    CompositionLocalProvider(LocalGlassSampling provides true) {
                        Box(
                            // Window-space alignment: the sampled backdrop is shifted so that
                            // pixel (x, y) of the real backdrop lands on pixel (x, y) of the
                            // sample, relative to this pane's position in the window.
                            Modifier
                                .offset {
                                    IntOffset(
                                        (origin.x - glassPosition.x + padPx).roundToInt(),
                                        (origin.y - glassPosition.y + padPx).roundToInt(),
                                    )
                                }
                                .size(backdropSize),
                        ) {
                            backdrop()
                        }
                    }
                }
            }
        }

        // ── Property 2: translucent tint (+ specular gloss for depth) ───────────────────────
        val tintBrush = remember(tint) { glassTintBrush(tint) }
        Box(
            Modifier
                .matchParentSize()
                .background(tintBrush, shape)
                .background(GlassGlossBrush, shape),
        )

        // Press brighten: white highlight spring-fading in on top of the tint.
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = GlassPressHighlightAlpha * press.press.value }
                .background(Color.White, shape),
        )

        // Foreground stays crisp above the frosted backdrop.
        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = contentAlignment,
            content = content,
        )

        // ── Property 3: concentric border, drawn on top of the foreground ───────────────────
        Box(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    onDrawBehind {
                        drawGlassBorder(borderColor, shape, borderWidth)
                    }
                },
        )
    }
}

/* *********************************************************************************************
 * Floating toolbar
 * **************************************************************************************** */

/**
 * Floating, edge-centred glass toolbar: a [LiquidGlassSurface] pill that hosts a row of actions.
 *
 * Place it inside a `Box` with `Modifier.align(Alignment.BottomCenter)` (or any edge alignment);
 * the bar itself insets from the navigation bar and the screen edges so it "floats" above the
 * bottom of the display. Uses the same four-property glass formula — and the same API < 33
 * fallback — as [LiquidGlassSurface].
 *
 * @param modifier positioning/sizing modifier; typically an `align(...)` from the host `Box`.
 * @param shape outline of the bar (a pill by default).
 * @param tint translucent pane tint (property 2).
 * @param borderColor colour of the concentric border (property 3).
 * @param borderWidth width of the concentric border.
 * @param elevation depth of the floating shadow (property 4).
 * @param blurRadius radius of the backdrop blur (property 1).
 * @param enabled set `false` to force the tinted-pane fallback.
 * @param content the toolbar's actions, laid out in a centred row.
 */
@Composable
fun LiquidGlassTopBar(
    modifier: Modifier = Modifier,
    shape: Shape = ExpressiveShapes.pill,
    tint: Color = Color.White.copy(alpha = 0.10f),
    borderColor: Color = Color.White.copy(alpha = 0.20f),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 18.dp,
    blurRadius: Dp = 24.dp,
    enabled: Boolean = GlassBackdropSupported,
    content: @Composable RowScope.() -> Unit,
) {
    LiquidGlassSurface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = shape,
        tint = tint,
        borderColor = borderColor,
        borderWidth = borderWidth,
        elevation = elevation,
        blurRadius = blurRadius,
        enabled = enabled,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            content = content,
        )
    }
}
