package com.abhiram.music.database.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * An [AlbumEntity] together with its cached songs.
 *
 * @property album the raw album row.
 * @property songs the album's tracks (unordered — the DAO's `albumSongs(...)` /
 * `songsOfAlbum(...)` queries return them sorted by [SongAlbumMap.sortIndex]).
 */
data class Album(
    @Embedded
    val album: AlbumEntity,
    @Relation(
        entity = SongEntity::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SongAlbumMap::class,
            parentColumn = "albumId",
            entityColumn = "songId",
        ),
    )
    val songs: List<Song> = emptyList(),
) {
    /** [AlbumEntity.id]. */
    val id: String get() = album.id

    /** [AlbumEntity.title]. */
    val title: String get() = album.title

    /** [AlbumEntity.thumbnailUrl]. */
    val thumbnailUrl: String? get() = album.thumbnailUrl

    /** [AlbumEntity.year]. */
    val year: Int? get() = album.year

    /** [AlbumEntity.songCountText]. */
    val songCountText: String? get() = album.songCountText
}
