package com.abhiram.music.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.music.ui.component.ConfirmationDialog

/**
 * Cache settings (route `settings/cache`): the real on-disk size of `cacheDir/exoplayer`, the
 * maximum-size chips persisted via `AppPreferences.setExoPlayerDiskCacheMaxSize`, and a
 * confirmation-guarded "Clear cache" action.
 */
@Composable
fun CacheSettings(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val preferences = viewModel.preferences
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val cacheMaxSize by
        preferences.exoPlayerDiskCacheMaxSize.collectAsStateWithLifecycle(initialValue = 2048L * 1024L * 1024L)

    var showClearDialog by remember { mutableStateOf(false) }

    SettingsPage(
        title = "Cache",
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingsProgress(
            label = "Audio cache on disk",
            progress = if (cacheMaxSize > 0 && cacheSizeBytes != null) {
                (cacheSizeBytes!!.toFloat() / cacheMaxSize.toFloat())
            } else {
                null
            },
            supportingText = buildString {
                append(formatBytes(cacheSizeBytes ?: 0L))
                append(" used")
            },
        )

        SettingsGroup(title = "Maximum size") {
            CacheSizeChips(
                selectedBytes = cacheMaxSize,
                onSelect = preferences::setExoPlayerDiskCacheMaxSize,
            )
            Text(
                text = "Streamed audio is kept on disk up to this size; older chunks are evicted first. Changes apply after the player restarts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        SettingsGroup(title = "Maintenance") {
            SettingsEntry(
                title = "Clear cache",
                icon = Icons.Rounded.DeleteSweep,
                description = "Free up space; songs will re-download on the next play",
                onClick = { showClearDialog = true },
            )
        }

        SettingsInformation(
            text = "The cache only stores audio you have already streamed — clearing it never affects your library or playlists.",
            icon = Icons.Rounded.Storage,
            modifier = Modifier.padding(top = 16.dp),
        )
    }

    if (showClearDialog) {
        ConfirmationDialog(
            title = "Clear audio cache?",
            text = "${formatBytes(cacheSizeBytes ?: 0L)} of cached audio will be deleted. Tracks will re-download the next time you play them.",
            confirmText = "Clear",
            onConfirm = {
                showClearDialog = false
                viewModel.clearCache()
            },
            onDismiss = { showClearDialog = false },
        )
    }
}

/**
 * Row of cache-size choice chips (256 MB / 512 MB / 1 GB / 2 GB / Unlimited = -1), shared by the
 * player and cache pages.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CacheSizeChips(
    selectedBytes: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Audio cache size",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CacheSizeOptions.forEach { (label, bytes) ->
                FilterChip(
                    selected = selectedBytes == bytes,
                    onClick = { onSelect(bytes) },
                    label = { Text(label) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
