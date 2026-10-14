package com.abhiram.music.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stream metadata of the best format resolved for a song, cached to avoid re-resolving.
 *
 * Written by the playback pipeline whenever `YouTube.player(...)` succeeds for [songId].
 */
@Entity(tableName = "format")
data class FormatEntity(
    @PrimaryKey
    val songId: String,
    val itag: Int? = null,
    val mimeType: String? = null,
    /** Bits per second. */
    val bitrate: Long? = null,
    val loudnessDb: Double? = null,
    val contentLength: Long? = null,
    /** `lastModified` header reported by the stream server, when present. */
    val lastModified: String? = null,
)
