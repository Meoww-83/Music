package com.abhiram.music.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Internal DataStore preference keys.
 *
 * These names are an implementation detail of [AppPreferences]; nothing outside the
 * `constants` package should reference them — use the typed [AppPreferences] API instead.
 */
internal object PreferencesKeys {
    val DARK_MODE = stringPreferencesKey("dark_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val PURE_BLACK = booleanPreferencesKey("pure_black")
    val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
    val NORMALIZATION = booleanPreferencesKey("normalization")
    val PERSISTENT_QUEUE = booleanPreferencesKey("persistent_queue")
    val HIDE_EXPLICIT = booleanPreferencesKey("hide_explicit")
    val SQUIGGLY_PROGRESS = booleanPreferencesKey("squiggly_progress")
    val STOP_AT_END = booleanPreferencesKey("stop_at_end")
    val EXO_PLAYER_DISK_CACHE_MAX_SIZE = longPreferencesKey("exo_player_disk_cache_max_size")
    val DEFAULT_SLEEP_TIMER_MINUTES = intPreferencesKey("default_sleep_timer_minutes")
    val SLEEP_TIMER_REMAINING_MS = longPreferencesKey("sleep_timer_remaining_ms")
    val LAST_UPDATE_CHECK_SKIPPED_VERSION = stringPreferencesKey("last_update_check_skipped_version")
    val SEARCH_HISTORY_ENABLED = booleanPreferencesKey("search_history_enabled")
}
