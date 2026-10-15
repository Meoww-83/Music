@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.abhiram.music.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.abhiram.innertube.models.ArtistItem
import com.abhiram.innertube.models.PlaylistItem
import com.abhiram.innertube.models.YTItem
import com.abhiram.innertube.pages.AlbumPage
import com.abhiram.innertube.pages.ArtistPage
import com.abhiram.music.database.DatabaseDao
import com.abhiram.music.database.entities.Album
import com.abhiram.music.database.entities.AlbumEntity
import com.abhiram.music.database.entities.Artist
import com.abhiram.music.database.entities.Playlist
import com.abhiram.music.database.entities.SongWithStats
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.model.BuiltInPlaylist
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.model.toMediaMetadata
import com.abhiram.music.repository.MusicRepository
import com.abhiram.music.repository.YouTubeRepository
import com.abhiram.music.repository.artists
import com.abhiram.music.repository.songs
import com.abhiram.music.repository.toMediaMetadataList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * View models of the five detail screens (owner SCR-D). Everything is resolved through
 * [ServiceLocator] — no DI framework — and every network call benefits from the
 * [YouTubeRepository] first-page TTL cache, so tab switches and recompositions stay off the
 * network. Local (Room-backed) screens are plain flow projections and never cache themselves.
 */

/** Fallback failure text for exceptions without a message. */
private const val DEFAULT_ERROR = "Something went wrong"

/** Safety cap when following a playlist's song continuations to completion. */
private const val MAX_PLAYLIST_CONTINUATIONS = 20

/* =============================================================================================
 *  Artist — route `artist/{id}`
 * ========================================================================================== */

/**
 * UI state of the artist detail screen.
 *
 * The id arriving on the route is a YouTube channel/browse id in both directions (library rows
 * persist InnerTube ids), so the screen is online-first and falls back to the Room cache when
 * the page cannot be fetched — [local] and [localSongs] are collected independently and stay
 * usable offline.
 *
 * @property online parsed InnerTube artist page, `null` while loading or when offline.
 * @property local Room [Artist] wrapper for the same id, when the artist was played/liked before.
 * @property localSongs the artist's songs cached on this device.
 * @property error failure message of the last online fetch; CMP-D renders it as a full retry
 * state when nothing else is loaded and as a compact footer under already-rendered content.
 */
data class ArtistDetailUiState(
    val isLoading: Boolean = true,
    val online: ArtistPage? = null,
    val local: Artist? = null,
    val localSongs: List<MediaMetadata> = emptyList(),
    val error: String? = null,
) {
    /** True once anything renderable (online page or local row) has arrived. */
    val hasContent: Boolean get() = online != null || local != null

    /** Songs of the "Songs" tab: the page's songs section, else every page song, else local. */
    val songs: List<MediaMetadata>
        get() =
            online
                ?.let { page ->
                    page
                        .sectionItems("Songs")
                        .ifEmpty { page.allItems() }
                        .toMediaMetadataList()
                        .ifEmpty { localSongs }
                } ?: localSongs

    /** "Top songs" carousel of the overview tab, capped to a single row of cards. */
    val topSongs: List<MediaMetadata>
        get() = online?.sectionItems("Top songs").orEmpty().toMediaMetadataList().take(10)

    /** "Fans might also like" carousel of the overview tab. */
    val relatedArtists: List<ArtistItem>
        get() = online?.sectionItems("Fans might also like").orEmpty().filterIsInstance<ArtistItem>()
}

/** Items of the first [ArtistPage] section whose title starts with [prefix], if any. */
private fun ArtistPage.sectionItems(prefix: String): List<YTItem> =
    sections.firstOrNull { it.title.startsWith(prefix, ignoreCase = true) }?.items.orEmpty()

/** Every item across all sections of the page, songs included. */
private fun ArtistPage.allItems(): List<YTItem> = sections.flatMap { it.items }

/** Loads and refreshes one artist page plus its Room cache for `artist/{id}`. */
class ArtistDetailViewModel(private val id: String) : ViewModel() {

    private val youTubeRepository: YouTubeRepository = ServiceLocator.container.youTubeRepository
    private val dao: DatabaseDao = ServiceLocator.container.dao

