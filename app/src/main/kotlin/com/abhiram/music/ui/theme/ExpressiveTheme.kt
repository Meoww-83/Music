package com.abhiram.music.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
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
 *   hand-tuned deep-purple/indigo music scheme; [pureBlack] additionally forces every dark
 *   surface to [Color.Black] for AMOLED panels;
 * - [AppTypography] (chunky display/headline styles) and [AppShapes] (expressive shape scale);
 * - [MotionScheme.expressive()] for bouncy, emphasized motion — or [LinearMotionScheme] when
 *   motion is reduced, so Material components animate near-instantly;
 * - [LocalDimTokens] and [LocalReduceMotion] composition locals, exposing the dim palette
 *   (`MaterialTheme.colorScheme.primaryDim` etc.) and the reduced-motion flag.
 *
 * @param darkTheme Whether to use a dark colour scheme; defaults to the system setting.
 * @param dynamicColor Whether to use a Material You dynamic palette (Android 12+); on older
 * devices the hand-tuned fallback scheme is used regardless.
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
 * hand-tuned deep-purple/indigo fallback for [darkTheme]. When [pureBlack] is requested in dark
 * mode, every surface role is flattened to [Color.Black] for AMOLED panels.
 */
@Composable
private fun rememberAppColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    pureBlack: Boolean,
): ColorScheme {
    val context = LocalContext.current
    return remember(context, darkTheme, dynamicColor, pureBlack) {
        val base = when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            darkTheme -> handTunedDarkColorScheme()
            else -> handTunedLightColorScheme()
        }
        if (darkTheme && pureBlack) base.withPureBlackSurfaces() else base
    }
}

/**
 * AMOLED variant of this scheme: every dark surface tier becomes [Color.Black] so unlit
 * pixels stay off, while text, outline and container roles keep their contrast.
 */
private fun ColorScheme.withPureBlackSurfaces(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color.Black,
    surfaceContainerHighest = Color.Black,
)

/**
 * Hand-tuned light palette used when dynamic colour is off or unavailable: a deep indigo
 * primary with a muted violet secondary and a berry tertiary — the "music app" identity.
 */
internal fun handTunedLightColorScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF4A37C8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE1DEFF),
    onPrimaryContainer = Color(0xFF140069),
    secondary = Color(0xFF5C55A0),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1DDFF),
    onSecondaryContainer = Color(0xFF180050),
    tertiary = Color(0xFF8E4B79),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8EC),
    onTertiaryContainer = Color(0xFF3A0728),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE5E0EB),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0),
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2F0F7),
    inversePrimary = Color(0xFFC1BFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F5FC),
    surfaceContainer = Color(0xFFF1EFF8),
    surfaceContainerHigh = Color(0xFFEBE9F2),
    surfaceContainerHighest = Color(0xFFE5E3EC),
)

/**
 * Hand-tuned dark palette used when dynamic colour is off or unavailable: lifted indigo
 * primaries on near-black violet surfaces, mirroring the light scheme's identity.
 */
internal fun handTunedDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFFC1BFFF),
    onPrimary = Color(0xFF1E0070),
    primaryContainer = Color(0xFF3F2DB8),
    onPrimaryContainer = Color(0xFFE1DEFF),
    secondary = Color(0xFFC8C1FF),
    onSecondary = Color(0xFF2B2173),
    secondaryContainer = Color(0xFF433B86),
    onSecondaryContainer = Color(0xFFE1DDFF),
    tertiary = Color(0xFFFFAFD5),
    onTertiary = Color(0xFF561A3B),
    tertiaryContainer = Color(0xFF703353),
    onTertiaryContainer = Color(0xFFFFD8EC),
    background = Color(0xFF131218),
    onBackground = Color(0xFFE5E2E9),
    surface = Color(0xFF131218),
    onSurface = Color(0xFFE5E2E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF908E98),
    outlineVariant = Color(0xFF46464F),
    inverseSurface = Color(0xFFE5E2E9),
    inverseOnSurface = Color(0xFF303036),
    inversePrimary = Color(0xFF4A37C8),
    surfaceContainerLowest = Color(0xFF0E0D13),
    surfaceContainerLow = Color(0xFF1A1920),
    surfaceContainer = Color(0xFF1E1D24),
    surfaceContainerHigh = Color(0xFF29282F),
    surfaceContainerHighest = Color(0xFF34333A),
)
