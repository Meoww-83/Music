package com.abhiram.music.database.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * A [PlaylistEntity] with all of its songs resolved in one query.
 *
 * @property playlist the raw playlist row.
 * @property songs the playlist's songs (unordered — use the DAO's `playlistSongs(...)` for the
 * playlist order).
 */
data class PlaylistWithSongs(
    @Embedded
    val playlist: PlaylistEntity,
    @Relation(
        entity = SongEntity::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongMap::class,
            parentColumn = "playlistId",
            entityColumn = "songId",
        ),
    )
    val songs: List<Song>,
)
