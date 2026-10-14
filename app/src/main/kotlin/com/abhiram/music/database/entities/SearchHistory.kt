package com.abhiram.music.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * A single entry of the local search history.
 *
 * The [query] column is unique, so re-pushing the same query (with
 * `OnConflictStrategy.REPLACE`) refreshes its [createdAt] instead of duplicating it.
 */
@Entity(
    tableName = "search_history",
    indices = [
        Index(value = ["query"], unique = true),
    ],
)
data class SearchHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val query: String,
    val createdAt: LocalDateTime,
)
