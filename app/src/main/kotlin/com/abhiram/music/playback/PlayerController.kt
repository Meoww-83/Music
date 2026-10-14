package com.abhiram.music.playback

import androidx.compose.runtime.Immutable
import com.abhiram.music.model.MediaMetadata
import kotlinx.coroutines.flow.StateFlow

/** Repeat behaviour exposed to the UI, mapped 1:1 onto Media3's `Player.REPEAT_MODE_*`. */
enum class RepeatMode { OFF, ONE, ALL }

/** Fatal/non fatal playback failures surfaced by the player screen. */
@Immutable
sealed class PlaybackError {
    data object Unplayable : PlaybackError()

    data object LoginRequired : PlaybackError()

    data object VideoIdMismatch : PlaybackError()

    data object NoFormat : PlaybackError()

    data class Network(val message: String?) : PlaybackError()

    data class Unknown(val message: String?) : PlaybackError()
}

/** Snapshot of everything the UI needs to render the (mini) player. */
@Immutable
data class PlayerState(
    val current: MediaMetadata? = null,
    val queue: List<MediaMetadata> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val error: PlaybackError? = null,
    /** Progress (0f..1f) of the sleep timer, `null` when no timer is armed. */
    val sleepTimerProgress: Float? = null,
) {
    val hasQueue: Boolean get() = queue.isNotEmpty()
}

/**
 * The one entry point every screen uses to control playback.
 *
 * The implementation lives inside [com.abhiram.music.service.PlayerService]; the UI resolves the
 * active instance through [PlayerControllerProvider].
 */
interface PlayerController {
    val state: StateFlow<PlayerState>

    /** Plays [items] starting at [startIndex] and replaces the current queue. */
    fun playAll(items: List<MediaMetadata>, startIndex: Int = 0, shuffle: Boolean = false)

    /** Convenience for a single track. */
    fun play(item: MediaMetadata) = playAll(listOf(item), 0)

    fun togglePlayPause()

    fun pause()

    fun resume()

    fun seekTo(positionMs: Long)

    fun seekToNext()

    fun seekToPrevious()

    fun toggleShuffle()

    fun setRepeatMode(mode: RepeatMode)

    /** Appends [item] to the end of the queue. */
    fun addToQueue(item: MediaMetadata)

    /** Inserts [item] right after the current track. */
    fun playNext(item: MediaMetadata)

    fun removeQueueItem(index: Int)

    fun moveQueueItem(fromIndex: Int, toIndex: Int)

    fun clearQueue()

    /** Persists + applies the like state for the current track. */
    fun toggleLike()

    /** True when the current track is liked. */
    fun isCurrentLiked(): Boolean

    /** Arms (value > 0) or cancels (value == null) the sleep timer, in milliseconds. */
    fun setSleepTimer(durationMillis: Long?)

    fun stop()

    /** Releases the service connection; called from the Application/Activity teardown. */
    fun destroy()
}

/**
 * Process wide holder for the active [PlayerController].
 *
 * [com.abhiram.music.service.PlayerService] installs its implementation when it starts and clears
 * it when it is destroyed. Screens use [rememberPlayerController] which transparently falls back
 * to a no-op controller while the service is still coming up — and, for actions that must run,
 * [with] which cold-starts the service and replays the action once it is installed.
 */
object PlayerControllerProvider {
    private val _controller = kotlinx.coroutines.flow.MutableStateFlow<PlayerController?>(null)
    val controller: kotlinx.coroutines.flow.StateFlow<PlayerController?> = _controller

    /** Action queued while the playback service is still starting. */
    @Volatile
    private var pending: ((PlayerController) -> Unit)? = null

    fun install(controller: PlayerController) {
        _controller.value = controller
        val queued = synchronized(this) { pending.also { pending = null } }
        queued?.invoke(controller)
    }

    fun clear(controller: PlayerController) {
        if (_controller.value === controller) _controller.value = null
    }

    /**
     * Runs [action] against the live controller, cold-starting
     * [com.abhiram.music.service.PlayerService] first when it is not running yet (the action is
     * replayed the moment the service installs its controller). This is the entry point screens
     * should use instead of `controller?.playAll(...)` so the very first play always works.
     */
    fun with(
        context: android.content.Context,
        action: (PlayerController) -> Unit,
    ) {
        val current = _controller.value
        if (current != null) {
            action(current)
            return
        }
        synchronized(this) { pending = action }
        runCatching {
            val intent = android.content.Intent(context.applicationContext, com.abhiram.music.service.PlayerService::class.java)
            android.content.ContextCompat.startForegroundService(context.applicationContext, intent)
        }.onFailure {
            synchronized(this) { pending = null }
        }
    }

    /** Convenience: start playing [items] (cold-starting the service when needed). */
    fun playAll(
        context: android.content.Context,
        items: List<MediaMetadata>,
        startIndex: Int = 0,
        shuffle: Boolean = false,
    ) = with(context) { it.playAll(items, startIndex, shuffle) }
}

/** A safe, inert implementation used before the playback service is available. */
object NoOpPlayerController : PlayerController {
    override val state: kotlinx.coroutines.flow.StateFlow<PlayerState> =
        kotlinx.coroutines.flow.MutableStateFlow(PlayerState())

    override fun playAll(items: List<MediaMetadata>, startIndex: Int, shuffle: Boolean) = Unit
    override fun togglePlayPause() = Unit
    override fun pause() = Unit
    override fun resume() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun seekToNext() = Unit
    override fun seekToPrevious() = Unit
    override fun toggleShuffle() = Unit
    override fun setRepeatMode(mode: RepeatMode) = Unit
    override fun addToQueue(item: MediaMetadata) = Unit
    override fun playNext(item: MediaMetadata) = Unit
    override fun removeQueueItem(index: Int) = Unit
    override fun moveQueueItem(fromIndex: Int, toIndex: Int) = Unit
    override fun clearQueue() = Unit
    override fun toggleLike() = Unit
    override fun isCurrentLiked(): Boolean = false
    override fun setSleepTimer(durationMillis: Long?) = Unit
    override fun stop() = Unit
    override fun destroy() = Unit
}
