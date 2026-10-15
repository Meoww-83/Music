package com.abhiram.music.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * The app's chunky, expressive type scale.
 *
 * Display and headline styles use heavy weights (Bold → Black) with tight, negative letter
 * spacing so large text reads as one dense block — the "expressive ≠ toy" rule. Titles and
 * labels step down to SemiBold/Bold, while body styles keep the regular weight, generous line
 * height and only a whisper of tracking so long-form reading stays comfortable.
 *
 * The emphasized variants (used by Material 3 Expressive components) start from the same
 * metrics with the font weight bumped one step.
 *
 * Installed into [androidx.compose.material3.MaterialTheme] by [ExpressiveTheme], so call sites
 * simply read `MaterialTheme.typography.headlineMedium`.
 */
object AppTypography {

    /**
     * The [Typography] instance installed into [androidx.compose.material3.MaterialTheme] by
     * [ExpressiveTheme]: full type scale (baseline + emphasized) in the chunky house style.
     */
    val typography: Typography = Typography(
        displayLarge = DisplayLarge,
        displayMedium = DisplayMedium,
        displaySmall = DisplaySmall,
        headlineLarge = HeadlineLarge,
        headlineMedium = HeadlineMedium,
        headlineSmall = HeadlineSmall,
        titleLarge = TitleLarge,
        titleMedium = TitleMedium,
        titleSmall = TitleSmall,
        bodyLarge = BodyLarge,
        bodyMedium = BodyMedium,
        bodySmall = BodySmall,
        labelLarge = LabelLarge,
        labelMedium = LabelMedium,
        labelSmall = LabelSmall,
        displayLargeEmphasized = DisplayLarge.emphasized(),
        displayMediumEmphasized = DisplayMedium.emphasized(),
        displaySmallEmphasized = DisplaySmall.emphasized(),
        headlineLargeEmphasized = HeadlineLarge.emphasized(),
        headlineMediumEmphasized = HeadlineMedium.emphasized(),
        headlineSmallEmphasized = HeadlineSmall.emphasized(),
        titleLargeEmphasized = TitleLarge.emphasized(),
        titleMediumEmphasized = TitleMedium.emphasized(),
        titleSmallEmphasized = TitleSmall.emphasized(),
        bodyLargeEmphasized = BodyLarge.emphasized(),
        bodyMediumEmphasized = BodyMedium.emphasized(),
        bodySmallEmphasized = BodySmall.emphasized(),
        labelLargeEmphasized = LabelLarge.emphasized(),
        labelMediumEmphasized = LabelMedium.emphasized(),
        labelSmallEmphasized = LabelSmall.emphasized(),
    )
}

/** Builds one style of the chunky house type scale. */
private fun chunky(
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontWeight: FontWeight,
    letterSpacing: TextUnit,
): TextStyle = TextStyle(
    fontSize = fontSize,
    lineHeight = lineHeight,
    fontWeight = fontWeight,
    letterSpacing = letterSpacing,
)

/** The emphasized counterpart of a baseline style: same metrics, one weight step heavier. */
private fun TextStyle.emphasized(): TextStyle =
    copy(fontWeight = when (fontWeight) {
        null, FontWeight.Normal -> FontWeight.Medium
        FontWeight.Medium -> FontWeight.SemiBold
        FontWeight.SemiBold -> FontWeight.Bold
        FontWeight.Bold -> FontWeight.ExtraBold
        else -> FontWeight.Black
    })

private val DisplayLarge = chunky(57.sp, 64.sp, FontWeight.Black, (-1.4).sp)
private val DisplayMedium = chunky(45.sp, 52.sp, FontWeight.Black, (-1.0).sp)
private val DisplaySmall = chunky(36.sp, 44.sp, FontWeight.ExtraBold, (-0.7).sp)

private val HeadlineLarge = chunky(32.sp, 40.sp, FontWeight.ExtraBold, (-0.5).sp)
private val HeadlineMedium = chunky(28.sp, 36.sp, FontWeight.Bold, (-0.4).sp)
private val HeadlineSmall = chunky(24.sp, 32.sp, FontWeight.Bold, (-0.2).sp)

private val TitleLarge = chunky(22.sp, 28.sp, FontWeight.Bold, (-0.1).sp)
private val TitleMedium = chunky(16.sp, 24.sp, FontWeight.SemiBold, 0.sp)
private val TitleSmall = chunky(14.sp, 20.sp, FontWeight.SemiBold, 0.sp)

private val BodyLarge = chunky(16.sp, 24.sp, FontWeight.Normal, 0.2.sp)
private val BodyMedium = chunky(14.sp, 20.sp, FontWeight.Normal, 0.15.sp)
private val BodySmall = chunky(12.sp, 16.sp, FontWeight.Normal, 0.1.sp)

private val LabelLarge = chunky(14.sp, 20.sp, FontWeight.SemiBold, 0.1.sp)
private val LabelMedium = chunky(12.sp, 16.sp, FontWeight.SemiBold, 0.2.sp)
private val LabelSmall = chunky(11.sp, 16.sp, FontWeight.Medium, 0.2.sp)
