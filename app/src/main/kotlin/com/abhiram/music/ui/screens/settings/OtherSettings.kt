package com.abhiram.music.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.music.BuildConfig

private const val RepoUrl = "https://github.com/Meoww-83/Music"
private const val IssuesUrl = "$RepoUrl/issues"
private const val DiscussionsUrl = "$RepoUrl/discussions"
private const val LicenceUrl = "https://www.gnu.org/licenses/gpl-3.0.html"

/**
 * "Other" settings (route `settings/other`): GitHub updater entry, licence, issue/feedback
 * links, and honestly disabled backup/restore entries with an explanation.
 */
@Composable
fun OtherSettings(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uriHandler = LocalUriHandler.current
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val isChecking by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()

    SettingsPage(
        title = "Other",
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingsGroup(title = "Updates") {
            val available = updateInfo
            if (available != null) {
                SettingsEntry(
                    title = "Update available: v${available.versionName}",
                    icon = Icons.Rounded.SystemUpdate,
                    description = "Tap to open the release page",
                    onClick = { uriHandler.openUri(available.htmlUrl) },
                )
            } else {
                SettingsEntry(
                    title = "Check for updates",
                    icon = Icons.Rounded.SystemUpdate,
                    description = "Currently installed: v${BuildConfig.VERSION_NAME}",
                    valueText = if (isChecking) "Checking…" else null,
                    onClick = { viewModel.checkForUpdate() },
                )
            }
        }

        SettingsGroup(title = "Project") {
            SettingsEntry(
                title = "Report an issue",
                icon = Icons.Rounded.BugReport,
                description = "Found a bug? Open a ticket on GitHub",
                onClick = { uriHandler.openUri(IssuesUrl) },
            )
            SettingsEntry(
                title = "Feedback & discussion",
                icon = Icons.Rounded.Forum,
                description = "Share ideas or ask questions",
                onClick = { uriHandler.openUri(DiscussionsUrl) },
            )
            SettingsEntry(
                title = "Licences",
                icon = Icons.Rounded.Gavel,
                description = "This app is free software under GPL-3.0",
                onClick = { uriHandler.openUri(LicenceUrl) },
            )
        }

        SettingsGroup(title = "Backup") {
            SettingsEntry(
                title = "Backup library",
                icon = Icons.Rounded.Backup,
                description = "Not implemented yet in this build",
                enabled = false,
            )
            SettingsEntry(
                title = "Restore library",
                icon = Icons.Rounded.Backup,
                description = "Not implemented yet in this build",
                enabled = false,
            )
        }

        SettingsInformation(
            text = "Backup and restore are honestly disabled: they are not implemented in this build. Your library, playlists and preferences live only on this device for now.",
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
