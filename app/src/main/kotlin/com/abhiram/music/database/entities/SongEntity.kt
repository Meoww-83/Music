package com.abhiram.music.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * A playable track as cached in the local database.
 *
 * This is the raw table row; UI and repository code should prefer the [Song] relation wrapper,
 * which resolves artists and the album through the relation maps.
 */
@Entity(
    tableName = "song",
    indices = [
        Index(value = ["albumId"]),
    ],
)
data class SongEntity(
    /** YouTube video id — the stable identity of the track. */
    @PrimaryKey
    val id: String,
    val title: String,
    /** Track length in seconds. */
    val duration: Int,
    val thumbnailUrl: String? = null,
    /** Denormalized album id (see [SongAlbumMap] for the ordered relation). */
    val albumId: String? = null,
    /** Denormalized album title, kept for tracks whose album row was never cached. */
    val albumName: String? = null,
    val explicit: Boolean = false,
    /** `null` = unknown, `false` = known not liked, `true` = liked. */
    val liked: Boolean? = null,
    val likedDate: LocalDateTime? = null,
    val inLibrary: LocalDateTime? = null,
    val libraryAddToken: String? = null,
    val libraryRemoveToken: String? = null,
    val isVideo: Boolean = false,
    val isEpisode: Boolean = false,
    val uploadEntityId: String? = null,
    /** Accumulated playback time, milliseconds. */
    val totalPlayTimeMs: Long = 0,
)
