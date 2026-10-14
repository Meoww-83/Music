package com.abhiram.music.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * A playlist row — either a local playlist or a cached copy of a remote one.
 *
 * `browseId == null` marks a purely local playlist; otherwise it holds the InnerTube browse id.
 */
@Entity(tableName = "playlist")
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    /** InnerTube browse id; `null` means the playlist exists locally only. */
    val browseId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastUpdatedAt: LocalDateTime = LocalDateTime.now(),
)
