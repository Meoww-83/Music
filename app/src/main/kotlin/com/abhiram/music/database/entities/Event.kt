package com.abhiram.music.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A listening event: one playback session of [songId] that lasted long enough to be counted.
 *
 * Raw [timestamp] is epoch milliseconds; [playDuration] is the listened amount in milliseconds.
 */
@Entity(
    tableName = "event",
    indices = [
        Index(value = ["songId"]),
    ],
)
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val songId: String,
    /** Epoch milliseconds when the playback happened. */
    val timestamp: Long,
    /** How long the song was played in this session, milliseconds. */
    val playDuration: Long,
)
