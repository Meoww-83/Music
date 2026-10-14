package com.abhiram.music.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An album row. Songs are attached through [SongAlbumMap], artists through [AlbumArtistMap].
 */
@Entity(tableName = "album")
data class AlbumEntity(
    /** Album browse id from InnerTube. */
    @PrimaryKey
    val id: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val year: Int? = null,
    /** Human readable track count (e.g. `"12 songs"`), when known. */
    val songCountText: String? = null,
)
