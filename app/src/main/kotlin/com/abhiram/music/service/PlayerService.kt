package com.abhiram.music.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.abhiram.music.MainActivity
import com.abhiram.music.R
import com.abhiram.music.constants.Logs
import com.abhiram.music.constants.NotificationChannels
import com.abhiram.music.constants.NotificationIds
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.model.toMediaMetadata
import com.abhiram.music.playback.PlayerControllerProvider
import com.abhiram.music.playback.PlayerState
import com.abhiram.music.utils.YoutubeRadio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import androidx.media3.session.R as Media3R

/** Sub-directory of `cacheDir` holding the ExoPlayer `SimpleCache` (CONTRACTS §4). */
private const val EXO_CACHE_DIRECTORY = "exoplayer"

/** Auto-radio fires once at most this many items remain **after** the current one. */
private const val RADIO_EXTEND_THRESHOLD = 3

/** Minimum pause between radio requests after a successful batch (avoids request bursts). */
private const val RADIO_SUCCESS_GAP_MS = 500L

/** Backoff after a failed/empty radio request (the state ticker fires every 250 ms). */
private const val RADIO_RETRY_BACKOFF_MS = 15_000L

/** Preferences read once, synchronously, while the service starts up. */
private data class StartupPrefs(
    val cacheMaxBytes: Long,
    val skipSilence: Boolean,
    val stopAtEnd: Boolean,
)

/** A queue recovered from the `QueuedMediaItem` table, ready for [PlayerControllerImpl.restoreQueue]. */
private data class RestoredQueue(
    val items: List<MediaMetadata>,
    val startIndex: Int,
    val startPositionMs: Long,
)

