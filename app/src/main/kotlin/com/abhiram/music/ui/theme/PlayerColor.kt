package com.abhiram.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.materialkolor.ktx.harmonize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * A set of colours sampled from a piece of album artwork, ready to drive the dynamic
 * full-screen gradient behind the player (design language §9: artwork colour on top, pure
 * black below).
 *
 * Every value is harmonised toward the active theme's primary with MaterialKolor, so artwork
 * of any hue still feels like it belongs to the app, and [onContainer] is picked by WCAG
 * contrast so labels drawn on [container] stay legible (≥ 4.5:1 where the maths allows).
 *
 * @property primary Dominant vivid colour of the artwork — the head of the gradient.
 * @property accent Secondary artwork colour, one step down the gradient.
 * @property container Deep artwork colour that the gradient fades through on its way to black.
 * @property onContainer Readable foreground (near-black or white) for text on [container].
 */
@Immutable
data class ArtworkColors(
    val primary: Color,
    val accent: Color,
    val container: Color,
    val onContainer: Color,
) {
    companion object {

        /** Neutral placeholder used before artwork arrives, or when it fails to load. */
        val Fallback: ArtworkColors = ArtworkColors(
            primary = Color(0xFF6E6E7A),
            accent = Color(0xFF4B4B55),
            container = Color(0xFF14141A),
            onContainer = Color.White,
        )

        /** Sentinel set: every channel is [Color.Unspecified]; see [isUnspecified]. */
        val Unspecified: ArtworkColors = ArtworkColors(
            primary = Color.Unspecified,
            accent = Color.Unspecified,
            container = Color.Unspecified,
            onContainer = Color.Unspecified,
        )
    }

    /** True when this set was never resolved (all channels [Color.Unspecified]). */
    val isUnspecified: Boolean
        get() = primary.isUnspecified
}

/**
 * Loads [url] through Coil, samples its dominant colours with AndroidX [Palette] and returns
 * them as theme-harmonised [ArtworkColors], or `null` while loading / after a failure.
 *
 * The value only changes when [url] or the theme's primary colour changes; the bitmap lives in
 * Coil's memory cache, so the gradient plus a thumbnail of the same artwork cost one decode.
 * Hardware bitmaps are disabled for the request — they cannot be read back for palette
 * extraction.
 *
 * Typical use in the player:
 * ```
 * val colors by rememberArtworkColors(song.thumbnailUrl)
 * Box(Modifier.fillMaxSize().artworkBackground(colors ?: ArtworkColors.Fallback))
 * ```
 *
 * @param url Artwork URL, or `null`/blank to stay at the neutral [ArtworkColors.Fallback].
 */
@Composable
fun rememberArtworkColors(url: String?): State<ArtworkColors?> {
    val context = LocalContext.current
    val themePrimary = MaterialTheme.colorScheme.primary
    return produceState<ArtworkColors?>(initialValue = null, key1 = url, key2 = themePrimary) {
        if (url.isNullOrBlank()) {
            value = null
            return@produceState
        }
        val bitmap = runCatching {
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    // Hardware bitmaps cannot be read back for palette extraction.
                    .allowHardware(false)
                    .build()
                context.imageLoader.execute(request).image?.toBitmap()
            }
        }.getOrNull()
        if (bitmap == null) {
            value = null
            return@produceState
        }
        val palette = runCatching {
            withContext(Dispatchers.Default) {
                Palette.from(bitmap)
                    .maximumColorCount(16)
                    .resizeBitmapArea(64 * 64)
                    .generate()
            }
        }.getOrNull()
        value = palette.toArtworkColors(themePrimary)
    }
}

/**
 * The full-screen gradient for the player (and any other artwork-driven surface): the
 * artwork's [ArtworkColors.primary] at the top, stepping through [ArtworkColors.accent] and
 * [ArtworkColors.container], then fading into **pure black** for the rest of the screen.
 *
 * Per design language §9 the artwork colours occupy the top portion of the draw area —
 * `topFraction = 0.5f` for the player, `0.4f` for home-style screens — with black below, so
 * list content scrolling underneath always sits on a dark, high-contrast base.
 *
 * A [Brush] takes no modifier; wrap this in `Modifier.drawBehind { drawRect(brush) }` or use
 * [Modifier.artworkBackground], which does exactly that.
 *
 * @param colors Artwork colours, typically straight from [rememberArtworkColors].
 * @param topFraction Share of the draw area (0f..1f) the artwork colours cover before black.
 */
fun playerBackgroundBrush(colors: ArtworkColors, topFraction: Float = 0.5f): Brush {
    val end = topFraction.coerceIn(0.05f, 1f)
    val mid = end / 2f
    return Brush.verticalGradient(
        0f to colors.primary,
        mid to colors.accent,
        end to colors.container,
        1f to Color.Black,
    )
}

/**
 * Paints the [playerBackgroundBrush] gradient behind this element — the artwork-driven
 * background of §9 (artwork colour in the top [topFraction], pure black below).
 *
 * ```
 * Modifier.fillMaxSize().artworkBackground(colors)
 * ```
 *
 * @param colors Artwork colours, typically straight from [rememberArtworkColors].
 * @param topFraction Share of the element's height (0f..1f) the artwork colours cover.
 */
fun Modifier.artworkBackground(colors: ArtworkColors, topFraction: Float = 0.5f): Modifier {
    val brush = playerBackgroundBrush(colors, topFraction)
    return drawBehind { drawRect(brush) }
}

/**
 * Maps an AndroidX [Palette] (or `null`, when sampling failed) to theme-harmonised
 * [ArtworkColors]: vibrant/dominant tones for [ArtworkColors.primary], the lighter and darker
 * swatches for [ArtworkColors.accent] and [ArtworkColors.container], all nudged toward
 * [themePrimary] so mixed artwork still reads as one palette. When nothing usable is found the
 * neutral [ArtworkColors.Fallback] is returned.
 */
private fun Palette?.toArtworkColors(themePrimary: Color): ArtworkColors {
    if (this == null) return ArtworkColors.Fallback
    fun rgb(swatch: Palette.Swatch?): Color? =
        swatch?.let { Color(-0x1000000 or (it.rgb and 0xFFFFFF)) }
    val primary = rgb(vibrantSwatch) ?: rgb(dominantSwatch) ?: return ArtworkColors.Fallback
    val accent = rgb(lightVibrantSwatch) ?: rgb(mutedSwatch) ?: primary
    val container = rgb(darkVibrantSwatch) ?: rgb(darkMutedSwatch) ?: primary
    return ArtworkColors(
        primary = primary.harmonize(themePrimary),
        accent = accent.harmonize(primary),
        container = container.harmonize(primary),
        onContainer = readableOn(container),
    )
}

/** Picks whichever of white / black has the higher WCAG contrast against [background]. */
private fun readableOn(background: Color): Color =
    if (contrastRatio(Color.White, background) >= contrastRatio(Color.Black, background)) {
        Color.White
    } else {
        Color.Black
    }

/** WCAG relative-luminance contrast ratio between two colours; 1f (equal) .. 21f. */
private fun contrastRatio(a: Color, b: Color): Float {
    val first = a.luminance()
    val second = b.luminance()
    val lighter = max(first, second)
    val darker = min(first, second)
    return (lighter + 0.05f) / (darker + 0.05f)
}
