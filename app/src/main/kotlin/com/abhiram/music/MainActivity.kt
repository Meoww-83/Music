package com.abhiram.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.ui.glass.GlassBackdrop
import com.abhiram.music.ui.navigation.AppNavigation
import com.abhiram.music.ui.theme.ExpressiveTheme
import com.abhiram.music.ui.update.UpdateHostContext
import com.abhiram.music.ui.update.UpdateDialog
import com.abhiram.music.ui.update.rememberUpdateCheckState

/**
 * Single activity host.
 *
 * Responsibilities kept deliberately small: resolve the user's theme preferences, wrap the app in
 * [ExpressiveTheme] + [GlassBackdrop] (so floating liquid-glass layers have something to sample),
 * render the navigation graph, request the notification permission and show the GitHub update
 * dialog once per launch.
 */
class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // Notification denial only affects the media notification, playback still works.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        UpdateHostContext = applicationContext
        runCatching { ServiceLocator.init(this) }
        requestNotificationPermissionIfNeeded()

        setContent {
            val navController = rememberNavController()
            val preferences = remember { ServiceLocator.container.preferences }
            val darkMode by preferences.darkMode.collectAsStateWithLifecycle(initialValue = com.abhiram.music.constants.DarkMode.SYSTEM)
            val dynamicColor by preferences.dynamicColor.collectAsStateWithLifecycle(initialValue = true)
            val pureBlack by preferences.pureBlack.collectAsStateWithLifecycle(initialValue = false)

            val updateState = rememberUpdateCheckState()

            ExpressiveTheme(
                darkTheme = when (darkMode) {
                    com.abhiram.music.constants.DarkMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                    com.abhiram.music.constants.DarkMode.LIGHT -> false
                    com.abhiram.music.constants.DarkMode.DARK -> true
                },
                dynamicColor = dynamicColor,
                pureBlack = pureBlack,
            ) {
                GlassBackdrop {
                    AppNavigation(navController = navController)
                }

                UpdateDialog(state = updateState)
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            UpdateHostContext = null
        }
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
