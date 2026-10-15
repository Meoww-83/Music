package com.abhiram.music.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.music.ui.component.TextFieldDialog

/**
 * Player settings (route `settings/player`): audio behaviour switches, the default sleep timer
 * and the audio disk-cache size — all backed by the frozen `AppPreferences` API.
 */
@Composable
fun PlayerSettings(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val preferences = viewModel.preferences

    val skipSilence by preferences.skipSilence.collectAsStateWithLifecycle(initialValue = false)
    val normalization by preferences.normalization.collectAsStateWithLifecycle(initialValue = true)
    val persistentQueue by preferences.persistentQueue.collectAsStateWithLifecycle(initialValue = true)
    val stopAtEnd by preferences.stopAtEnd.collectAsStateWithLifecycle(initialValue = false)
    val squigglyProgress by preferences.squigglyProgress.collectAsStateWithLifecycle(initialValue = true)
    val sleepTimerMinutes by preferences.defaultSleepTimerMinutes.collectAsStateWithLifecycle(initialValue = 15)
    val cacheMaxSize by preferences.exoPlayerDiskCacheMaxSize.collectAsStateWithLifecycle(initialValue = 2048L * 1024L * 1024L)

    var showSleepTimerDialog by remember { mutableStateOf(false) }

    SettingsPage(
        title = "Player",
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingsGroup(title = "Playback") {
            SwitchSettingEntry(
                title = "Skip silence",
                checked = skipSilence,
                onCheckedChange = preferences::setSkipSilence,
                icon = Icons.Rounded.GraphicEq,
                description = "Automatically skip quiet parts of a track",
            )
            SwitchSettingEntry(
                title = "Loudness normalisation",
                checked = normalization,
                onCheckedChange = preferences::setNormalization,
                icon = Icons.Rounded.Equalizer,
                description = "Balance volume between tracks using YouTube loudness data",
            )
            SwitchSettingEntry(
                title = "Persistent queue",
                checked = persistentQueue,
                onCheckedChange = preferences::setPersistentQueue,
                icon = Icons.Rounded.Restore,
                description = "Restore the queue after the app restarts",
            )
            SwitchSettingEntry(
                title = "Stop at the end of the queue",
                checked = stopAtEnd,
                onCheckedChange = preferences::setStopAtEnd,
                icon = Icons.Rounded.Stop,
                description = "Pause instead of continuing when nothing is left to play",
            )
            SwitchSettingEntry(
                title = "Squiggly progress",
                checked = squigglyProgress,
                onCheckedChange = preferences::setSquigglyProgress,
                icon = Icons.Rounded.ShowChart,
                description = "Animated wavy seek bar in the full player",
            )
        }

        SettingsGroup(title = "Sleep timer") {
            SettingsEntry(
                title = "Default sleep timer",
                icon = Icons.Rounded.Timer,
                description = "Starting duration for the player sleep timer",
                valueText = "$sleepTimerMinutes min",
                onClick = { showSleepTimerDialog = true },
            )
        }

        SettingsGroup(title = "Audio cache") {
            CacheSizeChips(
                selectedBytes = cacheMaxSize,
                onSelect = preferences::setExoPlayerDiskCacheMaxSize,
            )
        }
    }

    if (showSleepTimerDialog) {
        TextFieldDialog(
            title = "Default sleep timer",
            value = sleepTimerMinutes.toString(),
            label = "Minutes",
            placeholder = "15",
            onDismiss = { showSleepTimerDialog = false },
            onConfirm = { input ->
                showSleepTimerDialog = false
                input.toIntOrNull()
                    ?.coerceIn(1, 24 * 60)
                    ?.let { preferences.setDefaultSleepTimerMinutes(it) }
            },
        )
    }
}
