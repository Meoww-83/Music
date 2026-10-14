package com.abhiram.music.service

import com.abhiram.music.playback.PlaybackError
import java.io.IOException

/**
 * Failures raised while `ResolvingDataSource` translates a `videoId` into a playable stream URL
 * (CONTRACTS §4 / §7).
 *
 * Media3 has no built-in "resolving exception" type, so the resolver throws this sealed [IOException]
 * hierarchy instead. The exception travels up through ExoPlayer's loader and surfaces inside the
 * `PlaybackException` cause chain delivered to `Player.Listener.onPlayerError`, where
 * [PlayerControllerImpl] unwraps it with [toPlaybackError] and publishes the matching member of the
 * frozen [PlaybackError] sealed class. Messages never contain stream URLs.
 */
sealed class StreamResolutionException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause) {

    /** The data spec carried neither a cache key nor a usable video id URI. */
    class MissingVideoId : StreamResolutionException("Resolution request is missing a video id")

    /** `playabilityStatus.status == UNPLAYABLE` — video is blocked, deleted or region-locked. */
    class Unplayable(reason: String?) :
        StreamResolutionException(reason?.let { "Stream is unplayable: $it" } ?: "Stream is unplayable")

    /** `playabilityStatus.status == LOGIN_REQUIRED` — the content needs an signed-in account. */
    class LoginRequired(reason: String?) :
        StreamResolutionException(reason?.let { "Login required: $it" } ?: "Login required for this stream")

    /** InnerTube answered for a different video than the one we asked for. */
    class VideoIdMismatch(requested: String, actual: String) :
        StreamResolutionException("Video id mismatch (requested $requested, got $actual)")

    /** No streaming data, or no format that carries a direct URL. */
    class NoFormat : StreamResolutionException("No playable stream format available")

    /** Connectivity failure while talking to InnerTube. */
    class Network(
        message: String?,
        cause: Throwable? = null,
    ) : StreamResolutionException(message ?: "Network error while resolving the stream", cause)

    /** Any other resolution failure (unexpected payload, parser error, …). */
    class Generic(
        message: String?,
        cause: Throwable? = null,
    ) : StreamResolutionException(message ?: "Stream resolution failed", cause)

    /** Maps this failure onto the frozen [PlaybackError] hierarchy surfaced by the player UI. */
    fun toPlaybackError(): PlaybackError = when (this) {
        is MissingVideoId -> PlaybackError.VideoIdMismatch
        is VideoIdMismatch -> PlaybackError.VideoIdMismatch
        is Unplayable -> PlaybackError.Unplayable
        is LoginRequired -> PlaybackError.LoginRequired
        is NoFormat -> PlaybackError.NoFormat
        is Network -> PlaybackError.Network(message)
        is Generic -> PlaybackError.Unknown(message)
    }

    companion object {
        /** Wraps a [Throwable] escaping `YouTube.player(...)` into a resolution failure. */
        fun fromInnerTubeFailure(throwable: Throwable): StreamResolutionException = when (throwable) {
            is IOException -> Network(throwable.message, throwable)
            else -> Generic(throwable.message, throwable)
        }
    }
}