    private val _state = MutableStateFlow(ArtistDetailUiState())
    val state: StateFlow<ArtistDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dao.artistById(id),
                dao.artistSongs(id),
            ) { artist, songs -> artist to songs.map { it.toMediaMetadata() } }
                .collect { (artist, songs) ->
                    _state.value = _state.value.copy(local = artist, localSongs = songs)
                }
        }
        refresh()
    }

    /** Re-fetches the online page; Room data keeps rendering underneath. */
    fun refresh() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            youTubeRepository
                .artist(id)
                .onSuccess { page ->
                    _state.value = _state.value.copy(isLoading = false, online = page, error = null)
                }
                .onFailure { throwable ->
                    // CMP-D renders this as a full retry state when nothing is loaded yet and as
                    // a compact footer under already-rendered content.
                    _state.value =
                        _state.value.copy(isLoading = false, error = throwable.message ?: DEFAULT_ERROR)
                }
        }
    }

    companion object {
        /** Factory for a single artist id — the view model resolves everything through [ServiceLocator]. */
        fun factory(id: String): ViewModelProvider.Factory =
            viewModelFactory { initializer { ArtistDetailViewModel(id) } }
    }
}

/* =============================================================================================
 *  Album — route `album/{id}`
 * ========================================================================================== */

/**
 * UI state of the album detail screen.
 *
 * The route id is the InnerTube album browse id (also stored as the local `AlbumEntity.id`), so
 * the page is fetched online first and falls back to the Room cache when unavailable.
 *
 * @property songs tracks in track order — the online page's songs, else the local cache.
 */
data class AlbumDetailUiState(
    val isLoading: Boolean = true,
    val online: AlbumPage? = null,
    val local: Album? = null,
    val localSongs: List<MediaMetadata> = emptyList(),
    val error: String? = null,
) {
    /** True once anything renderable (online page or local row) has arrived. */
    val hasContent: Boolean get() = online != null || local != null

    /** Album tracks in track order, whichever source provided them. */
    val songs: List<MediaMetadata>
        get() = online?.toMediaMetadataList() ?: localSongs
}

/** Loads and refreshes one album page plus its Room cache for `album/{id}`. */
class AlbumDetailViewModel(private val id: String) : ViewModel() {

    private val youTubeRepository: YouTubeRepository = ServiceLocator.container.youTubeRepository
    private val musicRepository: MusicRepository = ServiceLocator.container.musicRepository
    private val dao: DatabaseDao = ServiceLocator.container.dao

    private val _state = MutableStateFlow(AlbumDetailUiState())
    val state: StateFlow<AlbumDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dao.albumById(id),
                dao.albumSongs(id),
            ) { album, songs -> album to songs.map { it.toMediaMetadata() } }
                .collect { (album, songs) ->
                    _state.value = _state.value.copy(local = album, localSongs = songs)
                }
        }
        refresh()
    }

    /** Re-fetches the online page; Room data keeps rendering underneath. */
    fun refresh() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            youTubeRepository
                .album(id)
                .onSuccess { page ->
                    _state.value = _state.value.copy(isLoading = false, online = page, error = null)
                }
                .onFailure { throwable ->
                    // CMP-D renders this as a full retry state when nothing is loaded yet and as
                    // a compact footer under already-rendered content.
                    _state.value =
                        _state.value.copy(isLoading = false, error = throwable.message ?: DEFAULT_ERROR)
                }
        }
    }

    /** Persists every album track into the local library (song rows + library membership). */
    fun saveToLibrary(songs: List<MediaMetadata>) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                songs.forEach { musicRepository.addToLibrary(it) }
            }
        }
    }

    companion object {
        /** Factory for a single album id — the view model resolves everything through [ServiceLocator]. */
        fun factory(id: String): ViewModelProvider.Factory =
            viewModelFactory { initializer { AlbumDetailViewModel(id) } }
    }
}

/* =============================================================================================
 *  Online playlist — route `playlist/{id}`
 * ========================================================================================== */

/**
 * UI state of the online playlist detail screen.
 *
 * @property songs the playlist's tracks in playlist order; every song continuation page is
 * followed (capped at [MAX_PLAYLIST_CONTINUATIONS]) before the list is published, so the screen
 * renders the complete playlist exactly once.
 */
data class PlaylistDetailUiState(
    val isLoading: Boolean = true,
    val playlist: PlaylistItem? = null,
    val songs: List<MediaMetadata> = emptyList(),
    val error: String? = null,
)

/** Loads one online playlist (with all of its song continuations) for `playlist/{id}`. */
class PlaylistDetailViewModel(private val id: String) : ViewModel() {

    private val youTubeRepository: YouTubeRepository = ServiceLocator.container.youTubeRepository

