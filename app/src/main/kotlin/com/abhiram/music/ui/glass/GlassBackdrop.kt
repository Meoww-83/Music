package com.abhiram.music.ui.glass

import android.animation.ValueAnimator
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.abhiram.music.ui.theme.LocalReduceMotion

/*
 * Liquid-glass rendering layer (design spec §"Liquid Glass").
 *
 * The whole effect is built from one strict four-property formula that every glass element obeys:
 *
 *   1. **Deep backdrop blur**   – the content registered by [GlassBackdrop] is re-rendered,
 *                                 clipped to the glass outline and blurred with a platform
 *                                 `RenderEffect` (API 33+ only).
 *   2. **Translucent tint**     – a subtle vertical tint brush plus a specular gloss layer gives
 *                                 the pane depth without hiding the backdrop behind it.
 *   3. **Concentric border**    – a hairline stroke inset by exactly one border width from the
 *                                 outline, drawn on top of everything (see [drawGlassBorder]).
 *   4. **Deep shadow**          – an ambient/spot elevation shadow ([GlassShadowAmbient] /
 *                                 [GlassShadowSpot]) lifts the pane off the page.
 *
 * Below API 33, or when animations are disabled (reduce motion), or when no backdrop has been
 * registered, property 1 degrades to the plain tinted pane — properties 2–4 keep the glass
 * identity, so the app stays fully usable on Android 8.
 */

/* *********************************************************************************************
 * Capability gate
 * **************************************************************************************** */

/**
 * Whether the full liquid-glass treatment (the backdrop blur of property 1) can run here.
 *
 * True on Android 13+ (API 33, where the platform blur [android.graphics.RenderEffect] used by
 * the glass layer is guaranteed) **and** while system animations are enabled. When the user or an
 * accessibility service disables animations (reduce motion), or the device is simply too old,
 * [LiquidGlassSurface] and [Modifier.liquidGlass] fall back to the tinted-pane variant.
 */
val GlassBackdropSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ValueAnimator.areAnimatorsEnabled()

/* *********************************************************************************************
 * Backdrop recording (composition locals)
 * **************************************************************************************** */

/**
 * Window-space metrics of the [GlassBackdrop]; glass surfaces subtract their own position from
 * these so the re-rendered backdrop sample lines up pixel-for-pixel with the real one.
 */
internal class GlassBackdropMetrics {
    /** Size of the backdrop layer in pixels. */
    var sizePx: IntSize
        by mutableStateOf(IntSize.Zero)

    /** Top-left corner of the backdrop layer in window space, in pixels. */
    var originPx: Offset
        by mutableStateOf(Offset.Zero)
}

/**
 * The full-screen backdrop content registered by [GlassBackdrop], so that [LiquidGlassSurface]
 * can re-render it clipped + blurred inside its own bounds — the "sample the backdrop" trick that
 * gives the glass its liquid look without any root-level graphics capture.
 */
internal val LocalGlassBackdrop = staticCompositionLocalOf<(@Composable () -> Unit)?>(null)

/** Metrics of the registered backdrop; `null` when no [GlassBackdrop] is hosted. */
internal val LocalGlassBackdropMetrics = staticCompositionLocalOf<GlassBackdropMetrics?>(null)

/**
 * Set to `true` while a glass surface is rendering a backdrop *sample*. Nested glass surfaces
 * inside a sample see this flag and skip their own sampling, which both bounds the recursion to
 * a single extra level and keeps the sampled rendition cheap.
 */
internal val LocalGlassSampling = staticCompositionLocalOf { false }

/* *********************************************************************************************
 * Shared glass internals (visual constants + drawing helpers)
 * **************************************************************************************** */

/** Ambient component of the deep glass shadow (property 4). */
internal val GlassShadowAmbient: Color = Color(0x59000000)

/** Spotlight component of the deep glass shadow (property 4). */
internal val GlassShadowSpot: Color = Color(0x8A000000)

/** Specular gloss brushed over the tint (property 2): a bright rim at the top, fading down. */
internal val GlassGlossBrush: Brush = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.10f),
    0.28f to Color.White.copy(alpha = 0.03f),
    1f to Color.Transparent,
)

/** Scale-down applied to a glass pane while it is held down (press warp). */
internal const val GlassPressScale: Float = 0.015f

/** Brightening white overlay shown on a pane while it is held down. */
internal const val GlassPressHighlightAlpha: Float = 0.05f

