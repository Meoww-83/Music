package com.abhiram.music.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.abhiram.music.database.entities.Album
import com.abhiram.music.database.entities.AlbumArtistMap
import com.abhiram.music.database.entities.AlbumEntity
import com.abhiram.music.database.entities.AlbumWithSongs
import com.abhiram.music.database.entities.Artist
import com.abhiram.music.database.entities.ArtistEntity
import com.abhiram.music.database.entities.Event
import com.abhiram.music.database.entities.EventWithSong
import com.abhiram.music.database.entities.FormatEntity
import com.abhiram.music.database.entities.LyricsEntity
import com.abhiram.music.database.entities.PlayCountEntity
import com.abhiram.music.database.entities.Playlist
import com.abhiram.music.database.entities.PlaylistEntity
import com.abhiram.music.database.entities.PlaylistSongMap
import com.abhiram.music.database.entities.PlaylistWithSongs
import com.abhiram.music.database.entities.QueuedMediaItem
import com.abhiram.music.database.entities.SearchHistory
import com.abhiram.music.database.entities.Song
import com.abhiram.music.database.entities.SongAlbumMap
import com.abhiram.music.database.entities.SongArtistMap
import com.abhiram.music.database.entities.SongEntity
import com.abhiram.music.database.entities.SongWithStats
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import kotlin.jvm.JvmName

/**
 * The single DAO of the app: songs, artists, albums, playlists, search history, listening
 * events, stream formats, queue persistence, lyrics and play counts.
 *
 * Conventions:
 * * every method that only reads is either `Flow` (reactive) or `suspend` (one-shot);
 * * songs are ordered by title, playlist songs by [PlaylistSongMap.id], album/artist songs by
 *   the corresponding map's `sortIndex`;
 * * list-flavoured `insert(...)` overloads erase to the same JVM signature, so each one is a
 *   final wrapper with a distinct `@JvmName` delegating to a distinctly named abstract method.
 */
@Dao
abstract class DatabaseDao {

    /* ----------------------------------------------------------------------------------------
     * Songs
     * ------------------------------------------------------------------------------------- */

    /** Inserts (or replaces) a single [SongEntity] row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(song: SongEntity)

    /** Inserts (or replaces) many [SongEntity] rows. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSongs(songs: List<SongEntity>)

    /** Inserts (or replaces) many [SongEntity] rows. */
    @JvmName("insertSongList")
    suspend fun insert(songs: List<SongEntity>) = insertSongs(songs)

    /** Inserts (or replaces) a single [ArtistEntity] row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(artist: ArtistEntity)

    /** Inserts (or replaces) many [ArtistEntity] rows. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertArtists(artists: List<ArtistEntity>)

    /** Inserts (or replaces) many [ArtistEntity] rows. */
    @JvmName("insertArtistList")
    suspend fun insert(artists: List<ArtistEntity>) = insertArtists(artists)

    /** Inserts (or replaces) a single [AlbumEntity] row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(album: AlbumEntity)

    /** Inserts (or replaces) many [AlbumEntity] rows. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAlbums(albums: List<AlbumEntity>)

    /** Inserts (or replaces) many [AlbumEntity] rows. */
    @JvmName("insertAlbumList")
    suspend fun insert(albums: List<AlbumEntity>) = insertAlbums(albums)

    /** Inserts (or replaces) a song → artist join row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(map: SongArtistMap)

    /** Inserts (or replaces) song → artist join rows. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSongArtistMaps(maps: List<SongArtistMap>)

    /** Inserts (or replaces) song → artist join rows. */
    @JvmName("insertSongArtistMapList")
    suspend fun insert(maps: List<SongArtistMap>) = insertSongArtistMaps(maps)

    /** Inserts (or replaces) a song → album join row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(map: SongAlbumMap)

    /** Inserts (or replaces) song → album join rows. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSongAlbumMaps(maps: List<SongAlbumMap>)

    /** Inserts (or replaces) song → album join rows. */
    @JvmName("insertSongAlbumMapList")
    suspend fun insert(maps: List<SongAlbumMap>) = insertSongAlbumMaps(maps)

    /** Inserts (or replaces) an album → artist join row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(map: AlbumArtistMap)

    /** Inserts (or replaces) album → artist join rows. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAlbumArtistMaps(maps: List<AlbumArtistMap>)

    /** Inserts (or replaces) album → artist join rows. */
    @JvmName("insertAlbumArtistMapList")
    suspend fun insert(maps: List<AlbumArtistMap>) = insertAlbumArtistMaps(maps)

