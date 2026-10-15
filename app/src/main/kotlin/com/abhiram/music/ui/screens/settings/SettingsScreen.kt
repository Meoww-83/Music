package com.abhiram.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.music.BuildConfig
import com.abhiram.music.ui.navigation.Routes

/**
 * Settings hub (route `settings`).
 *
 * An expressive header above grouped category entries; every row routes through the
 * [onNavigate] callback with one of the `settings/` sub-routes from [Routes]. Main surfaces are
 * plain — liquid glass is reserved for floating layers.
 */
@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()

    SettingsPage(
        title = "Settings",
        onBack = onBack,
        modifier = modifier,
        headerContent = {
            SettingsHubHeader(
                versionName = BuildConfig.VERSION_NAME,
                updateVersion = updateInfo?.versionName,
            )
        },
    ) {
        SettingsGroup(title = "Personalisation") {
            SettingsEntry(
                title = "General",
                icon = Icons.Rounded.Palette,
                description = "Theme, dynamic colour and content filters",
                onClick = { onNavigate(Routes.SETTINGS_GENERAL) },
            )
        }

        SettingsGroup(title = "Playback") {
            SettingsEntry(
                title = "Player",
                icon = Icons.Rounded.PlayArrow,
                description = "Audio behaviour, queue and sleep timer",
                onClick = { onNavigate(Routes.SETTINGS_PLAYER) },
            )
            SettingsEntry(
                title = "Gestures",
                icon = Icons.Rounded.Gesture,
                description = "Double-tap seek, swipe and haptics",
                onClick = { onNavigate(Routes.SETTINGS_GESTURE) },
            )
        }

        SettingsGroup(title = "Storage") {
            SettingsEntry(
                title = "Cache",
                icon = Icons.Rounded.Storage,
                description = "Audio streaming cache size",
                onClick = { onNavigate(Routes.SETTINGS_CACHE) },
            )
            SettingsEntry(
                title = "Database",
                icon = Icons.Rounded.Dns,
                description = "Library stats and history",
                onClick = { onNavigate(Routes.SETTINGS_DATABASE) },
            )
        }

        SettingsGroup(title = "About") {
            SettingsEntry(
                title = "Other",
                icon = Icons.Rounded.Tune,
                description = "Updates, licences and feedback",
                onClick = { onNavigate(Routes.SETTINGS_OTHER) },
            )
            SettingsEntry(
                title = "About",
                icon = Icons.Rounded.Favorite,
                description = "Version, credits and features",
                onClick = { onNavigate(Routes.SETTINGS_ABOUT) },
            )
        }
    }
}

/** Expressive settings header: chunky app title, one-line summary and version chip. */
@Composable
private fun SettingsHubHeader(
    versionName: String,
    updateVersion: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Music",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "An open source YouTube Music client",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(top = 8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VersionPill(text = "v$versionName")
            if (updateVersion != null) {
                Spacer(Modifier.width(8.dp))
                VersionPill(
                    text = "v$updateVersion available",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/** Small pill-shaped version badge. */
@Composable
private fun VersionPill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