    private val _state = MutableStateFlow(PlaylistDetailUiState())
    val state: StateFlow<PlaylistDetailUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Fetches the playlist page and follows its song continuations to completion. */
    fun refresh() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            youTubeRepository
                .playlist(id)
                .onFailure { throwable ->
                    _state.value =
                        _state.value.copy(isLoading = false, error = throwable.message ?: DEFAULT_ERROR)
                }
                .onSuccess { page ->
                    val songs = ArrayList(page.songs.toMediaMetadataList())
                    var continuation = page.songsContinuation ?: page.continuation
                    var pages = 0
                    while (continuation != null && pages < MAX_PLAYLIST_CONTINUATIONS) {
                        val next =
                            youTubeRepository
                                .playlistContinuation(continuation)
                                .getOrNull() ?: break
                        songs += next.songs.toMediaMetadataList()
                        continuation = next.continuation
                        pages++
                    }
                    _state.value =
                        _state.value.copy(isLoading = false, playlist = page.playlist, songs = songs)
                }
        }
    }

    companion object {
        /** Factory for a single playlist id — the view model resolves everything through [ServiceLocator]. */
        fun factory(id: String): ViewModelProvider.Factory =
            viewModelFactory { initializer { PlaylistDetailViewModel(id) } }
    }
}

/* =============================================================================================
 *  Local playlist — route `localPlaylist/{id}`
 * ========================================================================================== */

/**
 * UI state of the local playlist detail screen — a direct projection of two Room flows, so
 * every mutation (remove, reorder, rename) is reflected immediately without a refresh.
 *
 * @property notFound `true` once the flow settled and the playlist row no longer exists.
 */
data class LocalPlaylistDetailUiState(
    val isLoading: Boolean = true,
    val playlist: Playlist? = null,
    val songs: List<MediaMetadata> = emptyList(),
) {
    val notFound: Boolean get() = !isLoading && playlist == null
}

/** Owns one device playlist (`PlaylistEntity.browseId == null`) for `localPlaylist/{id}`. */
class LocalPlaylistDetailViewModel(private val id: String) : ViewModel() {

    private val dao: DatabaseDao = ServiceLocator.container.dao
    private val musicRepository: MusicRepository = ServiceLocator.container.musicRepository

    private val _state = MutableStateFlow(LocalPlaylistDetailUiState())
    val state: StateFlow<LocalPlaylistDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dao.playlistById(id),
                dao.playlistSongs(id),
            ) { playlist, songs -> playlist to songs.map { it.toMediaMetadata() } }
                .collect { (playlist, songs) ->
                    _state.value =
                        LocalPlaylistDetailUiState(isLoading = false, playlist = playlist, songs = songs)
                }
        }
    }

    /** Removes the song at [index] from the playlist (the song row itself stays in the library). */
    fun removeSongAt(index: Int) {
        val song = _state.value.songs.getOrNull(index) ?: return
        viewModelScope.launch { musicRepository.removeSongFromPlaylist(id, song.id) }
    }

    /** Moves the song at [from] to [to], renumbering the affected play orders. */
    fun moveSong(from: Int, to: Int) {
        viewModelScope.launch { reorderSongs(from, to) }
    }

    /** Renames the playlist. */
    fun rename(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { dao.renamePlaylist(id, trimmed) }
    }

    /** Deletes the playlist row and its membership; the screen pops itself afterwards. */
    fun delete() {
        viewModelScope.launch { dao.deletePlaylist(id) }
    }

    /**
     * Renumbers `playlist_song_map.id` after a move. The maps keep their `setVideoId` /
     * `addedAt` values and only the play order changes; upserts are keyed by
     * `(playlistId, songId)` so renumbering cannot cross-contaminate rows.
     */
    private suspend fun reorderSongs(from: Int, to: Int) = withContext(Dispatchers.IO) {
        val songs = _state.value.songs
        if (from == to || from !in songs.indices || to !in songs.indices) return@withContext
        val maps = songs.mapNotNull { song -> dao.playlistSongMap(id, song.id) }
        if (maps.size != songs.size) return@withContext
        val reordered = ArrayList(maps)
        reordered.add(to, reordered.removeAt(from))
        val base = reordered.minOf { it.id }
        reordered.forEachIndexed { index, map -> dao.insert(map.copy(id = base + index)) }
    }

    companion object {
        /** Factory for a single playlist id — the view model resolves everything through [ServiceLocator]. */
        fun factory(id: String): ViewModelProvider.Factory =
            viewModelFactory { initializer { LocalPlaylistDetailViewModel(id) } }
    }
}

/* =============================================================================================
 *  Built-in playlists — route `builtInPlaylist/{id}`
 * ========================================================================================== */