/**
 * The blurred backdrop sample is rendered [GlassBlurPadFactor]×blur-radius larger than the pane
 * on every side, so the CLAMP-tile smearing of the platform blur happens outside the visible
 * outline instead of at its edges.
 */
internal const val GlassBlurPadFactor: Float = 2.5f

/**
 * Property 2 as a brush: the pane tint, marginally denser at the top and lighter at the bottom so
 * the pane reads as a slightly curved piece of glass rather than a flat overlay.
 */
internal fun glassTintBrush(tint: Color): Brush = Brush.verticalGradient(
    tint.copy(alpha = (tint.alpha * 1.15f).coerceAtMost(1f)),
    tint,
    tint.copy(alpha = tint.alpha * 0.85f),
)

/**
 * Property 3: draws the concentric glass border — a stroke of [borderWidth] whose centre line is
 * inset by exactly [borderWidth] from the outline of [shape], so the ring sits fully inside the
 * pane edge and mirrors it at any corner radius. Rounded/rect outlines are inset analytically;
 * generic (path) outlines fall back to a centre stroke that the pane clip trims to the inside.
 */
internal fun DrawScope.drawGlassBorder(color: Color, shape: Shape, borderWidth: Dp) {
    if (borderWidth <= 0.dp) return
    val inset = borderWidth.toPx()
    val stroke = Stroke(inset)
    when (val outline = shape.createOutline(size, layoutDirection, this)) {
        is Outline.Rounded -> {
            val rect = outline.roundRect
            drawRoundRect(
                color = color,
                topLeft = Offset(rect.left + inset, rect.top + inset),
                size = Size(
                    (rect.width - 2f * inset).coerceAtLeast(0f),
                    (rect.height - 2f * inset).coerceAtLeast(0f),
                ),
                cornerRadius = CornerRadius(
                    (rect.cornerRadii.x - inset).coerceAtLeast(0f),
                    (rect.cornerRadii.y - inset).coerceAtLeast(0f),
                ),
                style = stroke,
            )
        }

        is Outline.Rectangle -> {
            drawRect(
                color = color,
                topLeft = Offset(outline.rect.left + inset, outline.rect.top + inset),
                size = Size(
                    (outline.rect.width - 2f * inset).coerceAtLeast(0f),
                    (outline.rect.height - 2f * inset).coerceAtLeast(0f),
                ),
                style = stroke,
            )
        }

        is Outline.Generic -> {
            // Path outlines cannot be inset analytically; the pane clip trims the outer half.
            drawPath(outline.path, color = color, style = stroke)
        }
    }
}

/**
 * Reports whether reduce-motion is active: either the closest [ExpressiveTheme] via the theme's
 * [LocalReduceMotion], or the platform animator-duration scale sitting at 0 (read directly as a
 * fallback for hosts composed without the theme). Glass honours it by dropping the press warp and
 * by folding into the tinted-pane fallback through [GlassBackdropSupported].
 */
@Composable
internal fun rememberGlassReduceMotion(): Boolean {
    val themeReduceMotion = LocalReduceMotion.current
    val context = LocalContext.current
    val systemReduceMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
    return themeReduceMotion || systemReduceMotion
}

/* *********************************************************************************************
 * The backdrop layer
 * **************************************************************************************** */

/**
 * Full-screen background + content layer that records itself for the liquid-glass surfaces.
 *
 * [MainActivity] wraps the whole app in this composable; it renders [content] normally **and**
 * publishes it through [LocalGlassBackdrop] together with its window-space metrics. Whenever a
 * [LiquidGlassSurface] is drawn on top, it re-renders the recorded content clipped to its own
 * outline and applies the platform blur — so the pane shows a frosted, live view of whatever is
 * actually behind it, with no root-level graphics capture.
 *
 * Below API 33 (or with animations disabled) glass surfaces ignore the backdrop entirely and fall
 * back to the tinted pane; [GlassBackdrop] then costs nothing beyond the metrics bookkeeping.
 *
 * @param modifier sizing/positioning modifier; defaults to filling the window.
 * @param content the app background + UI that glass surfaces should be able to sample.
 */
@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val metrics = remember { GlassBackdropMetrics() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                metrics.sizePx = coordinates.size
                metrics.originPx = coordinates.positionInWindow()
            },
    ) {
        CompositionLocalProvider(
            LocalGlassBackdrop provides content,
            LocalGlassBackdropMetrics provides metrics,
        ) {
            content()
        }
    }
}
