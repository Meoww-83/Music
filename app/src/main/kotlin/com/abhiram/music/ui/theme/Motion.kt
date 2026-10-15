package com.abhiram.music.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether animations should be reduced (near-instant) for the current subtree.
 *
 * [ExpressiveTheme] provides `true` when the caller passed `reduceMotion = true` or when the
 * system animator duration scale is zero, and `false` otherwise. App-level animations read this
 * (directly or through [rememberReduceMotion]) to swap springs for instant state changes, which
 * keeps custom transitions respectful of the accessibility setting even when a Material
 * component ignores the theme's motion scheme.
 */
val LocalReduceMotion: ProvidableCompositionLocal<Boolean> = compositionLocalOf { false }

/**
 * Resolves whether motion should be reduced for this part of the composition.
 *
 * The answer is `true` when [explicit] is `true` (an explicit caller preference), when the
 * closest [ExpressiveTheme] has reduced motion enabled (see [LocalReduceMotion]), or when the
 * device reports an animator duration scale of `0f` (the system-wide "Animator duration scale"
 * accessibility setting). Otherwise the answer is `false`.
 *
 * @param explicit Force the reduced-motion answer to `true`, e.g. from a feature flag or test.
 * @return `true` when animations in this subtree should be near-instant.
 */
@Composable
fun rememberReduceMotion(explicit: Boolean = false): Boolean {
    val themeReduceMotion = LocalReduceMotion.current
    val systemScaleIsZero = rememberSystemAnimatorDurationIsZero()
    return explicit || themeReduceMotion || systemScaleIsZero
}

/**
 * Reads `Settings.Global.ANIMATOR_DURATION_SCALE` once per [android.content.Context] and reports
 * whether the user has disabled animations system-wide. A missing or unreadable setting is
 * treated as "animations enabled".
 */
@Composable
private fun rememberSystemAnimatorDurationIsZero(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f) == 0f
    }
}

/**
 * The app's linear/constant motion scheme: every Material motion slot resolves to an
 * instantaneous snap, so components the theme cannot reach directly (Material buttons, sheets,
 * indicator morphs…) still honour reduced motion.
 *
 * This stands in for a built-in `MotionScheme.linear()`/`MotionScheme.constant()` — material3
 * 1.5.0-alpha28 only ships `standard()` and `expressive()`, so the reduced-motion variant is
 * implemented here as a plain [MotionScheme]. [ExpressiveTheme] installs it whenever motion is
 * reduced and pairs it with `LocalReduceMotion = true` for app-authored animations.
 *
 * Specs: all six slots ([MotionScheme.defaultSpatialSpec], [MotionScheme.defaultEffectsSpec],
 * [MotionScheme.fastSpatialSpec], [MotionScheme.fastEffectsSpec], [MotionScheme.slowSpatialSpec],
 * [MotionScheme.slowEffectsSpec]) return a zero-length [snap].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object LinearMotionScheme : MotionScheme {

    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = instant()

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = instant()

    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = instant()

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = instant()

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = instant()

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = instant()

    /** A spec that lands on its target value on the very next frame. */
    private fun <T> instant(): FiniteAnimationSpec<T> = snap()
}
