package com.abhiram.music.database.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * An [ArtistEntity] together with its cached songs.
 *
 * @property artist the raw artist row.
 * @property songs every song credited to this artist (unordered — the DAO's
 * `artistSongs(...)` / `songsOfArtist(...)` queries return them sorted by [SongArtistMap.sortIndex]).
 */
data class Artist(
    @Embedded
    val artist: ArtistEntity,
    @Relation(
        entity = SongEntity::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SongArtistMap::class,
            parentColumn = "artistId",
            entityColumn = "songId",
        ),
    )
    val songs: List<Song> = emptyList(),
) {
    /** [ArtistEntity.id]. */
    val id: String get() = artist.id

    /** [ArtistEntity.name]. */
    val name: String get() = artist.name

    /** [ArtistEntity.thumbnailUrl]. */
    val thumbnailUrl: String? get() = artist.thumbnailUrl

    /** [ArtistEntity.subscriberCountText]. */
    val subscriberCountText: String? get() = artist.subscriberCountText
}
