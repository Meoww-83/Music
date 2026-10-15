package com.abhiram.music.ui.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies the liquid-glass formula to any element in modifier form — the lightweight counterpart
 * of [LiquidGlassSurface].
 *
 * The modifier layers the four properties around the host's own drawing:
 *
 *  1. **Deep backdrop blur** — on API 33+ (with a hosted [GlassBackdrop] and [enabled]) the layer
 *     the modifier creates is frosted with a platform blur RenderEffect, so everything the host
 *     element draws *beneath the tint* — typically a backdrop rendition or decorative graphics —
 *     is blurred like real glass. Draw crisp foreground labels in a **sibling drawn after** this
 *     modifier (or use [LiquidGlassSurface], which composes and blurs its own backdrop sample
 *     while keeping the content sharp).
 *  2. **Translucent tint** — [glassTintBrush] plus the [GlassGlossBrush] specular rim, drawn
 *     under the frosted layer so the tint itself stays clean.
 *  3. **Concentric border** — [borderColor]/[borderWidth] inset by one border width and drawn on
 *     top of the host content ([drawGlassBorder]).
 *  4. **Deep shadow** — [elevation] with [GlassShadowAmbient]/[GlassShadowSpot].
 *
 * Below API 33, with animations disabled (reduce motion), with [enabled] `false`, or without a
 * [GlassBackdrop], property 1 degrades to the tinted pane — the same fallback
 * [LiquidGlassSurface] uses, so Android 8 keeps the full visual identity. Pressing the element
 * warps and brightens it with a spring, honouring reduce motion.
 *
 * The default [shape] is the fully rounded pill, expressed as `RoundedCornerShape(percent = 50)`
 * (the exact geometry of `MaterialTheme.expressiveShapes.pill`) because this function is not
 * composable and therefore cannot read the theme's `@Composable` shape accessors.
 *
 * @param shape outline of the pane; drives clip, tint, border and shadow.
 * @param tint translucent pane tint (property 2).
 * @param borderColor colour of the concentric border (property 3).
 * @param borderWidth width of the concentric border, also its inset.
 * @param elevation depth of the floating shadow (property 4).
 * @param blurRadius radius of the backdrop blur (property 1).
 * @param enabled set `false` to force the tinted-pane fallback regardless of device capability.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(percent = 50),
    tint: Color = Color.White.copy(alpha = 0.10f),
    borderColor: Color = Color.White.copy(alpha = 0.20f),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 18.dp,
    blurRadius: Dp = 24.dp,
    enabled: Boolean = GlassBackdropSupported,
): Modifier = composed {
    // Property 1 needs a registered backdrop and must not re-enter a backdrop sample.
    val active = enabled && GlassBackdropSupported &&
        LocalGlassBackdrop.current != null && !LocalGlassSampling.current
    val press = rememberGlassPress()

    val blurPx = with(LocalDensity.current) { blurRadius.toPx() }
    val blurEffect: RenderEffect? = remember(active, blurPx) {
        if (active && blurPx > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.graphics.RenderEffect
                .createBlurEffect(blurPx, blurPx, android.graphics.Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        } else {
            null
        }
    }

    this
        .then(press.modifier) // press warp (property interaction)
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = GlassShadowAmbient,
            spotColor = GlassShadowSpot,
        ) // property 4: deep shadow
        .clip(shape) // bounds tint, frosted content and border to the outline
        .background(glassTintBrush(tint), shape) // property 2: translucent tint (bottom)
        .background(GlassGlossBrush, shape) // property 2: specular gloss
        .drawWithContent {
            drawContent()
            // property 3: concentric border, crisp on top of the frosted content
            drawGlassBorder(borderColor, shape, borderWidth)
        }
        .then(
            // property 1: deep backdrop blur — frosts the host's own pane rendition
            if (blurEffect != null) {
                Modifier.graphicsLayer { renderEffect = blurEffect }
            } else {
                Modifier
            },
        )
}
