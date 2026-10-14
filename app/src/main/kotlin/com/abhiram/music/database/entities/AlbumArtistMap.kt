package com.abhiram.music.database.entities

import androidx.room.Entity
import androidx.room.Index

/**
 * Join table between [AlbumEntity] and [ArtistEntity].
 *
 * [sortIndex] preserves the credit order announced by the catalogue.
 */
@Entity(
    tableName = "album_artist_map",
    primaryKeys = ["albumId", "artistId"],
    indices = [
        Index(value = ["albumId"]),
        Index(value = ["artistId"]),
    ],
)
data class AlbumArtistMap(
    val albumId: String,
    val artistId: String,
    /** Position of the artist for this album, starting at 0. */
    val sortIndex: Int,
)
