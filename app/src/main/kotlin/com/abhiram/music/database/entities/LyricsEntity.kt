package com.abhiram.music.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached lyrics of a song.
 *
 * `plainLyrics` and `syncedLyrics` follow the InnerTube lyrics response shape; [source] keeps
 * the attribution of the provider.
 */
@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey
    val songId: String,
    val plainLyrics: String? = null,
    /** LRC-formatted, time-synced lyrics. */
    val syncedLyrics: String? = null,
    val source: String? = null,
)
