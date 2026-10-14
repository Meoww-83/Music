package com.abhiram.music.repository

import com.abhiram.innertube.YouTube
import com.abhiram.innertube.models.BrowseEndpoint
import com.abhiram.music.database.DatabaseDao
import com.abhiram.music.database.entities.AlbumEntity
import com.abhiram.music.database.entities.ArtistEntity
import com.abhiram.music.database.entities.LyricsEntity
import com.abhiram.music.database.entities.Playlist
import com.abhiram.music.database.entities.PlaylistEntity
import com.abhiram.music.database.entities.PlaylistSongMap
import com.abhiram.music.database.entities.SearchHistory
import com.abhiram.music.database.entities.Song
import com.abhiram.music.database.entities.SongAlbumMap
import com.abhiram.music.database.entities.SongArtistMap
import com.abhiram.music.model.MediaMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.util.UUID

/**
 * Local music library repository: likes, library membership, local playlists, playlist
 * membership, search history and cached lyrics.
 *
 * The local Room database is the **source of truth** for all of the above; YouTube library
 * mutations ([YouTube.likeVideo], [YouTube.addSongToLibrary], [YouTube.addToPlaylist],
 * [YouTube.deletePlaylist]) are best-effort mirroring calls whose failures are swallowed
 * (`runCatching`) — the local state has already been committed when they are issued.
 */
class MusicRepository(private val dao: DatabaseDao) {

    /* ---------------------------------------------------------------------------------------
     *  Likes
     * ------------------------------------------------------------------------------------ */

    /**
     * Flips the like state of [item], persists it locally and mirrors it to the YouTube Music
     * library (best effort).
     *
     * @return the new liked state.
     */
    suspend fun toggleLike(item: MediaMetadata): Boolean {
        val newLiked = !item.liked
        val likedAt = if (newLiked) LocalDateTime.now() else null
        persistSong(item, liked = newLiked, likedDate = likedAt)
        dao.updateLike(item.id, newLiked, likedAt)
        // Best effort: the local DB is the source of truth.
        runCatching { YouTube.likeVideo(item.id, newLiked) }
        return newLiked
    }

    /** Reactive list of locally liked songs, ordered like [DatabaseDao.likedSongs]. */
    fun likedSongs(): Flow<List<Song>> = dao.likedSongs().distinctUntilChanged()

    /* ---------------------------------------------------------------------------------------
     *  Library membership
     * ------------------------------------------------------------------------------------ */

    /**
     * Marks [item] as added to the local library and best-effort mirrors the add to the
     * YouTube Music library.
     */
    suspend fun addToLibrary(item: MediaMetadata) {
        persistSong(item, inLibrary = LocalDateTime.now())
        dao.addSongToLibrary(item.id)
        runCatching { YouTube.addSongToLibrary(item.id) }
    }

    /* ---------------------------------------------------------------------------------------
     *  Local playlists (browseId == null)
     * ------------------------------------------------------------------------------------ */

    /** Only playlists owned by the device (`PlaylistEntity.browseId == null`). */
    fun localPlaylists(): Flow<List<Playlist>> =
        dao.playlists()
            .map { playlists -> playlists.filter { it.browseId == null } }
            .distinctUntilChanged()

