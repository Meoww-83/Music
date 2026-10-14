package com.abhiram.music.ui.player

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.abhiram.music.playback.NoOpPlayerController
import com.abhiram.music.playback.PlayerController
import com.abhiram.music.playback.PlayerControllerProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Shared, package-private plumbing for the player components (owner CMP-E).
 *
 * Nothing here owns playback state: [rememberPlayerController] only exposes the process-wide
 * controller from the frozen `PlayerControllerProvider`, and the artwork helpers are pure
 * rendering concerns (load + palette extraction for the album-art gradient).
 */

/**
 * Resolves the active [PlayerController], falling back to the inert no-op while the service boots.
 * [PlayerControllerProvider.controller] is a `StateFlow`, so a plain composition-time collect is
 * enough (no lifecycle owner required).
 */
@Composable
internal fun rememberPlayerController(): PlayerController {
    val controller by PlayerControllerProvider.controller.collectAsState()
    return controller ?: NoOpPlayerController
}

/** True when the system animator duration scale is 0 (reduced motion / accessibility). */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Bouncy press feedback shared by every control: the pressed child scales past its rest position
 * and springs back, honouring reduced motion by staying static.
 */
@Composable
internal fun Modifier.springPress(
    interactionSource: InteractionSource,
    reduceMotion: Boolean = false,
    pressedScale: Float = 0.90f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = if (reduceMotion) {
        1f
    } else {
        animateFloatAsState(
            targetValue = if (pressed) pressedScale else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "springPress",
        ).value
    }
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/* -------------------------------------------------------------------------------------------
 * Artwork loading + palette extraction
 * ---------------------------------------------------------------------------------------- */

/** Palette sampled from the album artwork; drives the player's dynamic colour gradient. */
internal data class ArtworkPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
)

/** Result of loading (and palette-sampling) a piece of artwork. */
internal sealed interface ArtworkState {
    /** The image is still being fetched — render a shimmer. */
    data object Loading : ArtworkState

    /** The image could not be fetched or decoded — render a neutral placeholder. */
    data object Failed : ArtworkState

    /** The image is decoded; [palette] may still hold neutral fallbacks for tiny/flat images. */
    data class Ready(val image: ImageBitmap, val palette: ArtworkPalette) : ArtworkState
}

/**
 * Loads [url] through Coil on the IO dispatcher and samples its dominant colours.
 *
 * The value only changes when [url] changes, so the bitmap is held for as long as the artwork is
 * on screen; Coil's memory cache makes secondary requests (gradient + thumbnail) nearly free.
 */
@Composable
internal fun rememberArtworkState(url: String?): State<ArtworkState> {
    val context = LocalContext.current
    return produceState<ArtworkState>(initialValue = ArtworkState.Loading, key1 = url) {
        if (url.isNullOrBlank()) {
            value = ArtworkState.Failed
            return@produceState
        }
        value = ArtworkState.Loading
        val bitmap = runCatching {
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    // Hardware bitmaps cannot be read back for palette extraction.
                    .allowHardware(false)
                    .build()
                context.imageLoader.execute(request).image.toBitmap()
            }
        }.getOrNull()
        if (bitmap == null) {
            value = ArtworkState.Failed
            return@produceState
        }
        val palette = runCatching {
            withContext(Dispatchers.Default) {
                Palette.from(bitmap)
                    .maximumColorCount(12)
                    .resizeBitmapArea(64 * 64)
                    .generate()
            }
        }.getOrNull()
        value = ArtworkState.Ready(bitmap.asImageBitmap(), palette.toArtworkPalette())
    }
}

/** Converts an AndroidX [Palette] (or `null`) into player gradient colours. */
private fun Palette?.toArtworkPalette(): ArtworkPalette {
    fun rgb(value: Int?): Color? = value?.let { Color(-0x1000000 or (it and 0xFFFFFF)) }
    val dominant = rgb(this?.dominantSwatch?.rgb)
    val vibrant = rgb(this?.vibrantSwatch?.rgb)
    val muted = rgb(this?.mutedSwatch?.rgb) ?: rgb(this?.darkMutedSwatch?.rgb)
    return ArtworkPalette(
        primary = vibrant ?: dominant ?: Color(0xFF6E6E7A),
        secondary = dominant ?: muted ?: Color(0xFF3B3B45),
        tertiary = muted ?: dominant ?: Color(0xFF14141A),
    )
}

/**
 * Vertical brush for the full player: the artwork's dynamic colour for the top half, pure black
 * for the bottom half (design language §9).
 */
internal fun artworkPlayerBrush(palette: ArtworkPalette?): Brush {
    val top = palette?.primary ?: Color(0xFF2E2E36)
    val middle = palette?.secondary ?: top.copy(alpha = 0.7f)
    val low = palette?.tertiary ?: Color(0xFF0A0A0C)
    return Brush.verticalGradient(
        0f to top,
        0.30f to middle,
        0.50f to low,
        0.62f to Color.Black,
        1f to Color.Black,
    )
}