    /** Reactively observes a single song with its resolved relations, `null` when unknown. */
    @Transaction
    @Query("SELECT * FROM song WHERE id = :id LIMIT 1")
    abstract fun songById(id: String): Flow<Song?>

    /** One-shot lookup of the raw song row, `null` when unknown. */
    @Query("SELECT * FROM song WHERE id = :id LIMIT 1")
    abstract suspend fun songEntityById(id: String): SongEntity?

    /** Reactively observes every cached song, ordered by title. */
    @Transaction
    @Query("SELECT * FROM song ORDER BY title ASC")
    abstract fun songs(): Flow<List<Song>>

    /** Reactively observes every liked song, ordered by title. */
    @Transaction
    @Query("SELECT * FROM song WHERE liked = 1 ORDER BY title ASC")
    abstract fun likedSongs(): Flow<List<Song>>

    /** One-shot lookup of many songs (e.g. to rebuild a queue), ordered by title. */
    @Transaction
    @Query("SELECT * FROM song WHERE id IN (:ids) ORDER BY title ASC")
    abstract suspend fun songsByIds(ids: List<String>): List<Song>

    /** Deletes the song row itself; called from [deleteSong]. */
    @Query("DELETE FROM song WHERE id = :songId")
    protected abstract suspend fun deleteSongRow(songId: String)

    /** Deletes the song → artist join rows of a song; called from [deleteSong]. */
    @Query("DELETE FROM song_artist_map WHERE songId = :songId")
    protected abstract suspend fun deleteSongArtistMapsOfSong(songId: String)

    /** Deletes the song → album join rows of a song; called from [deleteSong]. */
    @Query("DELETE FROM song_album_map WHERE songId = :songId")
    protected abstract suspend fun deleteSongAlbumMapsOfSong(songId: String)

    /** Removes a deleted song from every playlist; called from [deleteSong]. */
    @Query("DELETE FROM playlist_song_map WHERE songId = :songId")
    protected abstract suspend fun deletePlaylistSongMapsOfSong(songId: String)

    /** Deletes the listening events of a song; called from [deleteSong]. */
    @Query("DELETE FROM event WHERE songId = :songId")
    protected abstract suspend fun deleteEventsOfSong(songId: String)

    /** Deletes the cached stream format of a song; called from [deleteSong]. */
    @Query("DELETE FROM format WHERE songId = :songId")
    protected abstract suspend fun deleteFormatOfSong(songId: String)

    /** Deletes the cached lyrics of a song; called from [deleteSong]. */
    @Query("DELETE FROM lyrics WHERE songId = :songId")
    protected abstract suspend fun deleteLyricsOfSong(songId: String)

    /** Deletes the play count row of a song; called from [deleteSong]. */
    @Query("DELETE FROM play_count WHERE songId = :songId")
    protected abstract suspend fun deletePlayCountOfSong(songId: String)

    /* -------------------------------------------------------------------------------
     *  Whole-table wipes used by the database settings screen
     * --------------------------------------------------------------------------- */

    /** Deletes every listening event (the "play history" shown in History). */
    @Query("DELETE FROM event")
    abstract suspend fun clearEvents()

    /** Deletes every play-count row (Top played rankings). */
    @Query("DELETE FROM play_count")
    abstract suspend fun clearPlayCounts()

    /**
     * Deletes a song and every dependent row (join maps, playlist membership, events,
     * format, lyrics and play count) in one transaction.
     */
    @Transaction
    open suspend fun deleteSong(songId: String) {
        deletePlaylistSongMapsOfSong(songId)
        deleteSongArtistMapsOfSong(songId)
        deleteSongAlbumMapsOfSong(songId)
        deleteEventsOfSong(songId)
        deleteFormatOfSong(songId)
        deleteLyricsOfSong(songId)
        deletePlayCountOfSong(songId)
        deleteSongRow(songId)
    }

    /** Updates the like state (and its timestamp) of a song. */
    @Query("UPDATE song SET liked = :liked, likedDate = :likedDate WHERE id = :id")
    abstract suspend fun updateLike(id: String, liked: Boolean?, likedDate: LocalDateTime?)

    /** Marks a song as part of the library at [at]. */
    @Query("UPDATE song SET inLibrary = :at WHERE id = :id")
    abstract suspend fun addSongToLibrary(id: String, at: LocalDateTime = LocalDateTime.now())

