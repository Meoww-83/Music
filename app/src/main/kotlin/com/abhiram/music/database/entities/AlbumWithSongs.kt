package com.abhiram.music.database.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * An [AlbumEntity] with all of its songs resolved in one query.
 *
 * @property album the raw album row.
 * @property songs the album's tracks (unordered — use the DAO's `albumSongs(...)` for the
 * track-number ordering).
 */
data class AlbumWithSongs(
    @Embedded
    val album: AlbumEntity,
    @Relation(
        entity = SongEntity::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SongAlbumMap::class,
            parentColumn = "albumId",
            entityColumn = "songId",
        ),
    )
    val songs: List<Song>,
)
