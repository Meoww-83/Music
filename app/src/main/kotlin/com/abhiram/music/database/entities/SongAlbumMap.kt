package com.abhiram.music.database.entities

import androidx.room.Entity
import androidx.room.Index

/**
 * Join table between [SongEntity] and [AlbumEntity].
 *
 * [sortIndex] is the track number of the song inside the album.
 */
@Entity(
    tableName = "song_album_map",
    primaryKeys = ["songId", "albumId"],
    indices = [
        Index(value = ["songId"]),
        Index(value = ["albumId"]),
    ],
)
data class SongAlbumMap(
    val songId: String,
    val albumId: String,
    /** Track position inside the album, starting at 0. */
    val sortIndex: Int,
)
