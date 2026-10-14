package com.abhiram.music.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.abhiram.music.constants.AppPreferences

/**
 * Returns the process-wide [AppPreferences] instance, remembered for the composition.
 *
 * Convenience accessor for settings screens and player components:
 * ```kotlin
 * val darkMode by rememberSettings().darkMode.collectAsState(DarkMode.SYSTEM)
 * ```
 */
@Composable
fun rememberSettings(): AppPreferences = remember { ServiceLocator.container.preferences }