/**
 * The Media3 playback service (CONTRACTS §4).
 *
 * Responsibilities, in lifecycle order:
 *
 *  * **`onCreate`** — creates the `"playback"` notification channel and immediately promotes the
 *    service with a placeholder notification (satisfies the 5-second `startForegroundService()`
 *    window before any I/O happens), then:
 *    * builds the `SimpleCache` in `cacheDir/exoplayer` (a `StandaloneDatabaseProvider` plus a
 *      `LeastRecentlyUsedCacheEvictor` sized by `exoPlayerDiskCacheMaxSize`, or a
 *      `NoOpCacheEvictor` when the preference is `-1` = unlimited);
 *    * builds the [ExoPlayer] with `DefaultMediaSourceFactory(createDataSourceFactory())`, audio
 *      attributes `USAGE_MEDIA`/`CONTENT_TYPE_MUSIC` (with audio focus), `setHandleAudioBecomingNoisy`,
 *      `WAKE_MODE_LOCAL`, repeat mode and skip-silence;
 *    * builds the [MediaSession] with a session-activity [PendingIntent] to [MainActivity];
 *    * installs [PlayerControllerImpl] through [PlayerControllerProvider];
 *    * observes preferences (skip-silence, `stopAtEnd`), restores the persisted queue and appends
 *      auto-radio items through [YoutubeRadio] whenever ≤3 items remain after the current one.
 *  * **`onTaskRemoved`** — persists the queue synchronously, then defers to Media3 (which keeps a
 *    playing session alive and stops an idle one).
 *  * **`onDestroy`** — clears the installed controller, releases the session, player and disk
 *    cache, and cancels any lingering notification.
 *
 * Notifications are produced by Media3's [DefaultMediaNotificationProvider] configured with the
 * shared [NotificationIds.PLAYER] / [NotificationChannels.PLAYER] ids, so the placeholder posted at
 * startup is replaced in place by the real media notification as soon as playback state changes.
 * Stream URLs are never logged anywhere in this service or its resolver.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlayerService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Radio session driving [YoutubeRadio] appends; single-flight via [radioJob]. */
    private val youtubeRadio = YoutubeRadio()

    private var diskCache: SimpleCache? = null
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var controller: PlayerControllerImpl? = null
    private var radioJob: Job? = null

    /** Mirrors the `stopAtEnd` preference; `true` disables auto-radio extension. */
    @Volatile
    private var stopAtEndEnabled: Boolean = false

    /** `elapsedRealtime` floor for the next radio request (backoff/gap between attempts). */
    private var radioNextAttemptMs: Long = 0L

    @Volatile
    private var serviceStopped: Boolean = false

    /* -------------------------------------------------------------------------------------- *
     * Lifecycle
     * -------------------------------------------------------------------------------------- */

    override fun onCreate() {
        super.onCreate()
        Timber.tag(Logs.SERVICE).d("Playback service starting")

        // Android 12+ can refuse background foreground-service starts; Media3 reports them here.
        setListener(
            object : MediaSessionService.Listener {
                override fun onForegroundServiceStartNotAllowedException() {
                    Timber.tag(Logs.SERVICE).w("Foreground start not allowed; stopping service")
                    stopSelf()
                }
            },
        )

        createNotificationChannel()
        // Promote before any I/O: PlayerControllerProvider.with() cold-starts this service with
        // Context.startForegroundService(), which requires startForeground() within ~5 s.
        if (!startForegroundSafely(buildStartupNotification())) return

        val startup = runBlocking(Dispatchers.IO) { readStartupPrefs() }
        stopAtEndEnabled = startup.stopAtEnd

        val cache = createDiskCache(startup.cacheMaxBytes).also { diskCache = it }
        val exoPlayer = buildPlayer(cache, startup.skipSilence).also { player = it }

        // Configure the provider first so the lazily-created MediaNotificationManager picks it up:
        // Media3 posts the real media notification with the same id/channel as our placeholder.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setNotificationId(NotificationIds.PLAYER)
                .setChannelId(NotificationChannels.PLAYER)
                .setChannelName(R.string.app_name)
                .build(),
        )

        val session =
            MediaSession.Builder(exoPlayer)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                .build()
        mediaSession = session
        // Register the session right away. The UI drives playback in-process through
        // PlayerControllerProvider and may never open a MediaController of its own — without this
        // the MediaNotificationManager would never watch the player, so no media notification or
        // foreground promotion would ever happen (the startup placeholder would linger instead).
        addSession(session)

        val controllerImpl = PlayerControllerImpl(exoPlayer).also { impl ->
            impl.onQueueReplaced = { seed ->
                // A brand-new queue re-anchors the radio; in-flight results of the old one die here.
                radioJob?.cancel()
                radioNextAttemptMs = 0L
                youtubeRadio.seed(seed?.id)
            }
            controller = impl
        }
        PlayerControllerProvider.install(controllerImpl)

        serviceScope.launch {
            ServiceLocator.container.preferences.skipSilence.collect { enabled ->
                exoPlayer.skipSilenceEnabled = enabled
            }
        }
        serviceScope.launch {
            ServiceLocator.container.preferences.stopAtEnd.collect { enabled ->
                stopAtEndEnabled = enabled
            }
        }
        serviceScope.launch {
            controllerImpl.state.collect { state -> maybeExtendRadio(state) }
        }
        // Restore last, after the controller is installed, and let Media3 re-evaluate the
        // notification/foreground state once we know whether a queue exists.
        serviceScope.launch { restorePersistedQueue(controllerImpl) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Capture the playhead before Media3 possibly tears everything down (CONTRACTS §4:
        // queue persistence via QueuedMediaItem).
        controller?.persistNow()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceStopped = true
        serviceScope.cancel()

        // Never leave a stale player notification behind once the session is gone.
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        runCatching { getSystemService(NotificationManager::class.java)?.cancel(NotificationIds.PLAYER) }

        controller?.destroy() // persists the final queue and clears PlayerControllerProvider
        controller = null
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        diskCache?.release()
        diskCache = null

        super.onDestroy()
        Timber.tag(Logs.SERVICE).d("Playback service destroyed")
    }

    /* -------------------------------------------------------------------------------------- *
     * Construction helpers
     * -------------------------------------------------------------------------------------- */

    /** Reads the three startup preferences in one blocking pass (DataStore, Dispatchers.IO). */
    private suspend fun readStartupPrefs(): StartupPrefs {
        val preferences = ServiceLocator.container.preferences
        return StartupPrefs(
            cacheMaxBytes = preferences.exoPlayerDiskCacheMaxSize.first(),
            skipSilence = preferences.skipSilence.first(),
            stopAtEnd = preferences.stopAtEnd.first(),
        )
    }

    /**
     * The contract-mandated disk cache: `cacheDir/exoplayer` with a `StandaloneDatabaseProvider`,
     * an LRU evictor sized by the preference, or `NoOpCacheEvictor` when the size is `-1`
     * (unlimited).
     */
    private fun createDiskCache(maxSizeBytes: Long): SimpleCache {
        val evictor =
            if (maxSizeBytes < 0L) {
                NoOpCacheEvictor()
            } else {
                LeastRecentlyUsedCacheEvictor(maxSizeBytes)
            }
        return SimpleCache(
            File(cacheDir, EXO_CACHE_DIRECTORY),
            evictor,
            StandaloneDatabaseProvider(applicationContext),
        )
    }

    /** `DefaultMediaSourceFactory(ResolvingDataSource(CacheDataSource(… → HttpDataSource)))`. */
    private fun createDataSourceFactory(cache: SimpleCache): DataSource.Factory =
        ResolvingDataSourceFactory(cache)

    private fun buildPlayer(cache: SimpleCache, skipSilence: Boolean): ExoPlayer =
        ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(createDataSourceFactory(cache)))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .also { exoPlayer ->
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.skipSilenceEnabled = skipSilence
            }

    /* -------------------------------------------------------------------------------------- *
     * Notification / foreground plumbing
     * -------------------------------------------------------------------------------------- */

    /** Creates the shared `"playback"` channel (IMPORTANCE_LOW: silent transport updates). */
    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(NotificationChannels.PLAYER) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    NotificationChannels.PLAYER,
                    NotificationChannels.PLAYER_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    /**
     * Minimal ongoing notification posted synchronously from `onCreate` so the
     * `startForegroundService()` contract is met before cache/player construction runs. Media3
     * replaces it in place (same [NotificationIds.PLAYER] id) as soon as playback state changes.
     */
    private fun buildStartupNotification(): Notification {
        createNotificationChannel()
        val contentIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat.Builder(this, NotificationChannels.PLAYER)
            .setSmallIcon(Media3R.drawable.media3_notification_small_icon)
            .setContentTitle(getString(R.string.app_name))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    /**
     * Enters the foreground with the media-playback service type. Returns `false` when the system
     * refuses (Android 12+ background-start restrictions, API 34+ type violations) — the service
     * stops itself instead of letting the system ANR the process.
     */
    private fun startForegroundSafely(notification: Notification): Boolean =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NotificationIds.PLAYER,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(NotificationIds.PLAYER, notification)
            }
            true
        } catch (e: RuntimeException) {
            // ForegroundServiceStartNotAllowedException (31+) / SecurityException (34+).
            Timber.tag(Logs.SERVICE).e(e, "Could not enter foreground; stopping service")
            stopSelf()
            false
        }

    /* -------------------------------------------------------------------------------------- *
     * Queue restore
     * -------------------------------------------------------------------------------------- */

    /**
     * Loads the persisted `QueuedMediaItem` rows (when `persistentQueue` is enabled), resolves
     * their songs and hands them to [PlayerControllerImpl.restoreQueue] — only while the player is
     * still empty, so a user pressing play first always wins. Finishes by letting Media3 re-check
     * notification/foreground state.
     */
    private suspend fun restorePersistedQueue(controllerImpl: PlayerControllerImpl) {
        try {
            val restored = withContext(Dispatchers.IO) { loadPersistedQueue() }
            if (restored != null && !serviceStopped && player?.mediaItemCount == 0) {
                controllerImpl.restoreQueue(
                    restored.items,
                    restored.startIndex,
                    restored.startPositionMs,
                )
                Timber.tag(Logs.SERVICE).d("Restored %d persisted queue items", restored.items.size)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(Logs.SERVICE).w(e, "Could not restore the persisted queue")
        } finally {
            if (!serviceStopped) triggerNotificationUpdate()
        }
    }

    /** Rebuilds the queue from Room; returns `null` when persistence is off or nothing is stored. */
    private suspend fun loadPersistedQueue(): RestoredQueue? {
        val container = ServiceLocator.container
        if (!container.preferences.persistentQueue.first()) return null
        val rows = container.dao.getQueue()
        if (rows.isEmpty()) return null
        val songsById = container.dao.songsByIds(rows.map { it.mediaId }).associateBy { it.id }

        val items = ArrayList<MediaMetadata>(rows.size)
        var startIndex = 0
        var startPositionMs = 0L
        for (row in rows) {
            // Songs deleted from the DB since the last run drop out of the restored queue.
            val song = songsById[row.mediaId] ?: continue
            val rowPosition = row.positionMs
            if (rowPosition != null) {
                startIndex = items.size
                startPositionMs = rowPosition
            }
            items.add(song.toMediaMetadata())
        }
        if (items.isEmpty()) return null
        return RestoredQueue(items = items, startIndex = startIndex, startPositionMs = startPositionMs)
    }

    /* -------------------------------------------------------------------------------------- *
     * Auto-radio
     * -------------------------------------------------------------------------------------- */

    /**
     * CONTRACTS §4: when ≤3 items remain after the current one, fetch the next [YoutubeRadio]
     * batch on this (main) coroutine and append it to the ExoPlayer queue.
     *
     * Guarded so it only runs while playback is actually engaged (playing/buffering/ended — not a
     * restored-but-idle queue, not after a fatal error), never concurrently ([radioJob] is
     * single-flight), and never faster than the configured backoff after a failed request.
     * `stopAtEnd` disables extension entirely.
     */
    private fun maybeExtendRadio(state: PlayerState) {
        if (stopAtEndEnabled) return
        val currentPlayer = player ?: return
        val currentIndex = state.currentIndex
        if (currentIndex < 0 || state.queue.isEmpty()) return
        if (state.queue.size - currentIndex - 1 > RADIO_EXTEND_THRESHOLD) return
        if (!isPlaybackEngaged(currentPlayer)) return
        if (radioJob?.isActive == true) return
        if (SystemClock.elapsedRealtime() < radioNextAttemptMs) return

        radioJob =
            serviceScope.launch {
                val items = youtubeRadio.process()
                radioNextAttemptMs =
                    SystemClock.elapsedRealtime() +
                        if (items.isEmpty()) RADIO_RETRY_BACKOFF_MS else RADIO_SUCCESS_GAP_MS
                if (items.isNotEmpty()) {
                    val appended = controller?.appendRadioItems(items) ?: 0
                    Timber.tag(Logs.SERVICE).d("Radio appended %d queue items", appended)
                }
            }
    }

    /** True while the player is actively progressing (or has just run out of items). */
    private fun isPlaybackEngaged(currentPlayer: ExoPlayer): Boolean =
        when (currentPlayer.playbackState) {
            Player.STATE_BUFFERING,
            Player.STATE_ENDED,
            -> true
            Player.STATE_READY -> currentPlayer.playWhenReady
            else -> false
        }
}
