package com.abhiram.music.database.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * A [SongEntity] together with its resolved relations.
 *
 * This is what the DAO returns for every song listing; `com.abhiram.music.model` maps it to
 * `MediaMetadata` for playback.
 *
 * @property song the raw song row.
 * @property artistMaps ordered song → artist join rows, kept so [orderedArtists] can reproduce
 * the credit order announced by the catalogue.
 * @property artists every credited artist, in the order Room returns them (not necessarily the
 * credit order — use [orderedArtists] for that).
 * @property album the [AlbumEntity] referenced by `song.albumId`, or `null` when the album row
 * is not cached.
 */
data class Song(
    @Embedded
    val song: SongEntity,
    @Relation(parentColumn = "id", entityColumn = "songId")
    val artistMaps: List<SongArtistMap> = emptyList(),
    @Relation(
        entity = ArtistEntity::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SongArtistMap::class,
            parentColumn = "songId",
            entityColumn = "artistId",
        ),
    )
    val artists: List<ArtistEntity> = emptyList(),
    @Relation(parentColumn = "albumId", entityColumn = "id")
    val album: AlbumEntity? = null,
) {
    /** [SongEntity.id]. */
    val id: String get() = song.id

    /** [SongEntity.title]. */
    val title: String get() = song.title

    /** [SongEntity.duration], seconds. */
    val duration: Int get() = song.duration

    /** [SongEntity.thumbnailUrl]. */
    val thumbnailUrl: String? get() = song.thumbnailUrl

    /**
     * The credited artists sorted by their [SongArtistMap.sortIndex] (lead artist first);
     * falls back to the unordered [artists] when no join rows are available.
     */
    val orderedArtists: List<ArtistEntity>
        get() {
            if (artistMaps.isEmpty()) return artists
            val artistsById = artists.associateBy { it.id }
            val sorted = artistMaps
                .sortedBy { it.sortIndex }
                .mapNotNull { artistsById[it.artistId] }
            return sorted.ifEmpty { artists }
        }
}
