package com.abhiram.music.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.abhiram.music.database.entities.AlbumArtistMap
import com.abhiram.music.database.entities.AlbumEntity
import com.abhiram.music.database.entities.ArtistEntity
import com.abhiram.music.database.entities.Event
import com.abhiram.music.database.entities.FormatEntity
import com.abhiram.music.database.entities.LyricsEntity
import com.abhiram.music.database.entities.PlayCountEntity
import com.abhiram.music.database.entities.PlaylistEntity
import com.abhiram.music.database.entities.PlaylistSongMap
import com.abhiram.music.database.entities.QueuedMediaItem
import com.abhiram.music.database.entities.SearchHistory
import com.abhiram.music.database.entities.SongAlbumMap
import com.abhiram.music.database.entities.SongArtistMap
import com.abhiram.music.database.entities.SongEntity

/**
 * The application's Room database: songs, library, playlists, history and playback caches.
 *
 * Schema version 1 — the initial schema; no migrations exist (or are needed) yet.
 * Obtain an instance through [build]; the DI layer (service locator) keeps the singleton.
 */
@Database(
    entities = [
        SongEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        SongArtistMap::class,
        SongAlbumMap::class,
        AlbumArtistMap::class,
        PlaylistEntity::class,
        PlaylistSongMap::class,
        SearchHistory::class,
        Event::class,
        FormatEntity::class,
        QueuedMediaItem::class,
        LyricsEntity::class,
        PlayCountEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MusicDatabase : RoomDatabase() {
    /** The single DAO exposing every query of the app. */
    abstract fun databaseDao(): DatabaseDao

    /** Convenience alias of [databaseDao], mirroring the style used across the app. */
    val dao: DatabaseDao get() = databaseDao()

    companion object {
        /** File name of the database inside the app's database directory. */
        const val DB_NAME: String = "music.db"

        /**
         * Builds the database instance. No destructive migration fallback is configured:
         * version upgrades must ship explicit migrations.
         */
        fun build(context: Context): MusicDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MusicDatabase::class.java,
                DB_NAME,
            ).build()
    }
}
