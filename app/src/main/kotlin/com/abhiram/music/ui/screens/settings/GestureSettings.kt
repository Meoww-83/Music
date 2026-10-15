package com.abhiram.music.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.SwipeUp
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.gesturePreferencesDataStore by preferencesDataStore(name = "gesture_preferences")

/**
 * Gesture-only preferences that have no counterpart in the frozen `AppPreferences` API (§3.5).
 *
 * Kept in this package (screen-local DataStore, file `gesture_preferences`) so the constants
 * package stays owned by a single agent. Setters write on the IO dispatcher.
 */
class GesturePreferences(context: Context) {
    private val dataStore: DataStore<Preferences> = context.applicationContext.gesturePreferencesDataStore
    private val scope = CoroutineScope(Dispatchers.IO)

    /** Seconds seeked per double-tap on the seek bar sides. */
    val doubleTapSeekSeconds: Flow<Int> =
        dataStore.map { it[Keys.DoubleTapSeekSeconds] ?: 10 }

    /** Whether a long press opens the item menu instead of playing immediately. */
    val longPressForOptions: Flow<Boolean> =
        dataStore.map { it[Keys.LongPressForOptions] ?: true }

    /** Multiplier applied to seek gestures (double-tap and swipe). */
    val seekSensitivity: Flow<Float> =
        dataStore.map { it[Keys.SeekSensitivity] ?: 1f }

    /** Whether swiping up on the mini player opens the full player. */
    val swipeUpOpensPlayer: Flow<Boolean> =
        dataStore.map { it[Keys.SwipeUpOpensPlayer] ?: true }

    /** Whether gesture interactions emit haptic feedback. */
    val hapticsEnabled: Flow<Boolean> =
        dataStore.map { it[Keys.HapticsEnabled] ?: true }

    fun setDoubleTapSeekSeconds(value: Int) = edit { it[Keys.DoubleTapSeekSeconds] = value.coerceIn(5, 30) }

    fun setLongPressForOptions(value: Boolean) = edit { it[Keys.LongPressForOptions] = value }

    fun setSeekSensitivity(value: Float) = edit { it[Keys.SeekSensitivity] = value.coerceIn(0.5f, 2f) }

    fun setSwipeUpOpensPlayer(value: Boolean) = edit { it[Keys.SwipeUpOpensPlayer] = value }

    fun setHapticsEnabled(value: Boolean) = edit { it[Keys.HapticsEnabled] = value }

    private fun edit(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch { dataStore.edit(block) }
    }

    private object Keys {
        val DoubleTapSeekSeconds = intPreferencesKey("double_tap_seek_seconds")
        val LongPressForOptions = booleanPreferencesKey("long_press_for_options")
        val SeekSensitivity = floatPreferencesKey("seek_sensitivity")
        val SwipeUpOpensPlayer = booleanPreferencesKey("swipe_up_opens_player")
        val HapticsEnabled = booleanPreferencesKey("haptics_enabled")
    }
}

/**
 * Gesture settings (route `settings/gesture`): double-tap seek distance, long-press menus,
 * seek sensitivity, the mini-player swipe-up gesture and haptics — backed by the local
 * [GesturePreferences] DataStore.
 */
@Composable
fun GestureSettings(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val gestures = viewModel.gestures

    val doubleTapSeekSeconds by gestures.doubleTapSeekSeconds.collectAsStateWithLifecycle(initialValue = 10)
    val longPressForOptions by gestures.longPressForOptions.collectAsStateWithLifecycle(initialValue = true)
    val seekSensitivity by gestures.seekSensitivity.collectAsStateWithLifecycle(initialValue = 1f)
    val swipeUpOpensPlayer by gestures.swipeUpOpensPlayer.collectAsStateWithLifecycle(initialValue = true)
    val hapticsEnabled by gestures.hapticsEnabled.collectAsStateWithLifecycle(initialValue = true)

    SettingsPage(
        title = "Gestures",
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingsGroup(title = "Seeking") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "Double-tap seek",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "How many seconds a double-tap on the seek bar jumps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30).forEach { seconds ->
                        FilterChip(
                            selected = doubleTapSeekSeconds == seconds,
                            onClick = { gestures.setDoubleTapSeekSeconds(seconds) },
                            label = { Text("$seconds s") },
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row {
                    Text(
                        text = "Seek sensitivity",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "%.1f×".format(seekSensitivity),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Scales the distance of every seek gesture",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = seekSensitivity,
                    onValueChange = gestures::setSeekSensitivity,
                    valueRange = 0.5f..2f,
                    steps = 14,
                )
            }
        }

        SettingsGroup(title = "Interaction") {
            SwitchSettingEntry(
                title = "Long-press for options",
                checked = longPressForOptions,
                onCheckedChange = gestures::setLongPressForOptions,
                icon = Icons.Rounded.TouchApp,
                description = "Long-press a song to open its menu instead of playing it",
            )
            SwitchSettingEntry(
                title = "Swipe up on mini player",
                checked = swipeUpOpensPlayer,
                onCheckedChange = gestures::setSwipeUpOpensPlayer,
                icon = Icons.Rounded.SwipeUp,
                description = "Swipe the mini player up to open the full player",
            )
            SwitchSettingEntry(
                title = "Haptic feedback",
                checked = hapticsEnabled,
                onCheckedChange = gestures::setHapticsEnabled,
                icon = Icons.Rounded.Vibration,
                description = "Vibrate lightly for gesture confirmations",
            )
        }

        SettingsInformation(
            text = "Double-tap seek, mini-player swipes and gesture haptics apply wherever the player surface is visible — the seek bar, mini player and full player.",
            icon = Icons.Rounded.Forward10,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