    /** Creates (and persists) a new empty local playlist; returns the stored entity. */
    suspend fun createLocalPlaylist(name: String): PlaylistEntity {
        val now = LocalDateTime.now()
        val playlist =
            PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim().ifEmpty { DEFAULT_PLAYLIST_NAME },
                thumbnailUrl = null,
                description = null,
                browseId = null,
                createdAt = now,
                lastUpdatedAt = now,
            )
        dao.insert(playlist)
        return playlist
    }

    /**
     * Appends [items] to a playlist (local or cached remote), inserting each song — with its
     * artist/album relations — into the library first. Duplicates (same song id already in the
     * playlist) are ignored.
     */
    suspend fun addSongsToPlaylist(playlistId: String, items: List<MediaMetadata>) {
        if (items.isEmpty()) return
        var nextOrder = dao.nextPlaylistSongOrder(playlistId)
        val addedAt = LocalDateTime.now()
        for (item in items) {
            persistSong(item)
            if (dao.playlistSongMap(playlistId, item.id) == null) {
                dao.insert(
                    PlaylistSongMap(
                        playlistId = playlistId,
                        songId = item.id,
                        id = nextOrder++,
                        setVideoId = item.setVideoId,
                        addedAt = addedAt,
                    ),
                )
            }
        }
    }

    /** Removes one song from a playlist's local membership. */
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        dao.deletePlaylistSong(playlistId, songId)
    }

    /** Reactive, playlist-ordered songs of a playlist (local or cached remote). */
    fun songsInPlaylist(playlistId: String): Flow<List<Song>> =
        dao.playlistSongs(playlistId).distinctUntilChanged()

    /* ---------------------------------------------------------------------------------------
     *  Remote playlists
     * ------------------------------------------------------------------------------------ */

    /**
     * Adds [item] to a YouTube Music playlist: updates the local cache for an instant UI
     * refresh and best-effort mirrors the add to the backend.
     */
    suspend fun addToRemotePlaylist(browseId: String, item: MediaMetadata) {
        persistSong(item)
        if (dao.playlistSongMap(browseId, item.id) == null) {
            dao.insert(
                PlaylistSongMap(
                    playlistId = browseId,
                    songId = item.id,
                    id = dao.nextPlaylistSongOrder(browseId),
                    setVideoId = item.setVideoId,
                    addedAt = LocalDateTime.now(),
                ),
            )
        }
        runCatching { YouTube.addToPlaylist(browseId, item.id) }
    }

    /** Deletes a YouTube Music playlist remotely (best effort) and drops its local cache. */
    suspend fun deleteRemotePlaylist(id: String) {
        runCatching { YouTube.deletePlaylist(id) }
        dao.deletePlaylist(id)
    }

    /* ---------------------------------------------------------------------------------------
     *  Search history
     * ------------------------------------------------------------------------------------ */

    /** Reactive search history, newest first (as provided by [DatabaseDao.searchHistory]). */
    fun searchHistory(): Flow<List<SearchHistory>> = dao.searchHistory().distinctUntilChanged()

    /**
     * Records [query] in the local search history, moving it to the top if it already exists.
     * Blank queries are ignored.
     */
    suspend fun pushSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        dao.deleteSearchHistory(trimmed)
        dao.insert(SearchHistory(query = trimmed, createdAt = LocalDateTime.now()))
    }

    /* ---------------------------------------------------------------------------------------
     *  Lyrics
     * ------------------------------------------------------------------------------------ */

    /**
     * Returns the lyrics for [videoId]: cache-first ([DatabaseDao.lyricsOf]), otherwise fetched
     * from YouTube Music via the `MUSIC_video_{videoId}` lyrics browse endpoint, persisted as a
     * [LyricsEntity] and returned.
     *
     * The fetched text is classified by looking for LRC timestamps: timestamped text is stored
     * as synced lyrics, anything else as plain lyrics.
     *
     * @return the cached/fetched lyrics, or `null` when YouTube has none for this video.
     */
    suspend fun lyrics(videoId: String): LyricsEntity? {
        dao.lyricsOf(videoId)?.let { return it }
        val text =
            YouTube
                .lyrics(BrowseEndpoint(browseId = "MUSIC_video_$videoId"))
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: return null
        val entity =
            if (SYNCED_LYRICS_REGEX.containsMatchIn(text)) {
                LyricsEntity(songId = videoId, plainLyrics = null, syncedLyrics = text, source = LYRICS_SOURCE)
            } else {
                LyricsEntity(songId = videoId, plainLyrics = text, syncedLyrics = null, source = LYRICS_SOURCE)
            }
        dao.insert(entity)
        return entity
    }

    /* ---------------------------------------------------------------------------------------
     *  Internals
     * ------------------------------------------------------------------------------------ */

    /**
     * Upserts [item] as a [com.abhiram.music.database.entities.SongEntity] together with its
     * artist/album rows and map entries, preserving local-only state (like, library membership,
     * play time) that a fresh Innertube item cannot carry.
     */
    private suspend fun persistSong(
        item: MediaMetadata,
        liked: Boolean? = null,
        likedDate: LocalDateTime? = null,
        inLibrary: LocalDateTime? = null,
    ) {
        val existing = dao.songEntityById(item.id)
        val effectiveLiked = liked ?: existing?.liked ?: false
        dao.insert(
            item
                .toSongEntity()
                .copy(
                    liked = effectiveLiked,
                    // When the caller drives the like state explicitly (liked != null),
                    // likedDate is taken verbatim — un-liking must clear it, not resurrect it.
                    likedDate =
                        if (liked != null) {
                            likedDate
                        } else {
                            likedDate ?: existing?.likedDate
                        },
                    inLibrary = inLibrary ?: existing?.inLibrary,
                    libraryAddToken = item.libraryAddToken ?: existing?.libraryAddToken,
                    libraryRemoveToken = item.libraryRemoveToken ?: existing?.libraryRemoveToken,
                    totalPlayTimeMs = existing?.totalPlayTimeMs ?: 0L,
                ),
        )
        val artists = item.artists.filter { !it.id.isNullOrBlank() }
        if (artists.isNotEmpty()) {
            dao.insert(
                artists.map {
                    ArtistEntity(
                        id = it.id!!,
                        name = it.name,
                        thumbnailUrl = null,
                        subscriberCountText = null,
                    )
                },
            )
            dao.insert(
                artists.mapIndexed { index, artist ->
                    SongArtistMap(songId = item.id, artistId = artist.id!!, sortIndex = index)
                },
            )
        }
        val album = item.album
        if (album != null) {
            dao.insert(
                listOf(
                    AlbumEntity(
                        id = album.id,
                        title = album.title,
                        thumbnailUrl = null,
                        year = null,
                        songCountText = null,
                    ),
                ),
            )
            dao.insert(SongAlbumMap(songId = item.id, albumId = album.id, sortIndex = 0))
        }
    }

    private companion object {
        /** LRC-style timestamp marker, e.g. `[01:23.456]`. */
        val SYNCED_LYRICS_REGEX: Regex = Regex("""\[\d{1,3}:\d{2}(\.\d+)?]""")

        const val LYRICS_SOURCE: String = "YouTube"
        const val DEFAULT_PLAYLIST_NAME: String = "New playlist"
    }
}
