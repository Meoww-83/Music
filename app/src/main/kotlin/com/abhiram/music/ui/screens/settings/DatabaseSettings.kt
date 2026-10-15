package com.abhiram.music.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.music.ui.component.ConfirmationDialog

/**
 * Database settings (route `settings/database`): real table counts (DAO from §3.3), the
 * database file size on disk, and confirmation-guarded maintenance actions.
 */
@Composable
fun DatabaseSettings(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val info by viewModel.databaseInfo.collectAsStateWithLifecycle()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearPlayHistoryDialog by remember { mutableStateOf(false) }

    SettingsPage(
        title = "Database",
        onBack = onBack,
        modifier = modifier,
    ) {
        val databaseInfo = info
        if (databaseInfo == null) {
            SettingsProgress(
                label = "Reading database…",
                progress = null,
            )
        } else {
            SettingsProgress(
                label = "Database on disk",
                progress = 1f,
                supportingText = formatBytes(databaseInfo.databaseBytes),
            )

            SettingsGroup(title = "Tables") {
                SettingsEntry(
                    title = "Songs",
                    icon = Icons.Rounded.LibraryMusic,
                    valueText = databaseInfo.songs.toString(),
                )
                SettingsEntry(
                    title = "Artists",
                    icon = Icons.Rounded.Person,
                    valueText = databaseInfo.artists.toString(),
                )
                SettingsEntry(
                    title = "Albums",
                    icon = Icons.Rounded.Album,
                    valueText = databaseInfo.albums.toString(),
                )
                SettingsEntry(
                    title = "Playlists",
                    icon = Icons.Rounded.PlaylistPlay,
                    valueText = databaseInfo.playlists.toString(),
                )
                SettingsEntry(
                    title = "Search history entries",
                    icon = Icons.Rounded.History,
                    valueText = databaseInfo.searchHistoryEntries.toString(),
                )
                SettingsEntry(
                    title = "Play history events",
                    icon = Icons.Rounded.Event,
                    valueText = databaseInfo.playHistoryEvents.toString(),
                )
            }

            SettingsGroup(title = "Maintenance") {
                SettingsEntry(
                    title = "Clear search history",
                    icon = Icons.Rounded.DeleteSweep,
                    description = "Remove every recent search stored on this device",
                    onClick = { showClearHistoryDialog = true },
                )
                SettingsEntry(
                    title = "Clear play history",
                    icon = Icons.Rounded.DeleteSweep,
                    description = "Remove every listening event and Top-played counter from this device",
                    onClick = { showClearPlayHistoryDialog = true },
                )
            }

            SettingsInformation(
                text = "Everything lives on this device only. Play counts and events power the \"Top played\" built-in playlist.",
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }

    if (showClearHistoryDialog) {
        ConfirmationDialog(
            title = "Clear search history?",
            text = "Every recent search you have made will be removed from this device.",
            confirmText = "Clear",
            onConfirm = {
                showClearHistoryDialog = false
                viewModel.clearSearchHistory()
            },
            onDismiss = { showClearHistoryDialog = false },
        )
    }

    if (showClearPlayHistoryDialog) {
        ConfirmationDialog(
            title = "Clear play history?",
            text = "Listening events and Top-played counters will be removed from this device. " +
                "Your library, playlists and downloads are not affected.",
            confirmText = "Clear",
            onConfirm = {
                showClearPlayHistoryDialog = false
                viewModel.clearPlayHistory()
            },
            onDismiss = { showClearPlayHistoryDialog = false },
        )
    }
}
