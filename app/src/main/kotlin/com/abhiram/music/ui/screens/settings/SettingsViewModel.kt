package com.abhiram.music.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.abhiram.music.constants.AppPreferences
import com.abhiram.music.database.DatabaseDao
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.utils.GitHubUpdateChecker
import com.abhiram.music.utils.UpdateInfo
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Snapshot of the local database's on-disk footprint and table sizes, produced on the IO
 * dispatcher by [SettingsViewModel.refreshDatabaseInfo].
 */
data class DatabaseInfo(
    val songs: Int,
    val artists: Int,
    val albums: Int,
    val playlists: Int,
    val searchHistoryEntries: Int,
    val playHistoryEvents: Int,
    val databaseBytes: Long,
)

/**
 * Backing store for every settings page.
 *
 * Wraps `ServiceLocator.container.preferences` plus the heavier work that must not run on the
 * main thread: measuring/clearing the ExoPlayer disk cache, collecting database table counts and
 * file sizes, and the GitHub release check. Gesture-only preferences live in the screen-local
 * [GesturePreferences] DataStore.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    /** The frozen, app-wide preference wrapper (§3.5). */
    val preferences: AppPreferences = ServiceLocator.container.preferences

    /** Extra gesture preferences owned by this package. */
    val gestures: GesturePreferences = GesturePreferences(application)

    private val dao: DatabaseDao = ServiceLocator.container.dao

    private val _cacheSizeBytes = MutableStateFlow<Long?>(null)

    /** Bytes currently used by `cacheDir/exoplayer`, `null` until the first measurement. */
    val cacheSizeBytes: StateFlow<Long?> = _cacheSizeBytes.asStateFlow()

    private val _databaseInfo = MutableStateFlow<DatabaseInfo?>(null)

    /** Table counts + DB file size, `null` until the first measurement. */
    val databaseInfo: StateFlow<DatabaseInfo?> = _databaseInfo.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)

    /** A newer GitHub release, `null` when up to date or when the check failed. */
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)

    /** True while the GitHub release check is in flight. */
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    init {
        refreshCacheSize()
        refreshDatabaseInfo()
    }

    /** Re-measures the ExoPlayer disk cache on the IO dispatcher. */
    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            _cacheSizeBytes.value = exoPlayerCacheBytes()
        }
    }

    /** Deletes every cached stream chunk; the directory itself is kept. */
    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(getApplication<Application>().cacheDir, ExoPlayerCacheDirName)
            dir.listFiles()?.forEach { child -> child.deleteRecursively() }
            _cacheSizeBytes.value = exoPlayerCacheBytes()
        }
    }

    /** Re-collects table counts and the database file size on the IO dispatcher. */
    fun refreshDatabaseInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val application = getApplication<Application>()
            val databaseBytes =
                application.databaseList()
                    .mapNotNull { name -> application.getDatabasePath(name) }
                    .filter { it.exists() }
                    .sumOf { it.length() }
            _databaseInfo.value =
                DatabaseInfo(
                    songs = dao.songs().first().size,
                    artists = dao.artists().first().size,
                    albums = dao.albums().first().size,
                    playlists = dao.playlists().first().size,
                    searchHistoryEntries = dao.searchHistory().first().size,
                    playHistoryEvents = dao.events().first().size,
                    databaseBytes = databaseBytes,
                )
        }
    }

    /** Clears every recent-search entry through the database layer. */
    fun clearSearchHistory() {
        viewModelScope.launch {
            dao.clearSearchHistory()
            refreshDatabaseInfo()
        }
    }

    /** Checks GitHub for a newer release and exposes it through [updateInfo]. */
    fun checkForUpdate() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _updateInfo.value = runCatching { GitHubUpdateChecker.checkForUpdate() }.getOrNull()
            _isCheckingUpdate.value = false
        }
    }

    private fun exoPlayerCacheBytes(): Long {
        val dir = File(getApplication<Application>().cacheDir, ExoPlayerCacheDirName)
        if (!dir.exists()) return 0L
        return dir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    companion object {
        /** Must match the directory the playback service uses for its `SimpleCache` (§4). */
        const val ExoPlayerCacheDirName = "exoplayer"

        /** Plain factory — the view model resolves everything through [ServiceLocator]. */
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    SettingsViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
                }
            }
    }
}

/** Human readable byte size, e.g. `512 MB`, used by the cache/database pages. */
internal fun formatBytes(bytes: Long): String =
    when {
        bytes <= 0L -> "0 B"
        bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
        bytes < 1024L * 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }

/** Audio disk cache size options offered on the player and cache pages. */
internal val CacheSizeOptions: List<Pair<String, Long>> =
    listOf(
        "256 MB" to 256L * 1024L * 1024L,
        "512 MB" to 512L * 1024L * 1024L,
        "1 GB" to 1024L * 1024L * 1024L,
        "2 GB" to 2048L * 1024L * 1024L,
        "Unlimited" to -1L,
    )
