package com.abhiram.music.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One persisted entry of the playback queue.
 *
 * [id] is the 0..n position of the item in the queue and its primary key, so restoring the queue
 * is a simple `ORDER BY id`.
 */
@Entity(tableName = "queued_media_item")
data class QueuedMediaItem(
    @PrimaryKey
    val id: Int,
    /** The [SongEntity.id] (video id) of the queued item. */
    val mediaId: String,
    /** Playback position to resume from, milliseconds; `null` starts from the beginning. */
    val positionMs: Long? = null,
)