    /** Removes a song from the library (keeps the row itself). */
    @Query("UPDATE song SET inLibrary = NULL WHERE id = :id")
    abstract suspend fun removeSongFromLibrary(id: String)

    /** Adds [ms] milliseconds to the accumulated play time of a song. */
    @Query("UPDATE song SET totalPlayTimeMs = totalPlayTimeMs + :ms WHERE id = :id")
    abstract suspend fun increaseTotalPlayTime(id: String, ms: Long)

    /* ----------------------------------------------------------------------------------------
     * Artists
     * ------------------------------------------------------------------------------------- */

    /** Reactively observes every cached artist, ordered by name. */
    @Transaction
    @Query("SELECT * FROM artist ORDER BY name ASC")
    abstract fun artists(): Flow<List<Artist>>

    /** Reactively observes a single artist with its cached songs, `null` when unknown. */
    @Transaction
    @Query("SELECT * FROM artist WHERE id = :id LIMIT 1")
    abstract fun artistById(id: String): Flow<Artist?>

    /** One-shot lookup of the raw artist row, `null` when unknown. */
    @Query("SELECT * FROM artist WHERE id = :id LIMIT 1")
    abstract suspend fun artistEntityById(id: String): ArtistEntity?

    /* ----------------------------------------------------------------------------------------
     * Albums
     * ------------------------------------------------------------------------------------- */

    /** Reactively observes every cached album, ordered by title. */
    @Transaction
    @Query("SELECT * FROM album ORDER BY title ASC")
    abstract fun albums(): Flow<List<Album>>

    /** Reactively observes a single album with its cached songs, `null` when unknown. */
    @Transaction
    @Query("SELECT * FROM album WHERE id = :id LIMIT 1")
    abstract fun albumById(id: String): Flow<Album?>

    /** One-shot lookup of the raw album row, `null` when unknown. */
    @Query("SELECT * FROM album WHERE id = :id LIMIT 1")
    abstract suspend fun albumEntityById(id: String): AlbumEntity?

    /** Reactively observes an album's songs, ordered by track number. */
    @Transaction
    @Query(
        """
        SELECT song.* FROM song
        INNER JOIN song_album_map ON song_album_map.songId = song.id
        WHERE song_album_map.albumId = :albumId
        ORDER BY song_album_map.sortIndex ASC
        """,
    )
    abstract fun albumSongs(albumId: String): Flow<List<Song>>

    /** One-shot variant of [albumSongs], ordered by track number. */
    @Transaction
    @Query(
        """
        SELECT song.* FROM song
        INNER JOIN song_album_map ON song_album_map.songId = song.id
        WHERE song_album_map.albumId = :albumId
        ORDER BY song_album_map.sortIndex ASC
        """,
    )
    abstract suspend fun songsOfAlbum(albumId: String): List<Song>

    /** Reactively observes an artist's songs, ordered by credit position. */
    @Transaction
    @Query(
        """
        SELECT song.* FROM song
        INNER JOIN song_artist_map ON song_artist_map.songId = song.id
        WHERE song_artist_map.artistId = :artistId
        ORDER BY song_artist_map.sortIndex ASC
        """,
    )
    abstract fun artistSongs(artistId: String): Flow<List<Song>>

    /** One-shot variant of [artistSongs], ordered by credit position. */
    @Transaction
    @Query(
        """
        SELECT song.* FROM song
        INNER JOIN song_artist_map ON song_artist_map.songId = song.id
        WHERE song_artist_map.artistId = :artistId
        ORDER BY song_artist_map.sortIndex ASC
        """,
    )
    abstract suspend fun songsOfArtist(artistId: String): List<Song>

    /** Reactively observes an album with all of its songs resolved in one query. */
    @Transaction
    @Query("SELECT * FROM album WHERE id = :albumId LIMIT 1")
    abstract fun albumWithSongs(albumId: String): Flow<AlbumWithSongs?>

    /* ----------------------------------------------------------------------------------------
     * Playlists (local playlists + cached remote playlists)
     * ------------------------------------------------------------------------------------- */

    /** Reactively observes every playlist, ordered by name. */
    @Transaction
    @Query("SELECT * FROM playlist ORDER BY name ASC")
    abstract fun playlists(): Flow<List<Playlist>>

