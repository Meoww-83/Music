package com.abhiram.music.playback

import com.abhiram.music.database.DatabaseDao
import com.abhiram.music.database.entities.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Accumulates per-item listen time from player position updates and applies the playback
 * statistics rules from the spec (§4):
 *
 * * more than [MIN_TOTAL_PLAY_TIME_MS] (5 s) of a song heard ⇒ increase its total play time;
 * * more than [HISTORY_EVENT_MIN_MS] (30 s) heard ⇒ additionally insert an [Event] (listening
 *   history entry with timestamp + play duration).
 *
 * Feed it from the player like this:
 * ```kotlin
 * override fun onMediaItemTransition(mediaItem: MediaItem?) {
 *     tracker.onMediaItemTransition(mediaItem?.mediaId)
 * }
 * override fun onPositionDiscontinuity(...) { tracker.onPositionChanged(player.currentPosition) }
 * // …periodically, and on pause/seek:
 * tracker.onPositionChanged(player.currentPosition)
 * ```
 *
 * Position advances larger than [SEEK_JUMP_THRESHOLD_MS] are treated as seeks and do **not**
 * count as listening time; backward seeks simply resynchronise the cursor.
 *
 * All methods are non-blocking — DAO writes run on [Dispatchers.IO] inside this tracker's own
 * [CoroutineScope].
 */
class PlaybackStatsTracker(private val dao: DatabaseDao) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var songId: String? = null
    private var lastPositionMs: Long = 0L
    private var listenedMs: Long = 0L

    /**
     * Called when playback transitions to a new media item. Flushes the accumulated stats of
     * the previous item, then starts tracking [songId] (`null` clears tracking).
     */
    fun onMediaItemTransition(songId: String?) {
        flush()
        this.songId = songId
        lastPositionMs = 0L
        listenedMs = 0L
    }

    /**
     * Called with the current playback position (ms) while an item is playing. Counts the
     * forward delta towards the item's listened time unless it looks like a seek.
     */
    fun onPositionChanged(positionMs: Long) {
        if (songId == null) return
        val delta = positionMs - lastPositionMs
        when {
            delta in 1..SEEK_JUMP_THRESHOLD_MS -> listenedMs += delta
            delta > SEEK_JUMP_THRESHOLD_MS -> Unit // seek forward: resync, count nothing
        }
        if (positionMs > lastPositionMs) lastPositionMs = positionMs
    }

    /** Flushes the current item's stats and stops tracking (e.g. on stop/destroy). */
    fun reset() {
        flush()
        songId = null
        lastPositionMs = 0L
        listenedMs = 0L
    }

    /**
     * Persists the accumulated listened time of the current item (if it crosses the
     * thresholds) and zeroes the accumulator. The caller re-assigns [songId] afterwards.
     */
    private fun flush() {
        val id = songId ?: return
        val total = listenedMs
        songId = null
        lastPositionMs = 0L
        listenedMs = 0L
        if (total > MIN_TOTAL_PLAY_TIME_MS) {
            scope.launch { dao.increaseTotalPlayTime(id, total) }
        }
        if (total > HISTORY_EVENT_MIN_MS) {
            scope.launch {
                dao.insert(
                    Event(
                        songId = id,
                        timestamp = System.currentTimeMillis(),
                        playDuration = total,
                    ),
                )
            }
        }
    }

    private companion object {
        /** > 5 s heard ⇒ count towards total play time. */
        const val MIN_TOTAL_PLAY_TIME_MS: Long = 5_000L

        /** > 30 s heard ⇒ also record a listening-history [Event]. */
        const val HISTORY_EVENT_MIN_MS: Long = 30_000L

        /**
         * Forward position jumps larger than this are considered seeks, not listening —
         * they must not inflate the play-time statistics.
         */
        const val SEEK_JUMP_THRESHOLD_MS: Long = 10_000L
    }
}
