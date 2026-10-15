package com.abhiram.music.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.music.BuildConfig

private const val AboutRepoUrl = "https://github.com/Meoww-83/Music"
private const val AboutIssuesUrl = "$AboutRepoUrl/issues"
private const val AboutLicenceUrl = "https://www.gnu.org/licenses/gpl-3.0.html"

private val FeatureList =
    listOf(
        "Ad-free YouTube Music streaming",
        "Background playback with media notification",
        "Search with suggestions, filters and history",
        "Local playlists, likes and play statistics",
        "Sleep timer, loudness normalisation and skip silence",
        "Squiggly animated progress bar",
        "Dynamic colour, pure black and dark mode themes",
    )

/**
 * About screen (route `settings/about`): version, update banner (same GitHub checker used by the
 * launch-time update dialog), feature list, credits and project links.
 */
@Composable
fun About(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uriHandler = LocalUriHandler.current
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val isChecking by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.checkForUpdate() }

    SettingsPage(
        title = "About",
        onBack = onBack,
        modifier = modifier,
        headerContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(88.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Music",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        val available = updateInfo
        if (available != null) {
            SettingsGroup(title = "Update") {
                SettingsEntry(
                    title = "Version ${available.versionName} is available",
                    icon = Icons.Rounded.SystemUpdate,
                    description = "Tap to open the download page",
                    onClick = { uriHandler.openUri(available.htmlUrl) },
                )
            }
        } else if (isChecking) {
            SettingsProgress(
                label = "Checking for updates…",
                progress = null,
            )
        } else {
            SettingsGroup(title = "Update") {
                SettingsEntry(
                    title = "Check for updates",
                    icon = Icons.Rounded.SystemUpdate,
                    description = "You are running the latest published build if nothing appears",
                    onClick = { viewModel.checkForUpdate() },
                )
            }
        }

        SettingsGroup(title = "Features") {
            SettingsInformation(
                text = FeatureList.joinToString(separator = "\n") { "•  $it" },
                icon = Icons.Rounded.Info,
            )
        }

        SettingsGroup(title = "Credits") {
            SettingsInformation(
                text = "Built by volunteers and standing on the shoulders of open source projects — the InnerTube protocol research, the Metrolist lineage and the Android community. Thank you!",
                icon = Icons.Rounded.Favorite,
            )
        }

        SettingsGroup(title = "Links") {
            SettingsEntry(
                title = "GitHub repository",
                icon = Icons.Rounded.Language,
                description = AboutRepoUrl,
                onClick = { uriHandler.openUri(AboutRepoUrl) },
            )
            SettingsEntry(
                title = "Report an issue",
                icon = Icons.Rounded.BugReport,
                description = "Help improve the app by filing a bug",
                onClick = { uriHandler.openUri(AboutIssuesUrl) },
            )
            SettingsEntry(
                title = "Licence (GPL-3.0)",
                icon = Icons.Rounded.Gavel,
                description = "Free software, redistributed under the GNU GPL v3",
                onClick = { uriHandler.openUri(AboutLicenceUrl) },
            )
        }
    }
}