    /** Reactively observes a single playlist with its cached songs, `null` when unknown. */
    @Transaction
    @Query("SELECT * FROM playlist WHERE id = :id LIMIT 1")
    abstract fun playlistById(id: String): Flow<Playlist?>

    /** One-shot lookup of the raw playlist row, `null` when unknown. */
    @Query("SELECT * FROM playlist WHERE id = :id LIMIT 1")
    abstract suspend fun playlistEntityById(id: String): PlaylistEntity?

    /** Reactively observes a playlist's songs in playlist order. */
    @Transaction
    @Query(
        """
        SELECT song.* FROM song
        INNER JOIN playlist_song_map ON playlist_song_map.songId = song.id
        WHERE playlist_song_map.playlistId = :playlistId
        ORDER BY playlist_song_map.id ASC
        """,
    )
    abstract fun playlistSongs(playlistId: String): Flow<List<Song>>

    /** One-shot variant of [playlistSongs], in playlist order. */
    @Transaction
    @Query(
        """
        SELECT song.* FROM song
        INNER JOIN playlist_song_map ON playlist_song_map.songId = song.id
        WHERE playlist_song_map.playlistId = :playlistId
        ORDER BY playlist_song_map.id ASC
        """,
    )
    abstract suspend fun playlistSongsList(playlistId: String): List<Song>

    /** Reactively observes a playlist with all of its songs resolved in one query. */
    @Transaction
    @Query("SELECT * FROM playlist WHERE id = :playlistId LIMIT 1")
    abstract fun playlistWithSongs(playlistId: String): Flow<PlaylistWithSongs?>

    /** Inserts (or replaces) a single playlist row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(playlist: PlaylistEntity)

    /** Deletes the playlist → song join rows of a playlist; called from [deletePlaylist]. */
    @Query("DELETE FROM playlist_song_map WHERE playlistId = :playlistId")
    protected abstract suspend fun deletePlaylistSongMaps(playlistId: String)

    /** Deletes the playlist row; called from [deletePlaylist]. */
    @Query("DELETE FROM playlist WHERE id = :playlistId")
    protected abstract suspend fun deletePlaylistRow(playlistId: String)

    /** Deletes a playlist together with its song memberships, in one transaction. */
    @Transaction
    open suspend fun deletePlaylist(playlistId: String) {
        deletePlaylistSongMaps(playlistId)
        deletePlaylistRow(playlistId)
    }

    /** Renames a playlist. */
    @Query("UPDATE playlist SET name = :name WHERE id = :playlistId")
    abstract suspend fun renamePlaylist(playlistId: String, name: String)

    /** Updates every column of an existing playlist row. */
    @Update
    abstract suspend fun updatePlaylist(playlist: PlaylistEntity)

    /** Inserts (or replaces) a playlist → song join row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(map: PlaylistSongMap)

    /** Inserts (or replaces) playlist → song join rows. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertPlaylistSongMaps(maps: List<PlaylistSongMap>)

    /** Inserts (or replaces) playlist → song join rows. */
    @JvmName("insertPlaylistSongMapList")
    suspend fun insert(maps: List<PlaylistSongMap>) = insertPlaylistSongMaps(maps)

    /** Removes one song from a playlist. */
    @Query("DELETE FROM playlist_song_map WHERE playlistId = :playlistId AND songId = :songId")
    abstract suspend fun deletePlaylistSong(playlistId: String, songId: String)

    /** Removes every song from a playlist. */
    @Query("DELETE FROM playlist_song_map WHERE playlistId = :playlistId")
    abstract suspend fun clearPlaylist(playlistId: String)

    /** Returns the join row of one playlist entry, `null` when the song is not in the playlist. */
    @Query(
        """
        SELECT * FROM playlist_song_map
        WHERE playlistId = :playlistId AND songId = :songId
        LIMIT 1
        """,
    )
    abstract suspend fun playlistSongMap(playlistId: String, songId: String): PlaylistSongMap?

    /** Returns the next free play order ([PlaylistSongMap.id]) for a playlist; 0 when empty. */
    @Query("SELECT COALESCE(MAX(id) + 1, 0) FROM playlist_song_map WHERE playlistId = :playlistId")
    abstract suspend fun nextPlaylistSongOrder(playlistId: String): Int

    /* ----------------------------------------------------------------------------------------
     * Search history
     * ------------------------------------------------------------------------------------- */

    /** Reactively observes the search history, most recent first. */
    @Query("SELECT * FROM search_history ORDER BY createdAt DESC, id DESC")
    abstract fun searchHistory(): Flow<List<SearchHistory>>

