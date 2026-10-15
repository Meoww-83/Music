package com.abhiram.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

/**
 * The app's colour-scheme seeds and static reference palettes.
 *
 * [ExpressiveTheme] prefers the user's Material You wallpaper colours when dynamic colour is
 * enabled (Android 12+); everywhere else the scheme is *generated* from the curated indigo seeds
 * below with MaterialKolor, using the Material 3 **2025 colour spec** and the vibrant palette
 * style — the same "deep indigo music" identity, but computed per the newest spec instead of
 * hard-coded, so container/on-container pairs keep their designed contrast at every tone.
 *
 * The hand-tuned palettes in this file are the *non-generated* fallbacks: they need no
 * composition (used, for example, as the static default of [LocalDimTokens]) and double as the
 * readable reference for what the generated schemes approximate.
 */
object AppColorSchemes {

    /**
     * Deep indigo seed the light fallback palette is generated from — the "primary" of the
     * app's visual identity when no wallpaper colour is available.
     */
    val SeedLight: Color = Color(0xFF4A37C8)

    /**
     * Lifted indigo seed the dark fallback palette is generated from: the same identity, raised
     * in luminance so it stays vivid on near-black surfaces.
     */
    val SeedDark: Color = Color(0xFFC1BFFF)
}

/**
 * Generates (and remembers) the fallback colour scheme used when dynamic colour is off or
 * unavailable: MaterialKolor's Material 3 2025-spec palette, vibrant style, seeded from
 * [AppColorSchemes.SeedLight] or [AppColorSchemes.SeedDark] depending on [darkTheme].
 *
 * This is a pure computation (no wallpaper/resource access), so it works on every API level.
 *
 * @param darkTheme Whether to generate the dark variant of the palette.
 */
@Composable
internal fun rememberSeedColorScheme(darkTheme: Boolean): ColorScheme =
    rememberDynamicColorScheme(
        seedColor = if (darkTheme) AppColorSchemes.SeedDark else AppColorSchemes.SeedLight,
        isDark = darkTheme,
        style = PaletteStyle.Vibrant,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )

/**
 * AMOLED variant of this scheme: every dark surface tier becomes [Color.Black] so unlit pixels
 * stay off, while text, outline and container roles keep their contrast.
 */
internal fun ColorScheme.withPureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color.Black,
    surfaceContainerHighest = Color.Black,
)

/**
 * Hand-tuned light palette used as the static (non-composable) reference: a deep indigo primary
 * with a muted violet secondary and a berry tertiary — the "music app" identity.
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
 * Hand-tuned dark palette used as the static (non-composable) reference — for example the
 * default of [LocalDimTokens] outside an [ExpressiveTheme]: lifted indigo primaries on
 * near-black violet surfaces, mirroring the light scheme's identity.
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