/**
 * UI state of the built-in playlist detail screen.
 *
 * @property songs tracks of the selected list; `null` sources (offline/downloaded/cache) stay
 * honestly empty until a download subsystem exists.
 */
data class BuiltInPlaylistDetailUiState(
    val isLoading: Boolean = true,
    val songs: List<MediaMetadata> = emptyList(),
)

/** Serves one of the system playlists (favorites / offline / downloaded / history / top / cache). */
class BuiltInPlaylistDetailViewModel(id: String) : ViewModel() {

    private val dao: DatabaseDao = ServiceLocator.container.dao

    private val _state = MutableStateFlow(BuiltInPlaylistDetailUiState())
    val state: StateFlow<BuiltInPlaylistDetailUiState> = _state.asStateFlow()

    init {
        val builtIn = BuiltInPlaylist.fromId(id)
        if (builtIn == null) {
            _state.value = BuiltInPlaylistDetailUiState(isLoading = false)
        } else {
            viewModelScope.launch {
                source(builtIn)
                    .collect { songs ->
                        _state.value = BuiltInPlaylistDetailUiState(isLoading = false, songs = songs)
                    }
            }
        }
    }

    /** Reactive track source backing each built-in list. */
    private fun source(builtIn: BuiltInPlaylist): Flow<List<MediaMetadata>> =
        when (builtIn) {
            BuiltInPlaylist.FAVORITES ->
                dao.likedSongs().map { songs -> songs.map { it.toMediaMetadata() } }

            BuiltInPlaylist.HISTORY ->
                dao.events().map { events -> events.map { it.song.toMediaMetadata() } }

            BuiltInPlaylist.TOP ->
                dao.songsWithStats().flatMapLatest { stats ->
                    val rankedIds = stats
                        .filter { it.playCount > 0 }
                        .sortedWith(
                            compareByDescending<SongWithStats> { it.playCount }
                                .thenByDescending { it.totalPlayTimeMs },
                        )
                        .map { it.song.id }
                    if (rankedIds.isEmpty()) {
                        flowOf(emptyList<MediaMetadata>())
                    } else {
                        flowOf(
                            dao.songsByIds(rankedIds)
                                .sortedBy { rankedIds.indexOf(it.id) }
                                .map { it.toMediaMetadata() },
                        )
                    }
                }

            // No download/offline subsystem exists yet — these lists are honestly empty
            // (the shared screen renders a "Nothing here yet" placeholder).
            BuiltInPlaylist.OFFLINE,
            BuiltInPlaylist.DOWNLOADED,
            BuiltInPlaylist.CACHE,
            -> flowOf(emptyList())
        }

    /**
     * Removes every listening event of [songId] from the history.
     *
     * The frozen DAO (CONTRACTS §3.3) only exposes the song's event delete through its
     * `@Transaction` cascade [DatabaseDao.deleteSong], so this call also drops the song's local
     * metadata row (stream formats, lyrics, play counts, playlist membership). The row is
     * rebuilt automatically the next time the song is played or liked.
     */
    fun removeFromHistory(songId: String) {
        viewModelScope.launch { dao.deleteSong(songId) }
    }

    /** Wipes the whole listening history (overflow action of the history list). */
    fun clearHistory() {
        viewModelScope.launch { dao.clearEvents() }
    }

    companion object {
        /** Factory for a single built-in id — the view model resolves everything through [ServiceLocator]. */
        fun factory(id: String): ViewModelProvider.Factory =
            viewModelFactory { initializer { BuiltInPlaylistDetailViewModel(id) } }
    }
}

/* =============================================================================================
 *  Shared placeholders
 * ========================================================================================== */

/**
 * Placeholder InnerTube artist shown while the real page is still loading (the CMP-D screen
 * always renders its banner, so an empty-titled item keeps the layout stable until data pops in).
 */
internal fun placeholderArtistItem(id: String): ArtistItem =
    ArtistItem(id = id, title = "", thumbnail = null, shuffleEndpoint = null, radioEndpoint = null)

/** Placeholder InnerTube playlist shown while the real page is still loading. */
internal fun placeholderPlaylistItem(id: String): PlaylistItem =
    PlaylistItem(
        id = id,
        title = "",
        author = null,
        songCountText = null,
        thumbnail = null,
        playEndpoint = null,
        shuffleEndpoint = null,
        radioEndpoint = null,
    )

/** Placeholder Room album shown while the real local row is still loading; keeps the CMP-D
 * local overload renderable from the very first frame.
 */
internal fun placeholderLocalAlbum(id: String): Album =
    Album(AlbumEntity(id = id, title = "", thumbnailUrl = null, year = null, songCountText = null))
