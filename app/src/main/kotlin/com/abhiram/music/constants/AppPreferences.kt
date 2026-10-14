package com.abhiram.music.constants

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences.Key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Typed wrapper over the app's `DataStore<Preferences>`.
 *
 * Every property exposes a cold [Flow] with a sensible default applied, and every setting has a
 * matching setter. Setter names/signatures are frozen — the settings screens and
 * [com.abhiram.music.service.PlayerService] depend on them.
 *
 * Non-`suspend` setters write asynchronously on [Dispatchers.IO]; they are fire-and-forget by
 * design (a failed write simply leaves the previous value in place and is retried next time the
 * setting changes).
 *
 * @param ds the preferences DataStore owned by `di/AppContainer`.
 */
class AppPreferences(private val ds: DataStore<Preferences>) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** UI dark-mode strategy. Defaults to [DarkMode.SYSTEM]. */
    val darkMode: Flow<DarkMode> =
        ds.data
            .map { DarkMode.fromName(it[PreferencesKeys.DARK_MODE]) }
            .distinctUntilChanged()

    /** Material You (dynamic colour) extraction from wallpaper. Defaults to `true`. */
    val dynamicColor: Flow<Boolean> =
        ds.data
            .map { it[PreferencesKeys.DYNAMIC_COLOR] ?: true }
            .distinctUntilChanged()

    /** Pure-black surfaces in dark mode. Defaults to `false`. */
    val pureBlack: Flow<Boolean> =
        ds.data
            .map { it[PreferencesKeys.PURE_BLACK] ?: false }
            .distinctUntilChanged()

    /** Skip silence while playing (ExoPlayer audio processing). Defaults to `false`. */
    val skipSilence: Flow<Boolean> =
        ds.data
            .map { it[PreferencesKeys.SKIP_SILENCE] ?: false }
            .distinctUntilChanged()

    /** Loudness normalisation (YouTube loudnessDb-based gain). Defaults to `false`. */
    val normalization: Flow<Boolean> =
        ds.data
            .map { it[PreferencesKeys.NORMALIZATION] ?: false }
            .distinctUntilChanged()

    /** Keep the playback queue across process restarts. Defaults to `true`. */
    val persistentQueue: Flow<Boolean> =
        ds.data
            .map { it[PreferencesKeys.PERSISTENT_QUEUE] ?: true }
            .distinctUntilChanged()

    /** Hide explicit content from listings and search results. Defaults to `false`. */
    val hideExplicit: Flow<Boolean> =
        ds.data
            .map { it[PreferencesKeys.HIDE_EXPLICIT] ?: false }
            .distinctUntilChanged()

    /** Squiggly (wavering) seek-bar progress. Defaults to `true`. */
    val squigglyProgress: Flow<Boolean> =
        ds.data
            .map { it[PreferencesKeys.SQUIGGLY_PROGRESS] ?: true }
            .distinctUntilChanged()

    /** Stop playback at the end of the queue instead of autoplaying. Defaults to `false`. */
    val stopAtEnd: Flow<Boolean> =
        ds.data
            .map { it[PreferencesKeys.STOP_AT_END] ?: false }
            .distinctUntilChanged()

    /**
     * Maximum ExoPlayer disk cache size in bytes. `-1L` means unlimited (`NoOpCacheEvictor`).
     * Defaults to 2 GiB.
     */
    val exoPlayerDiskCacheMaxSize: Flow<Long> =
        ds.data
            .map { it[PreferencesKeys.EXO_PLAYER_DISK_CACHE_MAX_SIZE] ?: DEFAULT_DISK_CACHE_MAX_SIZE_BYTES }
            .distinctUntilChanged()

    /** Sleep timer duration (minutes) pre-selected in the player UI. Defaults to 15. */
    val defaultSleepTimerMinutes: Flow<Int> =
        ds.data
            .map { it[PreferencesKeys.DEFAULT_SLEEP_TIMER_MINUTES] ?: DEFAULT_SLEEP_TIMER_MINUTES }
            .distinctUntilChanged()

    /**
     * Remaining sleep-timer time in milliseconds, persisted so an armed timer can survive a
     * service restart. `null` = no timer armed.
     */
    val sleepTimerRemainingMs: Flow<Long?> =
        ds.data
            .map { it[PreferencesKeys.SLEEP_TIMER_REMAINING_MS] }
            .distinctUntilChanged()

    /** GitHub release version the user chose to skip in the update dialog; `null` = none. */
    val lastUpdateCheckSkippedVersion: Flow<String?> =
        ds.data
            .map { it[PreferencesKeys.LAST_UPDATE_CHECK_SKIPPED_VERSION] }
            .distinctUntilChanged()

    /** Whether local search history is recorded. Defaults to `true`. */
    val searchHistoryEnabled: Flow<Boolean> =
        ds.data
            .map { it[PreferencesKeys.SEARCH_HISTORY_ENABLED] ?: true }
            .distinctUntilChanged()

    /** Persists [DarkMode]. Suspending so callers can await the write (e.g. before an UI restart). */
    suspend fun setDarkMode(value: DarkMode) {
        ds.edit { it[PreferencesKeys.DARK_MODE] = value.name }
    }

    fun setDynamicColor(value: Boolean) = editAsync(PreferencesKeys.DYNAMIC_COLOR, value)

    fun setPureBlack(value: Boolean) = editAsync(PreferencesKeys.PURE_BLACK, value)

    fun setSkipSilence(value: Boolean) = editAsync(PreferencesKeys.SKIP_SILENCE, value)

    fun setNormalization(value: Boolean) = editAsync(PreferencesKeys.NORMALIZATION, value)

    fun setPersistentQueue(value: Boolean) = editAsync(PreferencesKeys.PERSISTENT_QUEUE, value)

    fun setHideExplicit(value: Boolean) = editAsync(PreferencesKeys.HIDE_EXPLICIT, value)

    fun setSquigglyProgress(value: Boolean) = editAsync(PreferencesKeys.SQUIGGLY_PROGRESS, value)

    fun setStopAtEnd(value: Boolean) = editAsync(PreferencesKeys.STOP_AT_END, value)

    fun setExoPlayerDiskCacheMaxSize(value: Long) = editAsync(PreferencesKeys.EXO_PLAYER_DISK_CACHE_MAX_SIZE, value)

    fun setDefaultSleepTimerMinutes(value: Int) = editAsync(PreferencesKeys.DEFAULT_SLEEP_TIMER_MINUTES, value)

    /** Persists the remaining sleep-timer time; passing `null` clears the armed timer. */
    fun setSleepTimerRemainingMs(value: Long?) {
        scope.launch {
            ds.edit { prefs ->
                if (value == null) {
                    prefs.remove(PreferencesKeys.SLEEP_TIMER_REMAINING_MS)
                } else {
                    prefs[PreferencesKeys.SLEEP_TIMER_REMAINING_MS] = value
                }
            }
        }
    }

    /** Persists the skipped update-check version; passing `null` clears the skip. */
    fun setLastUpdateCheckSkippedVersion(value: String?) {
        scope.launch {
            ds.edit { prefs ->
                if (value == null) {
                    prefs.remove(PreferencesKeys.LAST_UPDATE_CHECK_SKIPPED_VERSION)
                } else {
                    prefs[PreferencesKeys.LAST_UPDATE_CHECK_SKIPPED_VERSION] = value
                }
            }
        }
    }

    fun setSearchHistoryEnabled(value: Boolean) = editAsync(PreferencesKeys.SEARCH_HISTORY_ENABLED, value)

    private fun <T> editAsync(key: Key<T>, value: T) {
        scope.launch { ds.edit { it[key] = value } }
    }

    companion object {
        /** 2 GiB — matches the CONTRACTS §4 default for the ExoPlayer disk cache. */
        const val DEFAULT_DISK_CACHE_MAX_SIZE_BYTES: Long = 2L * 1024 * 1024 * 1024

        /** Default sleep timer selection, in minutes. */
        const val DEFAULT_SLEEP_TIMER_MINUTES: Int = 15
    }
}
