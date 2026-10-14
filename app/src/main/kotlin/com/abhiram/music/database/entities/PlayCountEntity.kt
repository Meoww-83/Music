package com.abhiram.music.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lifetime play count of a song, kept as a separate row so listing screens can join or sort
 * on it without denormalizing into [SongEntity].
 */
@Entity(tableName = "play_count")
data class PlayCountEntity(
    @PrimaryKey
    val songId: String,
    val playCount: Int,
)
