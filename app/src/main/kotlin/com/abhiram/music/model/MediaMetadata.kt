package com.abhiram.music.model

import androidx.compose.runtime.Immutable
import com.abhiram.innertube.models.AlbumItem
import com.abhiram.innertube.models.ArtistItem
import com.abhiram.innertube.models.EpisodeItem
import com.abhiram.innertube.models.PlaylistItem
import com.abhiram.innertube.models.SongItem
import com.abhiram.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.abhiram.music.database.entities.Song
import com.abhiram.music.database.entities.SongEntity
import com.abhiram.music.utils.resize
import java.io.Serializable
import java.time.LocalDateTime

/**
 * The single, UI/player facing representation of a playable track.
 *
 * It is intentionally decoupled from both the InnerTube response models and the Room entities so
 * that the UI layer never has to care where a track came from (YouTube, local database, cache...).
 */
@Immutable
data class MediaMetadata(
    val id: String,
    val title: String,
    val artists: List<Artist>,
    val duration: Int,
    val thumbnailUrl: String? = null,
    val album: Album? = null,
    val setVideoId: String? = null,
    val musicVideoType: String? = null,
    val explicit: Boolean = false,
    val liked: Boolean = false,
    val likedDate: LocalDateTime? = null,
    val inLibrary: LocalDateTime? = null,
    val libraryAddToken: String? = null,
    val libraryRemoveToken: String? = null,
    val suggestedBy: String? = null,
    val isEpisode: Boolean = false,
    val uploadEntityId: String? = null,
) : Serializable {
    val isVideoSong: Boolean
        get() = musicVideoType != null && musicVideoType != MUSIC_VIDEO_TYPE_ATV

    val shareLink: String
        get() = "https://music.youtube.com/watch?v=$id"

    data class Artist(
        val id: String?,
        val name: String,
    ) : Serializable

    data class Album(
        val id: String,
        val title: String,
    ) : Serializable

    fun toSongEntity() =
        SongEntity(
            id = id,
            title = title,
            duration = duration,
            thumbnailUrl = thumbnailUrl,
            albumId = album?.id,
            albumName = album?.title,
            explicit = explicit,
            liked = liked,
            likedDate = likedDate,
            inLibrary = inLibrary,
            libraryAddToken = libraryAddToken,
            libraryRemoveToken = libraryRemoveToken,
            isVideo = isVideoSong,
            isEpisode = isEpisode,
            uploadEntityId = uploadEntityId,
        )

    fun toSongItem() =
        SongItem(
            id = id,
            title = title,
            artists = artists.map { com.abhiram.innertube.models.Artist(name = it.name, id = it.id) },
            album = album?.let { com.abhiram.innertube.models.Album(name = it.title, id = it.id) },
            duration = duration,
            musicVideoType = musicVideoType,
            thumbnail = thumbnailUrl.orEmpty(),
            explicit = explicit,
            setVideoId = setVideoId,
            isEpisode = isEpisode,
            uploadEntityId = uploadEntityId,
        )
}

fun SongItem.toMediaMetadata() =
    MediaMetadata(
        id = id,
        title = title,
        artists = artists.map { MediaMetadata.Artist(id = it.id, name = it.name) },
        duration = duration ?: 0,
        thumbnailUrl = thumbnail.resize(),
        album = album?.let { MediaMetadata.Album(id = it.id, title = it.name) },
        setVideoId = setVideoId,
        musicVideoType = musicVideoType,
        explicit = explicit,
        libraryAddToken = libraryAddToken,
        libraryRemoveToken = libraryRemoveToken,
        isEpisode = isEpisode,
        uploadEntityId = uploadEntityId,
    )

fun EpisodeItem.toMediaMetadata() = asSongItem().toMediaMetadata()

/** Converts a locally stored [Song] (with its ordered relations) into [MediaMetadata]. */
fun Song.toMediaMetadata() =
    MediaMetadata(
        id = song.id,
        title = song.title,
        artists =
            orderedArtists.map {
                MediaMetadata.Artist(id = it.id, name = it.name)
            },
        duration = song.duration,
        thumbnailUrl = song.thumbnailUrl,
        album =
            album?.let { MediaMetadata.Album(id = it.id, title = it.title) }
                ?: song.albumId?.let { albumId ->
                    MediaMetadata.Album(id = albumId, title = song.albumName.orEmpty())
                },
        explicit = song.explicit,
        musicVideoType = if (song.isVideo) "MUSIC_VIDEO_TYPE_OMV" else null,
        isEpisode = song.isEpisode,
    )

/* -------------------------------------------------------------------------------------------
 *  Helpers used while mapping remote catalogue objects (search / artist / album / playlist)
 * ---------------------------------------------------------------------------------------- */

fun AlbumItem.thumbnailList() = listOf(thumbnail.resize())

fun PlaylistItem.thumbnailList() = listOfNotNull(thumbnail?.resize())
