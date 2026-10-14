package com.abhiram.music.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An artist row. Songs are attached through [SongArtistMap], albums through [AlbumArtistMap].
 */
@Entity(tableName = "artist")
data class ArtistEntity(
    /** Channel/artist id from InnerTube. */
    @PrimaryKey
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    /** Human readable subscriber count (e.g. `"1.2M subscribers"`), when known. */
    val subscriberCountText: String? = null,
)
