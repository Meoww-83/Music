package com.abhiram.music.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.music.constants.DarkMode
import kotlinx.coroutines.launch

/**
 * General settings (route `settings/general`): theme, dynamic colour, pure black, search
 * history and the explicit content filter — all backed by the frozen `AppPreferences` API.
 */
@Composable
fun GeneralSettings(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val preferences = viewModel.preferences
    val scope = rememberCoroutineScope()

    val darkMode by preferences.darkMode.collectAsStateWithLifecycle(initialValue = DarkMode.SYSTEM)
    val dynamicColor by preferences.dynamicColor.collectAsStateWithLifecycle(initialValue = true)
    val pureBlack by preferences.pureBlack.collectAsStateWithLifecycle(initialValue = false)
    val searchHistoryEnabled by
        preferences.searchHistoryEnabled.collectAsStateWithLifecycle(initialValue = true)
    val hideExplicit by preferences.hideExplicit.collectAsStateWithLifecycle(initialValue = false)

    var showDarkModeDialog by remember { mutableStateOf(false) }
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    SettingsPage(
        title = "General",
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingsGroup(title = "Appearance") {
            SettingsEntry(
                title = "Dark mode",
                icon = Icons.Rounded.DarkMode,
                description = "Follow the system or force light/dark",
                valueText = darkMode.label,
                onClick = { showDarkModeDialog = true },
            )
            SwitchSettingEntry(
                title = "Dynamic colour",
                checked = dynamicColor,
                onCheckedChange = preferences::setDynamicColor,
                icon = Icons.Rounded.Palette,
                description = if (supportsDynamicColor) {
                    "Material You colours taken from your wallpaper"
                } else {
                    "Requires Android 12 or newer"
                },
                enabled = supportsDynamicColor,
            )
            SwitchSettingEntry(
                title = "Pure black",
                checked = pureBlack,
                onCheckedChange = preferences::setPureBlack,
                icon = Icons.Rounded.InvertColors,
                description = "Use a true-black background in dark mode (OLED friendly)",
            )
        }

        SettingsGroup(title = "Content") {
            SwitchSettingEntry(
                title = "Search history",
                checked = searchHistoryEnabled,
                onCheckedChange = preferences::setSearchHistoryEnabled,
                icon = Icons.Rounded.History,
                description = "Keep recent searches on this device",
            )
            SwitchSettingEntry(
                title = "Hide explicit content",
                checked = hideExplicit,
                onCheckedChange = preferences::setHideExplicit,
                icon = Icons.Rounded.Explicit,
                description = "Filter songs flagged as explicit from search results",
            )
        }
    }

    if (showDarkModeDialog) {
        DarkModeDialog(
            current = darkMode,
            onSelect = { mode ->
                showDarkModeDialog = false
                scope.launch { preferences.setDarkMode(mode) }
            },
            onDismiss = { showDarkModeDialog = false },
        )
    }
}

private val DarkMode.label: String
    get() =
        when (this) {
            DarkMode.SYSTEM -> "System default"
            DarkMode.LIGHT -> "Light"
            DarkMode.DARK -> "Dark"
        }

/** Expressive single-choice dialog for the [DarkMode] preference. */
@Composable
private fun DarkModeDialog(
    current: DarkMode,
    onSelect: (DarkMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dark mode") },
        text = {
            Column {
                DarkMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .heightIn(min = 48.dp)
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = current == mode,
                            onClick = { onSelect(mode) },
                        )
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}
