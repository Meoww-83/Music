package com.abhiram.music.service

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import com.abhiram.innertube.YouTube
import com.abhiram.innertube.models.response.PlayerResponse
import com.metrolist.innertubex.models.YouTubeClient.Companion.WEB_REMIX
import com.abhiram.music.database.entities.FormatEntity
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.utils.RingBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber

private const val TAG = "StreamResolver"

/**
 * [DataSource.Factory] backing `DefaultMediaSourceFactory` (CONTRACTS §4/§7).
 *
 * It builds `ResolvingDataSource(CacheDataSource(… → DefaultHttpDataSource))` whose resolver
 * implements the full just-in-time resolution flow:
 *
 * 1. **key = videoId** — every [MediaItem][androidx.media3.common.MediaItem] is enqueued with the
 *    raw video id as URI/cache key, so `DataSpec.key` (or the URI) identifies the track. A spec
 *    without one throws [StreamResolutionException.MissingVideoId].
 * 2. **Cache short-circuit** — when the 512 KiB chunk at `dataSpec.position` is already stored in
 *    the app's `SimpleCache`, resolution is skipped entirely and the spec is forwarded unchanged
 *    (the cache serves it by key, so the expired googlevideo URI is never touched).
 * 3. **Ring buffer** — otherwise the 2-entry [RingBuffer] of freshly resolved URLs is consulted
 *    before hitting the network.
 * 4. **InnerTube** — on a miss, `YouTube.player(videoId, …)` runs inside `runBlocking(Dispatchers.IO)`;
 *    the response is validated (playability status, `videoDetails.videoId`), the highest-bitrate
 *    audio format is chosen, its `FormatEntity` is persisted through `ServiceLocator` (best effort),
 *    the URL is stored in the ring buffer and the returned spec points at it — sub-ranged to a
 *    512 KiB chunk so `CacheDataSource` fills the disk cache incrementally.
 *
 * Failure modes map 1:1 onto [StreamResolutionException] (⇒ frozen `PlaybackError`):
 * `UNPLAYABLE`, `LOGIN_REQUIRED`, video-id mismatch, missing formats and generic/network errors.
 * Stream URLs are never written to logcat.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class ResolvingDataSourceFactory(
    private val cache: Cache,
    private val streamUrlCache: RingBuffer<String, String> = RingBuffer(),
) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        val httpFactory =
            DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
                .setReadTimeoutMs(READ_TIMEOUT_MS)
                .setUserAgent(FIREFOX_USER_AGENT)
        val cacheFactory =
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(httpFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return ResolvingDataSource.Factory(cacheFactory) { dataSpec -> resolve(dataSpec) }
                .createDataSource()
    }

    /** The CONTRACTS §7 resolution flow; runs on ExoPlayer's loader thread and may block. */
    private fun resolve(dataSpec: DataSpec): DataSpec {
        val videoId = dataSpec.key?.takeIf { it.isNotBlank() }
            ?: dataSpec.uri.toString().takeIf { it.isNotBlank() && !it.startsWith("http") }
            ?: throw StreamResolutionException.MissingVideoId()

        // 1) The requested 512 KiB chunk may already be on disk — then no URL is needed at all.
        val wantedLength =
            if (dataSpec.length != C.LENGTH_UNSET.toLong()) minOf(dataSpec.length, CHUNK_SIZE_BYTES)
            else CHUNK_SIZE_BYTES
        if (cache.isCached(videoId, dataSpec.position, wantedLength)) {
            return dataSpec
        }

        // 2) Recently resolved for the previous/next chunk of this session?
        streamUrlCache[videoId]?.let { cachedUrl ->
            Timber.tag(TAG).d("Stream for %s taken from ring buffer", videoId)
            return chunkedSpec(dataSpec, cachedUrl, videoId)
        }

        // 3) Fresh InnerTube round-trip.
        val response = runBlocking(Dispatchers.IO) {
            YouTube.player(videoId = videoId, playlistId = null, client = WEB_REMIX)
        }.getOrElse { throwable -> throw StreamResolutionException.fromInnerTubeFailure(throwable) }

        when (val status = response.playabilityStatus.status) {
            PLAYABILITY_OK -> Unit
            PLAYABILITY_UNPLAYABLE ->
                throw StreamResolutionException.Unplayable(response.playabilityStatus.reason)
            PLAYABILITY_LOGIN_REQUIRED ->
                throw StreamResolutionException.LoginRequired(response.playabilityStatus.reason)
            else ->
                throw StreamResolutionException.Generic("Stream unavailable (status $status)")
        }

        val details = response.videoDetails
            ?: throw StreamResolutionException.Generic("Player response carries no video details")
        if (details.videoId != videoId) {
            throw StreamResolutionException.VideoIdMismatch(requested = videoId, actual = details.videoId)
        }

        val streamingData = response.streamingData ?: throw StreamResolutionException.NoFormat()
        val candidates = streamingData.adaptiveFormats + streamingData.formats.orEmpty()
        val format =
            candidates.filter { it.isAudio && it.url != null }.maxByOrNull { it.bitrate }
                ?: candidates.filter { it.url != null }.maxByOrNull { it.bitrate }
                ?: throw StreamResolutionException.NoFormat()
        val url = format.url ?: throw StreamResolutionException.NoFormat()

        persistFormat(videoId, format, response.playerConfig?.audioConfig?.loudnessDb)
        streamUrlCache.put(videoId, url)
        Timber.tag(TAG).d("Resolved stream for %s (itag=%d, %d bps)", videoId, format.itag, format.bitrate)
        return chunkedSpec(dataSpec, url, videoId)
    }

    /**
     * Points [dataSpec] at the resolved [url] under the [videoId] cache key and bounds the request
     * to one 512 KiB chunk (`buildUpon` keeps position/headers, `subrange` applies the length).
     * The key is re-asserted after sub-ranging so `CacheDataSource` always indexes by video id.
     */
    private fun chunkedSpec(dataSpec: DataSpec, url: String, videoId: String): DataSpec {
        val resolved = dataSpec.buildUpon().setUri(Uri.parse(url)).setKey(videoId).build()
        val chunked =
            if (resolved.length == C.LENGTH_UNSET.toLong() || resolved.length > CHUNK_SIZE_BYTES) {
                resolved.subrange(0, CHUNK_SIZE_BYTES)
            } else {
                resolved
            }
        return chunked.buildUpon().setKey(videoId).build()
    }

    /** Best-effort persistence of the chosen format (loudness is consumed by the normalization hook). */
    private fun persistFormat(
        videoId: String,
        format: PlayerResponse.StreamingData.Format,
        responseLoudnessDb: Double?,
    ) {
        runBlocking(Dispatchers.IO) {
            runCatching {
                ServiceLocator.container.dao.insert(
                    FormatEntity(
                        songId = videoId,
                        itag = format.itag,
                        mimeType = format.mimeType.substringBefore(";"),
                        bitrate = format.bitrate.toLong(),
                        loudnessDb = format.loudnessDb ?: responseLoudnessDb,
                        contentLength = format.contentLength,
                        lastModified = format.lastModified?.toString(),
                    ),
                )
            }.onFailure {
                Timber.tag(TAG).w("Could not persist format metadata for %s", videoId)
            }
        }
    }

    companion object {
        /** 512 KiB — both the resolution skip-check granularity and the bounded request size. */
        const val CHUNK_SIZE_BYTES: Long = 512L * 1024L

        private const val CONNECT_TIMEOUT_MS = 16_000
        private const val READ_TIMEOUT_MS = 8_000
        private const val PLAYABILITY_OK = "OK"
        private const val PLAYABILITY_UNPLAYABLE = "UNPLAYABLE"
        private const val PLAYABILITY_LOGIN_REQUIRED = "LOGIN_REQUIRED"
        private const val FIREFOX_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
    }
}
