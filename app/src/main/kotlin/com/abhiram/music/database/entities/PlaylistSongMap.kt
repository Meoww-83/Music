package com.abhiram.music.database.entities

import androidx.room.Entity
import androidx.room.Index
import java.time.LocalDateTime

/**
 * Join table between [PlaylistEntity] and [SongEntity].
 *
 * [id] is the play order of the song inside the playlist (not the row identity — that is the
 * `(playlistId, songId)` pair). Songs are listed by ascending [id].
 */
@Entity(
    tableName = "playlist_song_map",
    primaryKeys = ["playlistId", "songId"],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["songId"]),
    ],
)
data class PlaylistSongMap(
    val playlistId: String,
    val songId: String,
    /** Play order of the song inside the playlist, starting at 0. */
    val id: Int,
    val setVideoId: String? = null,
    val addedAt: LocalDateTime = LocalDateTime.now(),
)
