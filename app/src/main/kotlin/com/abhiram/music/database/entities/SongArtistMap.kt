package com.abhiram.music.database.entities

import androidx.room.Entity
import androidx.room.Index

/**
 * Join table between [SongEntity] and [ArtistEntity].
 *
 * [sortIndex] preserves the artist order announced by the catalogue (lead artist first).
 */
@Entity(
    tableName = "song_artist_map",
    primaryKeys = ["songId", "artistId"],
    indices = [
        Index(value = ["songId"]),
        Index(value = ["artistId"]),
    ],
)
data class SongArtistMap(
    val songId: String,
    val artistId: String,
    /** Position of the artist for this song, starting at 0. */
    val sortIndex: Int,
)
