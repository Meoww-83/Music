package com.abhiram.music.database.entities

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A listening [Event] with the [Song] it belongs to, for the history screen.
 *
 * The DAO's history query joins the song table, so [song] is always present.
 */
data class EventWithSong(
    @Embedded
    val event: Event,
    @Relation(
        entity = SongEntity::class,
        parentColumn = "songId",
        entityColumn = "id",
    )
    val song: Song,
)
