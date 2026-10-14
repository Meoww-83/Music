package com.abhiram.music.utils

import com.abhiram.innertube.YouTube
import com.abhiram.innertube.models.WatchEndpoint
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.model.toMediaMetadata
import timber.log.Timber

private const val TAG = "YoutubeRadio"

/**
 * Continuation-driven radio that grows a playing queue with related tracks (CONTRACTS §4).
 *
 * The service owns one instance, seeds it with the first track of a queue via [seed] and calls
 * [process] whenever ≤3 items remain after the current one. [process] performs a single
 * `YouTube.next(...)` request (falling back to `YouTube.related(...)` on the very first call when
 * the queue panel comes back empty), converts the page into [MediaMetadata] items and re-arms the
 * continuation token for the next call.
 *
 * Loop protection: every continuation token that is successfully sent is remembered and never sent
 * again; a server response that echoes an already-used token immediately disarms the continuation.
 * If the endpoint itself fails, the token is kept (a later call may retry it) and an empty list is
 * returned — [process] never throws.
 */
class YoutubeRadio(
    videoId: String? = null,
    playlistId: String? = null,
    playlistSetVideoId: String? = null,
    params: String? = null,
) {
    /** The watch endpoint the next request is built from; updated from every response. */
    var endpoint: WatchEndpoint = WatchEndpoint(
        videoId = videoId,
        playlistId = playlistId,
        playlistSetVideoId = playlistSetVideoId,
        params = params,
    )
        private set

    /** Seed/current video id of the radio session. */
    val videoId: String? get() = endpoint.videoId

    /** Playlist id of the radio session, `null` until the first request upgrades it to `RDAMVM…`. */
    val playlistId: String? get() = endpoint.playlistId

    /** Playlist set-video id forwarded to InnerTube, when the seed provided one. */
    val playlistSetVideoId: String? get() = endpoint.playlistSetVideoId

    /** Watch params forwarded to InnerTube, when the seed provided one. */
    val params: String? get() = endpoint.params

    /** The re-armed continuation token; `null` while a fresh endpoint request is due. */
    val continuation: String? get() = nextContinuation

    private var nextContinuation: String? = null
    private val usedContinuations = LinkedHashSet<String>()
    private val returnedIds = LinkedHashSet<String>()

    /**
     * (Re-)anchors the radio on [videoId] and drops every page of state — continuation token,
     * seen tokens and already-returned ids. Call this when a brand new queue starts; do **not**
     * call it while a continuation chain is in progress or the session would restart each track.
     */
    fun seed(
        videoId: String?,
        playlistId: String? = null,
        playlistSetVideoId: String? = null,
        params: String? = null,
    ) {
        endpoint = WatchEndpoint(
            videoId = videoId,
            playlistId = playlistId,
            playlistSetVideoId = playlistSetVideoId,
            params = params,
        )
        nextContinuation = null
        usedContinuations.clear()
        returnedIds.clear()
    }

    /**
     * Fetches the next batch of radio items.
     *
     * Returns the tracks that this radio has **not** returned before (empty list when there is
     * nothing new). Never throws; a failed or repeated request just disarms/preserves the
     * continuation as described in the class docs. Callers must serialize concurrent invocations —
     * `PlayerService` guards this with a single in-flight job.
     */
    suspend fun process(): List<MediaMetadata> {
        val token = nextContinuation
        if (token == null && endpoint.videoId == null) return emptyList()
        if (token != null && token in usedContinuations) {
            // The server handed back a token we already consumed — stop instead of looping.
            nextContinuation = null
            return emptyList()
        }

        // First request without an explicit playlist: ask for the automotive radio mix so YouTube
        // returns a real radio panel instead of a static "up next" list.
        val requestEndpoint =
            if (token == null && endpoint.playlistId == null && endpoint.videoId != null) {
                endpoint.copy(playlistId = "RDAMVM${endpoint.videoId}")
            } else {
                endpoint
            }

        val result = YouTube.next(requestEndpoint, token).getOrElse { throwable ->
            Timber.tag(TAG).d("Radio request failed: %s", throwable.javaClass.simpleName)
            return emptyList()
        }
        if (token != null) usedContinuations.add(token)

        endpoint = result.endpoint
        nextContinuation = when {
            result.continuation == null -> null
            result.continuation == token -> null // echoed token: re-arming it would loop
            else -> result.continuation
        }

        var items = result.items
        if (token == null && items.size <= 1) {
            // A starved radio panel: fall back to the related-videos page once.
            result.relatedEndpoint?.let { relatedEndpoint ->
                YouTube.related(relatedEndpoint).getOrNull()?.songs?.let { relatedSongs ->
                    if (relatedSongs.isNotEmpty()) items = items + relatedSongs
                }
            }
        }

        val fresh = ArrayList<MediaMetadata>(items.size)
        for (song in items) {
            val metadata = song.toMediaMetadata()
            if (returnedIds.add(metadata.id)) fresh.add(metadata)
        }
        return fresh
    }
}
