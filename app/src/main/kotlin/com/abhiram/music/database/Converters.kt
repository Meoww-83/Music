package com.abhiram.music.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Room type converters for the database package.
 *
 * Every [LocalDateTime] is persisted as a UTC epoch **millisecond** value; the conversion is
 * symmetric so stored timestamps survive round trips unchanged.
 */
class Converters {
    /** Converts a stored UTC epoch-millisecond value back into a [LocalDateTime]. */
    @TypeConverter
    fun fromEpochMillis(value: Long?): LocalDateTime? =
        value?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) }

    /** Converts a [LocalDateTime] into a UTC epoch-millisecond value for storage. */
    @TypeConverter
    fun toEpochMillis(value: LocalDateTime?): Long? =
        value?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
}