    /** Adds a search history entry; re-pushing an existing query refreshes it. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(searchHistory: SearchHistory)

    /** Deletes every history entry equal to [query]. */
    @Query("DELETE FROM search_history WHERE query = :query")
    abstract suspend fun deleteSearchHistory(query: String)

    /** Empties the search history. */
    @Query("DELETE FROM search_history")
    abstract suspend fun clearSearchHistory()

    /* ----------------------------------------------------------------------------------------
     * Listening events / statistics
     * ------------------------------------------------------------------------------------- */

    /** Adds a listening event. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(event: Event)

    /** Reactively observes the listening history (joined with songs), newest first. */
    @Transaction
    @Query(
        """
        SELECT event.* FROM event
        INNER JOIN song ON song.id = event.songId
        ORDER BY event.timestamp DESC
        """,
    )
    abstract fun events(): Flow<List<EventWithSong>>

    /** Reactively observes the summed play time of every song, milliseconds; `null` when empty. */
    @Query("SELECT SUM(totalPlayTimeMs) FROM song")
    abstract fun totalPlayTime(): Flow<Long?>

    /** Reactively observes every song joined with its play count, ordered by title. */
    @Transaction
    @Query(
        """
        SELECT song.*,
               IFNULL(play_count.playCount, 0) AS playCount,
               song.totalPlayTimeMs AS statsTotalPlayTimeMs
        FROM song
        LEFT JOIN play_count ON play_count.songId = song.id
        ORDER BY song.title ASC
        """,
    )
    abstract fun songsWithStats(): Flow<List<SongWithStats>>

    /* ----------------------------------------------------------------------------------------
     * Formats (resolved stream metadata)
     * ------------------------------------------------------------------------------------- */

    /** Inserts (or replaces) the cached stream format of a song. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(format: FormatEntity)

    /** Returns the cached stream format of a song, `null` when never resolved. */
    @Query("SELECT * FROM format WHERE songId = :songId LIMIT 1")
    abstract suspend fun formatOf(songId: String): FormatEntity?

    /* ----------------------------------------------------------------------------------------
     * Queue persistence
     * ------------------------------------------------------------------------------------- */

    /** Replaces the persisted queue with [items] (cleared first), in one transaction. */
    @Transaction
    open suspend fun saveQueue(items: List<QueuedMediaItem>) {
        clearQueue()
        insertQueueItems(items)
    }

    /** Inserts (or replaces) persisted queue rows; called from [saveQueue]. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertQueueItems(items: List<QueuedMediaItem>)

    /** Returns the persisted queue in playback order. */
    @Query("SELECT * FROM queued_media_item ORDER BY id ASC")
    abstract suspend fun getQueue(): List<QueuedMediaItem>

    /** Empties the persisted queue. */
    @Query("DELETE FROM queued_media_item")
    abstract suspend fun clearQueue()

    /* ----------------------------------------------------------------------------------------
     * Lyrics
     * ------------------------------------------------------------------------------------- */

    /** Inserts (or replaces) the cached lyrics of a song. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(lyrics: LyricsEntity)

    /** Returns the cached lyrics of a song, `null` when never fetched. */
    @Query("SELECT * FROM lyrics WHERE songId = :songId LIMIT 1")
    abstract suspend fun lyricsOf(songId: String): LyricsEntity?

    /* ----------------------------------------------------------------------------------------
     * Play counts
     * ------------------------------------------------------------------------------------- */

    /** Adds one to the play-count row of a song; returns 0 when no row existed yet. */
    @Query("UPDATE play_count SET playCount = playCount + 1 WHERE songId = :songId")
    protected abstract suspend fun bumpPlayCount(songId: String): Int

    /** Inserts (or replaces) a play count row; called from [incrementPlayCount]. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertPlayCount(row: PlayCountEntity)

    /** Adds one to the lifetime play count of a song, creating the row when needed. */
    @Transaction
    open suspend fun incrementPlayCount(songId: String) {
        if (bumpPlayCount(songId) == 0) {
            insertPlayCount(PlayCountEntity(songId = songId, playCount = 1))
        }
    }

    /**
     * Reactively observes the lifetime play count of a song; `0` while the song has never
     * been played.
     */
    @Query("SELECT COALESCE((SELECT playCount FROM play_count WHERE songId = :songId), 0)")
    abstract fun playCountOf(songId: String): Flow<Int>
}
