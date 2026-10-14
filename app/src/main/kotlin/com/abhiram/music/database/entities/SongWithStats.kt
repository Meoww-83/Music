package com.abhiram.music.database.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * A [SongEntity] with its aggregated statistics, for "most played" style screens.
 *
 * @property song the raw song row.
 * @property playCount lifetime play count from the `play_count` table (`0` when never played).
 * @property totalPlayTimeMs mirrored copy of `song.totalPlayTimeMs`; the column alias differs
 * from the embedded one so Room can map both without a name collision.
 */
data class SongWithStats(
    @Embedded
    val song: SongEntity,
    val playCount: Int,
    @ColumnInfo(name = "statsTotalPlayTimeMs")
    val totalPlayTimeMs: Long,
)
