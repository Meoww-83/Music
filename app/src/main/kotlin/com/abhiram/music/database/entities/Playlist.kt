package com.abhiram.music.database.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * A [PlaylistEntity] together with its cached songs.
 *
 * @property playlist the raw playlist row.
 * @property songs the playlist's songs (unordered — the DAO's `playlistSongs(...)` /
 * `playlistSongsList(...)` queries return them ordered by [PlaylistSongMap.id]).
 */
data class Playlist(
    @Embedded
    val playlist: PlaylistEntity,
    @Relation(
        entity = SongEntity::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongMap::class,
            parentColumn = "playlistId",
            entityColumn = "songId",
        ),
    )
    val songs: List<Song> = emptyList(),
) {
    /** [PlaylistEntity.id]. */
    val id: String get() = playlist.id

    /** [PlaylistEntity.name]. */
    val name: String get() = playlist.name

    /** [PlaylistEntity.thumbnailUrl]. */
    val thumbnailUrl: String? get() = playlist.thumbnailUrl

    /** [PlaylistEntity.description]. */
    val description: String? get() = playlist.description

    /** [PlaylistEntity.browseId]; `null` for purely local playlists. */
    val browseId: String? get() = playlist.browseId
}
