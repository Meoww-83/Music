package com.abhiram.music.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * The Material 3 Expressive theme of the music app.
 *
 * Wraps [MaterialTheme] with:
 * - a colour scheme: [dynamicColor] Material You palettes on Android 12+, otherwise a
 *   MaterialKolor-generated deep-indigo scheme from [AppColorSchemes] (M3 2025 spec, vibrant
 *   style); [pureBlack] additionally forces every dark surface to [Color.Black] for AMOLED
 *   panels;
 * - [AppTypography] (chunky display/headline styles) and [AppShapes] (expressive shape scale);
 * - [MotionScheme.expressive()] for bouncy, emphasized motion — or [LinearMotionScheme] when
 *   motion is reduced, so Material components animate near-instantly;
 * - [LocalDimTokens] and [LocalReduceMotion] composition locals, exposing the dim palette
 *   (`MaterialTheme.colorScheme.primaryDim` etc.) and the reduced-motion flag.
 *
 * @param darkTheme Whether to use a dark colour scheme; defaults to the system setting.
 * @param dynamicColor Whether to use a Material You dynamic palette (Android 12+); on older
 * devices or with it disabled the MaterialKolor-generated scheme from [AppColorSchemes] is used
 * regardless.
 * @param pureBlack When [darkTheme] is `true`, replace every dark surface role with
 * [Color.Black] (AMOLED-friendly pure black).
 * @param reduceMotion When `true`, install a near-instant motion scheme and publish
 * `LocalReduceMotion = true`. The system "Animator duration scale = 0" setting triggers the
 * same behaviour even when this parameter is `false`.
 * @param content The rest of the UI, themed.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    pureBlack: Boolean = false,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val reduceMotionActive = rememberReduceMotion(explicit = reduceMotion)
    val colorScheme = rememberAppColorScheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        pureBlack = pureBlack,
    )
    val dimTokens = remember(colorScheme, darkTheme) { dimTokensFor(colorScheme, darkTheme) }

    CompositionLocalProvider(
        LocalDimTokens provides dimTokens,
        LocalReduceMotion provides reduceMotionActive,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography.typography,
            shapes = AppShapes.shapes,
            motionScheme = if (reduceMotionActive) {
                LinearMotionScheme
            } else {
                MotionScheme.expressive()
            },
            content = content,
        )
    }
}

/**
 * Resolves the colour scheme for the current configuration, cached on its inputs.
 *
 * Order of preference: Material You dynamic colours (when enabled and on Android 12+), then the
 * MaterialKolor-generated scheme from [AppColorSchemes]'s curated seeds for [darkTheme]. When
 * [pureBlack] is requested in dark mode, every surface role is flattened to [Color.Black] for
 * AMOLED panels.
 */
@Composable
private fun rememberAppColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    pureBlack: Boolean,
): ColorScheme {
    val context = LocalContext.current
    val base = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            remember(context, darkTheme) {
                if (darkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            }

        else -> rememberSeedColorScheme(darkTheme)
    }
    return remember(base, darkTheme, pureBlack) {
        if (darkTheme && pureBlack) base.withPureBlack() else base
    }
}

