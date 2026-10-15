package com.abhiram.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * A parallel palette of "dim" colour roles that mirrors the Material 3 [ColorScheme] roles this
 * app needs for subdued, low-emphasis chrome (mini player, dimmed backgrounds, inactive states).
 *
 * Material 3 1.5.0-alpha28 does not ship these roles, so [ExpressiveTheme] derives every value
 * from its base role by lowering luminance: subtly darkened in light themes and further darkened
 * in dark themes (AMOLED-friendly when [ExpressiveTheme]'s `pureBlack` is enabled).
 *
 * Read the tokens either through [MaterialTheme.dimTokens] or through the [ColorScheme] extension
 * properties such as `MaterialTheme.colorScheme.primaryDim`.
 *
 * @property primaryDim Dimmed variant of `ColorScheme.primary`.
 * @property onPrimaryDim Content colour drawn on top of [primaryDim].
 * @property primaryContainerDim Dimmed variant of `ColorScheme.primaryContainer`.
 * @property onPrimaryContainerDim Content colour drawn on top of [primaryContainerDim].
 * @property secondaryDim Dimmed variant of `ColorScheme.secondary`.
 * @property tertiaryDim Dimmed variant of `ColorScheme.tertiary`.
 * @property surfaceDim Dimmed variant of the scheme's base `surface`.
 * @property surfaceContainerLowestDim Dimmed variant of `ColorScheme.surfaceContainerLowest`.
 * @property surfaceContainerLowDim Dimmed variant of `ColorScheme.surfaceContainerLow`.
 * @property surfaceContainerDim Dimmed variant of `ColorScheme.surfaceContainer`.
 * @property surfaceContainerHighDim Dimmed variant of `ColorScheme.surfaceContainerHigh`.
 * @property surfaceContainerHighestDim Dimmed variant of `ColorScheme.surfaceContainerHighest`.
 */
@Immutable
data class DimTokens(
    val primaryDim: Color,
    val onPrimaryDim: Color,
    val primaryContainerDim: Color,
    val onPrimaryContainerDim: Color,
    val secondaryDim: Color,
    val tertiaryDim: Color,
    val surfaceDim: Color,
    val surfaceContainerLowestDim: Color,
    val surfaceContainerLowDim: Color,
    val surfaceContainerDim: Color,
    val surfaceContainerHighDim: Color,
    val surfaceContainerHighestDim: Color,
)

/**
 * Provides the [DimTokens] installed by the closest [ExpressiveTheme] ancestor.
 *
 * Falls back to the dim tokens derived from the hand-tuned dark colour scheme when read outside
 * of an [ExpressiveTheme], so call sites never crash with an unset local.
 */
val LocalDimTokens: ProvidableCompositionLocal<DimTokens> = staticCompositionLocalOf {
    dimTokensFor(handTunedDarkColorScheme(), darkTheme = true)
}

/**
 * The full [DimTokens] set installed by the closest [ExpressiveTheme] ancestor.
 *
 * Use this when several dim roles are needed together (for example to build a gradient); for a
 * single role prefer the [ColorScheme] extension properties, which read exactly as the design
 * spec (`MaterialTheme.colorScheme.primaryDim`).
 */
val MaterialTheme.dimTokens: DimTokens
    @Composable
    get() = LocalDimTokens.current

/**
 * Dimmed variant of [ColorScheme.primary] for subdued accents such as the mini player and
 * inactive transport controls.
 */
val ColorScheme.primaryDim: Color
    @Composable
    get() = LocalDimTokens.current.primaryDim

/**
 * Content colour meant to be drawn on top of [primaryDim].
 */
val ColorScheme.onPrimaryDim: Color
    @Composable
    get() = LocalDimTokens.current.onPrimaryDim

/**
 * Dimmed variant of [ColorScheme.primaryContainer] for quiet contained surfaces.
 */
val ColorScheme.primaryContainerDim: Color
    @Composable
    get() = LocalDimTokens.current.primaryContainerDim

/**
 * Content colour meant to be drawn on top of [primaryContainerDim].
 */
val ColorScheme.onPrimaryContainerDim: Color
    @Composable
    get() = LocalDimTokens.current.onPrimaryContainerDim

/**
 * Dimmed variant of [ColorScheme.secondary] for secondary quiet accents.
 */
val ColorScheme.secondaryDim: Color
    @Composable
    get() = LocalDimTokens.current.secondaryDim

/**
 * Dimmed variant of [ColorScheme.tertiary] for tertiary quiet accents.
 */
val ColorScheme.tertiaryDim: Color
    @Composable
    get() = LocalDimTokens.current.tertiaryDim

/**
 * Dimmed variant of the scheme surface; the background behind dim chrome.
 *
 * Note: material3 1.5.0-alpha28 also defines a `ColorScheme.surfaceDim` member of its own. A
 * member always wins over an extension, so this property only takes effect if that member is
 * absent; use [MaterialTheme.dimTokens].surfaceDim when you must be certain of reading the token.
 */
val ColorScheme.surfaceDim: Color
    @Composable
    get() = LocalDimTokens.current.surfaceDim

/**
 * Dimmed variant of [ColorScheme.surfaceContainerLowest].
 */
val ColorScheme.surfaceContainerLowestDim: Color
    @Composable
    get() = LocalDimTokens.current.surfaceContainerLowestDim

/**
 * Dimmed variant of [ColorScheme.surfaceContainerLow].
 */
val ColorScheme.surfaceContainerLowDim: Color
    @Composable
    get() = LocalDimTokens.current.surfaceContainerLowDim

/**
 * Dimmed variant of [ColorScheme.surfaceContainer].
 *
 * Note: material3 1.5.0-alpha28 may also define a `ColorScheme.surfaceContainerDim` member; the
 * member wins over this extension in that case. See [surfaceDim] for the same caveat.
 */
val ColorScheme.surfaceContainerDim: Color
    @Composable
    get() = LocalDimTokens.current.surfaceContainerDim

/**
 * Dimmed variant of [ColorScheme.surfaceContainerHigh].
 */
val ColorScheme.surfaceContainerHighDim: Color
    @Composable
    get() = LocalDimTokens.current.surfaceContainerHighDim

/**
 * Dimmed variant of [ColorScheme.surfaceContainerHighest].
 */
val ColorScheme.surfaceContainerHighestDim: Color
    @Composable
    get() = LocalDimTokens.current.surfaceContainerHighestDim

/**
 * Derives the dim counterparts of every Material 3 role from [colorScheme].
 *
 * Light schemes are darkened subtly (roughly 4-9% toward black) so dim chrome stays readable in
 * daylight; dark schemes are pushed down harder (roughly 15-28%) to give the player UI its
 * moody, low-emission look. Pure-black AMOLED schemes naturally end up near-black here.
 *
 * @param colorScheme The active colour scheme whose roles are dimmed.
 * @param darkTheme Whether [colorScheme] is a dark scheme; controls the dimming strength.
 */
internal fun dimTokensFor(colorScheme: ColorScheme, darkTheme: Boolean): DimTokens =
    if (darkTheme) {
        DimTokens(
            primaryDim = colorScheme.primary.dim(0.80f),
            onPrimaryDim = colorScheme.onPrimary.dim(0.85f),
            primaryContainerDim = colorScheme.primaryContainer.dim(0.78f),
            onPrimaryContainerDim = colorScheme.onPrimaryContainer.dim(0.88f),
            secondaryDim = colorScheme.secondary.dim(0.80f),
            tertiaryDim = colorScheme.tertiary.dim(0.80f),
            surfaceDim = colorScheme.surface.dim(0.72f),
            surfaceContainerLowestDim = colorScheme.surfaceContainerLowest.dim(0.75f),
            surfaceContainerLowDim = colorScheme.surfaceContainerLow.dim(0.78f),
            surfaceContainerDim = colorScheme.surfaceContainer.dim(0.80f),
            surfaceContainerHighDim = colorScheme.surfaceContainerHigh.dim(0.82f),
            surfaceContainerHighestDim = colorScheme.surfaceContainerHighest.dim(0.84f),
        )
    } else {
        DimTokens(
            primaryDim = colorScheme.primary.dim(0.93f),
            onPrimaryDim = colorScheme.onPrimary.dim(0.97f),
            primaryContainerDim = colorScheme.primaryContainer.dim(0.95f),
            onPrimaryContainerDim = colorScheme.onPrimaryContainer.dim(0.97f),
            secondaryDim = colorScheme.secondary.dim(0.91f),
            tertiaryDim = colorScheme.tertiary.dim(0.91f),
            surfaceDim = colorScheme.surface.dim(0.95f),
            surfaceContainerLowestDim = colorScheme.surfaceContainerLowest.dim(0.96f),
            surfaceContainerLowDim = colorScheme.surfaceContainerLow.dim(0.96f),
            surfaceContainerDim = colorScheme.surfaceContainer.dim(0.96f),
            surfaceContainerHighDim = colorScheme.surfaceContainerHigh.dim(0.96f),
            surfaceContainerHighestDim = colorScheme.surfaceContainerHighest.dim(0.96f),
        )
    }

/**
 * Multiplies every colour channel of this [Color] by [fraction] (0f = black, 1f = unchanged),
 * keeping alpha intact, which is the "lower the luminance" rule the dim roles are built on.
 */
private fun Color.dim(fraction: Float): Color =
    Color(red = red * fraction, green = green * fraction, blue = blue * fraction, alpha = alpha)
