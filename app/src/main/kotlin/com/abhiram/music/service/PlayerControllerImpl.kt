package com.abhiram.music.service

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.abhiram.music.database.entities.QueuedMediaItem
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.playback.PlaybackError
import com.abhiram.music.playback.PlaybackStatsTracker
import com.abhiram.music.playback.PlayerController
import com.abhiram.music.playback.PlayerControllerProvider
import com.abhiram.music.playback.PlayerState
import com.abhiram.music.playback.RepeatMode
import com.abhiram.music.playback.SleepTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "PlayerController"
private const val POSITION_TICK_MS = 250L
private const val PERSIST_EVERY_TICKS = 20 // ≈ every 5 s of active playback
private const val PREVIOUS_TRACK_THRESHOLD_MS = 3_000L
private const val MAX_CAUSE_DEPTH = 16

/** Latest like state mirrored from Room so menus that edit the DB update the player UI too. */
private data class LikedState(val liked: Boolean, val likedDate: LocalDateTime?)

/**
 * [PlayerController] backed directly by ExoPlayer — the queue *is* the player timeline
 * (CONTRACTS §4).
 *
 * Responsibilities:
 *  * publishes an immutable [PlayerState] snapshot into a [StateFlow]; position advances with a
 *    250 ms ticker (≥4 updates/s) while playing and refreshes synchronously on every
 *    [Player.Listener] event (transition, error, shuffle/repeat, playWhenReady, seek, …);
 *  * maps Media3 events and thrown resolution failures onto the frozen state/error types —
 *    [StreamResolutionException]s are unwrapped from the `PlaybackException` cause chain via
 *    [StreamResolutionException.toPlaybackError];
 *  * feeds [PlaybackStatsTracker] (frozen in CONTRACTS §4): a transition hands over the song id,
 *    the ticker feeds positions while playing, [stop]/[clearQueue]/[destroy] reset it;
 *  * persists the queue through `QueuedMediaItem` DAO writes (latest snapshot wins) and restores
 *    it via [restoreQueue];
 *  * arms [SleepTimer], whose expiry pauses playback.
 *
 * The service installs this instance through [PlayerControllerProvider] and tears it down in
 * `onDestroy`; [destroy] must be called on the main thread.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlayerControllerImpl(
    private val player: ExoPlayer,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) : PlayerController, Player.Listener {

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    /**
     * Service-internal hook fired only when a **brand-new** queue replaces the old one
     * ([playAll], [restoreQueue], [clearQueue]) with the track playback starts on — `PlayerService`
     * re-seeds its [YoutubeRadio] from it. Radio appends deliberately do not fire it.
     */
    var onQueueReplaced: ((MediaMetadata?) -> Unit)? = null

    private val statsTracker: PlaybackStatsTracker by lazy {
        PlaybackStatsTracker(ServiceLocator.container.dao)
    }
    private val sleepTimer = SleepTimer { pause() }

    private var lastError: PlaybackError? = null
    @Volatile
    private var sleepTimerProgress: Float? = null
    @Volatile
    private var persistentQueueEnabled: Boolean = false
    @Volatile
    private var destroyed: Boolean = false

    private val likedOverrides = ConcurrentHashMap<String, LikedState>()
    private val persistChannel = Channel<List<QueuedMediaItem>>(Channel.CONFLATED)
    private var tickCount: Int = 0

    init {
        player.addListener(this)
        scope.launch { positionTicker() }
        scope.launch { sleepTimer.progress.collect { progress -> sleepTimerProgress = progress; publishState() } }
        scope.launch { ServiceLocator.container.preferences.persistentQueue.collect { persistentQueueEnabled = it } }
        scope.launch { observeCurrentLikeState() }
        scope.launch(Dispatchers.IO) {
            for (snapshot in persistChannel) writeQueueSnapshot(snapshot)
        }
    }

    /* -------------------------------------------------------------------------------------- *
     * PlayerController surface
     * -------------------------------------------------------------------------------------- */

    override fun playAll(items: List<MediaMetadata>, startIndex: Int, shuffle: Boolean) {
        if (items.isEmpty()) {
            clearQueue()
            return
        }
        clearError()
        statsTracker.reset()
        val index = startIndex.coerceIn(0, items.lastIndex)
        // Keep timeline indices playlist-aligned while the new playlist is installed, then apply
        // shuffle (Media3 preserves the current item/position across the reorder).
        player.shuffleModeEnabled = false
        player.setMediaItems(items.map { it.toMediaItem() }, index, 0L)
        player.shuffleModeEnabled = shuffle
        player.prepare()
        player.play()
        persistQueue()
        publishState()
        onQueueReplaced?.invoke(items[index])
    }

    override fun togglePlayPause() {
        if (player.isPlaying) pause() else resume()
    }

    override fun pause() {
        player.pause()
        persistQueue()
        publishState()
    }

    override fun resume() {
        clearError()
        when (player.playbackState) {
            Player.STATE_IDLE -> player.prepare() // restored queue or a fatal error before play
            Player.STATE_ENDED -> player.seekTo(player.currentMediaItemIndex.coerceAtLeast(0), 0L)
        }
        player.play()
        publishState()
    }

    override fun seekTo(positionMs: Long) {
        if (player.mediaItemCount == 0) return
        player.seekTo(positionMs.coerceAtLeast(0L))
        persistQueue()
        publishState()
    }

    override fun seekToNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            persistQueue()
            publishState()
        }
    }

    override fun seekToPrevious() {
        if (player.mediaItemCount == 0) return
        if (player.currentPosition > PREVIOUS_TRACK_THRESHOLD_MS) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
        persistQueue()
        publishState()
    }

    override fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        publishState()
    }

    override fun setRepeatMode(mode: RepeatMode) {
        player.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        publishState()
    }

    override fun addToQueue(item: MediaMetadata) {
        if (player.mediaItemCount == 0) {
            playAll(listOf(item), 0, player.shuffleModeEnabled)
            return
        }
        player.addMediaItem(item.toMediaItem())
        persistQueue()
        publishState()
    }

    override fun playNext(item: MediaMetadata) {
        if (player.mediaItemCount == 0) {
            playAll(listOf(item), 0, player.shuffleModeEnabled)
            return
        }
        val insertAt = (currentPlaylistIndex() + 1).coerceIn(0, player.mediaItemCount)
        player.addMediaItem(insertAt, item.toMediaItem())
        persistQueue()
        publishState()
    }

    override fun removeQueueItem(index: Int) {
        if (index !in 0 until player.mediaItemCount) return
        player.removeMediaItem(index)
        if (player.mediaItemCount == 0) {
            player.playWhenReady = false
            statsTracker.reset()
            clearError()
        }
        persistQueue()
        publishState()
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in 0 until player.mediaItemCount) return
        if (toIndex !in 0 until player.mediaItemCount || fromIndex == toIndex) return
        player.moveMediaItem(fromIndex, toIndex)
        persistQueue()
        publishState()
    }

    override fun clearQueue() {
        statsTracker.reset()
        clearError()
        player.clearMediaItems()
        player.playWhenReady = false
        persistQueue()
        publishState()
        onQueueReplaced?.invoke(null)
    }

    override fun toggleLike() {
        val current = state.value.current ?: return
        scope.launch {
            val newLiked = runCatching {
                ServiceLocator.container.musicRepository.toggleLike(current)
            }.getOrNull() ?: current.liked
            val likedDate = if (newLiked) current.likedDate ?: LocalDateTime.now() else null
            likedOverrides[current.id] = LikedState(newLiked, likedDate)
            publishState()
        }
    }

    override fun isCurrentLiked(): Boolean = state.value.current?.liked == true

    override fun setSleepTimer(durationMillis: Long?) {
        if (durationMillis != null && durationMillis > 0L) {
            sleepTimer.start(durationMillis)
        } else {
            sleepTimer.cancel()
        }
        publishState()
    }

    override fun stop() {
        player.stop()
        player.playWhenReady = false
        statsTracker.reset()
        persistQueue()
        publishState()
    }

    override fun destroy() {
        if (destroyed) return
        destroyed = true
        val finalSnapshot = buildQueueSnapshot()
        persistChannel.close()
        scope.cancel()
        player.removeListener(this)
        if (persistentQueueEnabled) runBlocking(Dispatchers.IO) { writeQueueSnapshot(finalSnapshot) }
        PlayerControllerProvider.clear(this)
    }

    /* -------------------------------------------------------------------------------------- *
     * Service-internal API
     * -------------------------------------------------------------------------------------- */

    /**
     * Restores a persisted queue (CONTRACTS §4 restore-on-create). Only applies while the player
     * holds no items, so a user pressing play first always wins. Nothing is prepared or started —
     * playback begins lazily when the user hits play.
     */
    fun restoreQueue(items: List<MediaMetadata>, startIndex: Int, startPositionMs: Long) {
        if (destroyed || items.isEmpty() || player.mediaItemCount > 0) return
        val index = startIndex.coerceIn(0, items.lastIndex)
        player.shuffleModeEnabled = false
        player.playWhenReady = false
        player.setMediaItems(items.map { it.toMediaItem() }, index, startPositionMs.coerceAtLeast(0L))
        // setMediaItems may have kicked off preparation; a restored queue stays lazy (IDLE).
        if (player.playbackState != Player.STATE_IDLE) player.stop()
        publishState()
        onQueueReplaced?.invoke(items[index])
    }

    /**
     * Appends automatic-radio items to the end of the queue, dropping ids already present.
     * Returns how many items were actually appended. Service-internal.
     */
    fun appendRadioItems(items: List<MediaMetadata>): Int {
        if (destroyed || items.isEmpty()) return 0
        val existing = player.mediaItems.mapTo(HashSet()) { it.mediaId }
        val additions = items.filter { it.id !in existing }
        if (additions.isEmpty()) return 0
        player.addMediaItems(additions.map { it.toMediaItem() })
        if (player.playbackState == Player.STATE_ENDED) {
            // The queue ran out before the radio landed — keep the session going.
            player.prepare()
            player.playWhenReady = true
        }
        persistQueue()
        publishState()
        return additions.size
    }

    /**
     * Synchronously writes the current queue to Room. For teardown paths where the process may die
     * immediately afterwards (`onTaskRemoved`); normal changes go through the async channel.
     */
    fun persistNow() {
        if (destroyed || !persistentQueueEnabled) return
        runBlocking(Dispatchers.IO) { writeQueueSnapshot(buildQueueSnapshot()) }
    }

    /* -------------------------------------------------------------------------------------- *
     * Media3 events → PlayerState
     * -------------------------------------------------------------------------------------- */

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_DYNAMIC) persistQueue()
        publishState()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        statsTracker.onMediaItemTransition(mediaItem?.mediaId)
        clearError()
        persistQueue()
        publishState()
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (!playWhenReady) persistQueue()
        publishState()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) persistQueue() // captures the position when playback pauses/stalls
        publishState()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        publishState()
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        publishState()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        publishState()
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) persistQueue()
        publishState()
    }

    override fun onPlayerError(error: PlaybackException) {
        // Never log messages here: source errors may embed stream URLs.
        lastError = mapPlaybackError(error)
        Timber.tag(TAG).e("Player error (code=%d, type=%s)", error.errorCode, error.javaClass.simpleName)
        publishState()
    }

    override fun onEvents(player: Player, events: Player.Events) {
        publishState()
    }

    /* -------------------------------------------------------------------------------------- *
     * Internals
     * -------------------------------------------------------------------------------------- */

    /** ≥4 position updates per second while playing; also drives the stats tracker + queue saves. */
    private suspend fun positionTicker() {
        while (true) {
            delay(POSITION_TICK_MS)
            if (player.isPlaying) {
                val position = player.currentPosition
                statsTracker.onPositionChanged(position)
                _state.update {
                    it.copy(
                        isPlaying = true,
                        positionMs = position.coerceAtLeast(0L),
                        durationMs = resolveDurationMs(it.current),
                    )
                }
                tickCount++
                if (tickCount % PERSIST_EVERY_TICKS == 0) persistQueue()
            }
        }
    }

    /** Mirrors Room like state for the current song so out-of-band DB updates reach the UI. */
    private suspend fun observeCurrentLikeState() {
        state.map { it.current?.id }.distinctUntilChanged().collectLatest { songId ->
            if (songId != null) {
                ServiceLocator.container.dao.songById(songId).collect { song ->
                    if (song != null) {
                        val liked = song.song.liked == true
                        val likedDate = song.song.likedDate
                        val known = likedOverrides[songId]
                        if (known?.liked != liked || known.likedDate != likedDate) {
                            likedOverrides[songId] = LikedState(liked, likedDate)
                            publishState()
                        }
                    }
                }
            }
        }
    }

    private fun publishState() {
        if (destroyed) return
        _state.value = buildState()
    }

    private fun buildState(): PlayerState {
        val mediaItems = player.mediaItems
        val queue = mediaItems.map { metadataOf(it) }
        val current = player.currentMediaItem?.let { metadataOf(it) }
        return PlayerState(
            current = current,
            queue = queue,
            currentIndex = if (current == null) -1 else currentPlaylistIndex(),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = resolveDurationMs(current),
            repeatMode = repeatModeOf(player.repeatMode),
            shuffleEnabled = player.shuffleModeEnabled,
            error = lastError,
            sleepTimerProgress = sleepTimerProgress,
        )
    }

    private fun resolveDurationMs(current: MediaMetadata?): Long {
        val duration = player.duration
        return if (duration != C.TIME_UNSET && duration > 0L) {
            duration
        } else {
            ((current?.duration ?: 0).toLong()) * 1000L
        }
    }

    /** Index of the current item inside [Player.getMediaItems] (i.e. inside [state]'s queue). */
    private fun currentPlaylistIndex(): Int {
        val current = player.currentMediaItem ?: return -1
        if (!player.shuffleModeEnabled) {
            val windowIndex = player.currentMediaItemIndex
            if (windowIndex in 0 until player.mediaItemCount) return windowIndex
        }
        return player.mediaItems.indexOfFirst { it.mediaId == current.mediaId }
    }

    /** Recovers the [MediaMetadata] carried in the item's `mediaMetadata.tag`, else a placeholder. */
    private fun metadataOf(mediaItem: MediaItem): MediaMetadata {
        val base = mediaItem.mediaMetadata.tag as? MediaMetadata
            ?: MediaMetadata(id = mediaItem.mediaId, title = mediaItem.mediaId, artists = emptyList(), duration = 0)
        val override = likedOverrides[base.id] ?: return base
        return base.copy(liked = override.liked, likedDate = override.likedDate)
    }

    /**
     * Builds the canonical [MediaItem] for a track: `mediaId == uri == cache key == videoId`, plus
     * session-notification metadata (title / artist / artwork) and the model as `tag` so the queue
     * can be reconstructed losslessly from the timeline.
     */
    private fun MediaMetadata.toMediaItem(): MediaItem {
        val artistLine = artists.joinToString(", ") { it.name }.ifBlank { null }
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(id)
            .setCustomCacheKey(id)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artistLine)
                    .setAlbumTitle(album?.title)
                    .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
                    .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setIsPlayable(true)
                    .setTag(this)
                    .build(),
            )
            .build()
    }

    private fun repeatModeOf(@Player.RepeatMode mode: Int): RepeatMode = when (mode) {
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        else -> RepeatMode.OFF
    }

    private fun clearError() {
        if (lastError != null) {
            lastError = null
            publishState()
        }
    }

    /** Walks the `PlaybackException` cause chain for resolver failures, else maps by error code. */
    private fun mapPlaybackError(error: PlaybackException): PlaybackError {
        var cause: Throwable? = error
        var depth = 0
        while (cause != null && depth < MAX_CAUSE_DEPTH) {
            if (cause is StreamResolutionException) return cause.toPlaybackError()
            cause = cause.cause
            depth++
        }
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_REMOTE_ERROR,
            -> PlaybackError.Network(error.message)
            else -> PlaybackError.Unknown(error.message)
        }
    }

    /** Snapshot for Room: ordered ids, with the playhead position stored on the current row. */
    private fun buildQueueSnapshot(): List<QueuedMediaItem> {
        if (player.mediaItemCount == 0) return emptyList()
        val currentIndex = currentPlaylistIndex()
        return player.mediaItems.mapIndexed { index, item ->
            QueuedMediaItem(
                id = index,
                mediaId = item.mediaId,
                positionMs = if (index == currentIndex) player.currentPosition else null,
            )
        }
    }

    private fun writeQueueSnapshot(snapshot: List<QueuedMediaItem>) {
        runCatching {
            val dao = ServiceLocator.container.dao
            if (snapshot.isEmpty()) dao.clearQueue() else dao.saveQueue(snapshot)
        }.onFailure {
            Timber.tag(TAG).w("Failed to persist playback queue")
        }
    }

    /** Latest-snapshot-wins: builds the list on the player thread, writes it on Dispatchers.IO. */
    private fun persistQueue() {
        if (destroyed || !persistentQueueEnabled) return
        persistChannel.trySend(buildQueueSnapshot())
    }
}
