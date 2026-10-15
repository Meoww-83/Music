@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.abhiram.music.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.abhiram.music.database.entities.Album
import com.abhiram.music.database.entities.Artist
import com.abhiram.music.database.entities.Playlist
import com.abhiram.music.database.entities.Song
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.model.toMediaMetadata
import com.abhiram.music.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/* =============================================================================================
 *  Shared vocabulary of the four library tabs (routes `songs`, `artists`, `albums`, `playlists`)
 * ========================================================================================== */

/** Filter chips of the songs tab. */
enum class SongFilter(val label: String) {
    ALL("All"),
    LIKED("Liked"),
    RECENT("Recent"),
    ;
}

/** Sort keys offered by the songs tab. */
enum class SongSortType(val label: String) {
    TITLE("Title"),
    DATE_ADDED("Date added"),
    DURATION("Duration"),
    ARTIST("Artist"),
    ;
}

/** Sort keys offered by the artists tab. */
enum class ArtistSortType(val label: String) {
    TITLE("Title"),
    SONG_COUNT("Songs"),
    MOST_PLAYED("Most played"),
    ;
}

/** Sort keys offered by the albums tab. */
enum class AlbumSortType(val label: String) {
    TITLE("Title"),
    YEAR("Year"),
    SONG_COUNT("Songs"),
    MOST_PLAYED("Most played"),
    ;
}

/** Sort keys offered by the playlists tab. */
enum class PlaylistSortType(val label: String) {
    NAME("Name"),
    CREATED("Date created"),
    UPDATED("Recently updated"),
    ;
}

/** One selectable entry of a filter or sort chip row. */
data class ChipOption<T>(
    val key: T,
    val label: String,
)

/**
 * Reactive list state shared by every library tab.
 *
 * The screens render [isLoading] as a shimmer skeleton, [error] as a retry state and an empty
 * [items] list (with both flags cleared) as the empty state.
 *
 * @property items Currently visible rows, already filtered and sorted.
 * @property isLoading `true` until the backing Room flow emits its first value.
 * @property error Failure message of the backing flow, `null` when healthy.
 */
data class LibraryListState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    /** Convenience: the screen has nothing to draw yet. */
    val isEmpty: Boolean get() = !isLoading && error == null && items.isEmpty()
}

/** How long a backing Room flow stays hot after the last subscriber leaves. */
private const val STOP_TIMEOUT_MS: Long = 5_000L

/** Fallback failure text for exceptions without a message. */
private const val DEFAULT_ERROR = "Something went wrong"

/** Case-insensitive ascending comparator on a string key. */
private fun <T> titleComparator(selector: (T) -> String): Comparator<T> =
    compareBy { selector(it).lowercase() }

/** Wraps a failing flow so its error becomes a [LibraryListState.error] instead of a crash. */
private fun <T> Flow<LibraryListState<T>>.asLibraryState(): Flow<LibraryListState<T>> =
    catch { throwable ->
        emit(LibraryListState(isLoading = false, error = throwable.message ?: DEFAULT_ERROR))
    }

/* =============================================================================================
 *  Songs tab — route `songs`
 * ========================================================================================== */

/** Snapshot of the raw song data the songs tab derives its list from. */
private data class SongData(
    val all: List<Song>,
    val liked: List<Song>,
    /** songId → epoch millis of the most recent counted playback. */
    val lastPlayed: Map<String, Long>,
)

/**
 * View model of the songs tab (route `songs`).
 *
 * Exposes the Room flows `dao.songs()`, `dao.likedSongs()` and `dao.events()` (which powers the
 * "Recent" ordering) as a single [state] of [LibraryListState], plus the [filter] / [sort] /
 * [sortDescending] chip state, a [toggleLike] action and a [retry] hook. Everything resolves
 * through `ServiceLocator` — no DI framework.
 */
class LibrarySongsViewModel : ViewModel() {

    private val dao = ServiceLocator.container.dao
    private val musicRepository: MusicRepository = ServiceLocator.container.musicRepository

    private val _filter = MutableStateFlow(SongFilter.ALL)
    val filter: StateFlow<SongFilter> = _filter.asStateFlow()

    private val _sort = MutableStateFlow(SongSortType.TITLE)
    val sort: StateFlow<SongSortType> = _sort.asStateFlow()

    private val _sortDescending = MutableStateFlow(false)
    val sortDescending: StateFlow<Boolean> = _sortDescending.asStateFlow()

    /** Bumped by [retry]; re-subscribes the Room query after a failure. */
    private val retryTrigger = MutableStateFlow(0L)

    val state: StateFlow<LibraryListState<Song>> =
        retryTrigger
            .flatMapLatest {
                combine(songData(), _filter, _sort, _sortDescending) { data, filter, sort, descending ->
                    data.toState(filter, sort, descending)
                }.asLibraryState()
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryListState())

    fun setFilter(value: SongFilter) {
        _filter.value = value
    }

    fun setSort(value: SongSortType) {
        _sort.value = value
    }

    fun toggleSortDescending() {
        _sortDescending.value = !_sortDescending.value
    }

    /** Restarts the backing query after [LibraryListState.error]. */
    fun retry() {
        retryTrigger.value += 1L
    }

    /** Flips the like flag of [song] locally and mirrors it to the YouTube library. */
    fun toggleLike(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { musicRepository.toggleLike(song.toMediaMetadata()) }
        }
    }

    private fun songData(): Flow<SongData> =
        combine(dao.songs(), dao.likedSongs(), dao.events()) { all, liked, events ->
            val lastPlayed = HashMap<String, Long>(events.size)
            events.forEach { entry ->
                val key = entry.event.songId
                val previous = lastPlayed[key] ?: Long.MIN_VALUE
                if (entry.event.timestamp > previous) {
                    lastPlayed[key] = entry.event.timestamp
                }
            }
            SongData(all = all, liked = liked, lastPlayed = lastPlayed)
        }

    private fun SongData.toState(
        filter: SongFilter,
        sort: SongSortType,
        descending: Boolean,
    ): LibraryListState<Song> {
        val base =
            when (filter) {
                SongFilter.ALL, SongFilter.RECENT -> all
                SongFilter.LIKED -> liked
            }
        val sortComparator = songComparator(sort, descending)
        // "Recent" pins last-played ordering first; the sort chips order within those groups.
        val comparator =
            if (filter == SongFilter.RECENT) {
                compareByDescending<Song> { lastPlayed[it.id] ?: Long.MIN_VALUE }.then(sortComparator)
            } else {
                sortComparator
            }
        return LibraryListState(items = base.sortedWith(comparator), isLoading = false)
    }

    private fun songComparator(
        sort: SongSortType,
        descending: Boolean,
    ): Comparator<Song> {
        val base: Comparator<Song> =
            when (sort) {
                SongSortType.TITLE -> titleComparator<Song> { it.title }
                SongSortType.DATE_ADDED -> compareBy { it.song.inLibrary ?: LocalDateTime.MIN }
                SongSortType.DURATION -> compareBy { it.duration }
                SongSortType.ARTIST -> titleComparator<Song> { it.orderedArtists.firstOrNull()?.name.orEmpty() }
            }
        return if (descending) base.reversed() else base
    }

    companion object {
        /** Plain factory — the view model resolves everything through [ServiceLocator]. */
        val Factory: ViewModelProvider.Factory =
            viewModelFactory { initializer { LibrarySongsViewModel() } }
    }
}

/* =============================================================================================
 *  Artists tab — route `artists`
 * ========================================================================================== */

/**
 * View model of the artists tab (route `artists`).
 *
 * Exposes `dao.artists()` as [state] plus the [sort] / [sortDescending] chip state and [retry].
 */
class LibraryArtistsViewModel : ViewModel() {

    private val dao = ServiceLocator.container.dao

    private val _sort = MutableStateFlow(ArtistSortType.TITLE)
    val sort: StateFlow<ArtistSortType> = _sort.asStateFlow()

    private val _sortDescending = MutableStateFlow(false)
    val sortDescending: StateFlow<Boolean> = _sortDescending.asStateFlow()

    private val retryTrigger = MutableStateFlow(0L)

    val state: StateFlow<LibraryListState<Artist>> =
        retryTrigger
            .flatMapLatest {
                combine(dao.artists(), _sort, _sortDescending) { artists, sort, descending ->
                    LibraryListState(items = artists.sortedWith(artistComparator(sort, descending)), isLoading = false)
                }.asLibraryState()
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryListState())

    fun setSort(value: ArtistSortType) {
        _sort.value = value
    }

    fun toggleSortDescending() {
        _sortDescending.value = !_sortDescending.value
    }

    /** Restarts the backing query after [LibraryListState.error]. */
    fun retry() {
        retryTrigger.value += 1L
    }

    private fun artistComparator(
        sort: ArtistSortType,
        descending: Boolean,
    ): Comparator<Artist> {
        val base: Comparator<Artist> =
            when (sort) {
                ArtistSortType.TITLE -> titleComparator<Artist> { it.name }
                ArtistSortType.SONG_COUNT -> compareBy { it.songs.size }
                ArtistSortType.MOST_PLAYED -> compareBy { it.songs.sumOf { song -> song.song.totalPlayTimeMs } }
            }
        return if (descending) base.reversed() else base
    }

    companion object {
        /** Plain factory — the view model resolves everything through [ServiceLocator]. */
        val Factory: ViewModelProvider.Factory =
            viewModelFactory { initializer { LibraryArtistsViewModel() } }
    }
}

/* =============================================================================================
 *  Albums tab — route `albums`
 * ========================================================================================== */

/**
 * View model of the albums tab (route `albums`).
 *
 * Exposes `dao.albums()` as [state] plus the [sort] / [sortDescending] chip state and [retry].
 */
class LibraryAlbumsViewModel : ViewModel() {

    private val dao = ServiceLocator.container.dao

    private val _sort = MutableStateFlow(AlbumSortType.TITLE)
    val sort: StateFlow<AlbumSortType> = _sort.asStateFlow()

    private val _sortDescending = MutableStateFlow(false)
    val sortDescending: StateFlow<Boolean> = _sortDescending.asStateFlow()

    private val retryTrigger = MutableStateFlow(0L)

    val state: StateFlow<LibraryListState<Album>> =
        retryTrigger
            .flatMapLatest {
                combine(dao.albums(), _sort, _sortDescending) { albums, sort, descending ->
                    LibraryListState(items = albums.sortedWith(albumComparator(sort, descending)), isLoading = false)
                }.asLibraryState()
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryListState())

    fun setSort(value: AlbumSortType) {
        _sort.value = value
    }

    fun toggleSortDescending() {
        _sortDescending.value = !_sortDescending.value
    }

    /** Restarts the backing query after [LibraryListState.error]. */
    fun retry() {
        retryTrigger.value += 1L
    }

    private fun albumComparator(
        sort: AlbumSortType,
        descending: Boolean,
    ): Comparator<Album> {
        val base: Comparator<Album> =
            when (sort) {
                AlbumSortType.TITLE -> titleComparator<Album> { it.title }
                AlbumSortType.YEAR -> compareBy { it.year ?: 0 }
                AlbumSortType.SONG_COUNT -> compareBy { it.songs.size }
                AlbumSortType.MOST_PLAYED -> compareBy { it.songs.sumOf { song -> song.song.totalPlayTimeMs } }
            }
        return if (descending) base.reversed() else base
    }

    companion object {
        /** Plain factory — the view model resolves everything through [ServiceLocator]. */
        val Factory: ViewModelProvider.Factory =
            viewModelFactory { initializer { LibraryAlbumsViewModel() } }
    }
}

/* =============================================================================================
 *  Playlists tab — route `playlists`
 * ========================================================================================== */

/**
 * View model of the playlists tab (route `playlists`).
 *
 * Exposes the device-owned playlists (`MusicRepository.localPlaylists()`, i.e. `dao.playlists()`
 * filtered to `browseId == null`) as [state], the [sort] / [sortDescending] chip state, a
 * [retry] hook and [createPlaylist], which persists a new empty playlist through the frozen
 * repository API.
 */
class LibraryPlaylistsViewModel : ViewModel() {

    private val musicRepository: MusicRepository = ServiceLocator.container.musicRepository

    private val _sort = MutableStateFlow(PlaylistSortType.NAME)
    val sort: StateFlow<PlaylistSortType> = _sort.asStateFlow()

    private val _sortDescending = MutableStateFlow(false)
    val sortDescending: StateFlow<Boolean> = _sortDescending.asStateFlow()

    private val retryTrigger = MutableStateFlow(0L)

    val state: StateFlow<LibraryListState<Playlist>> =
        retryTrigger
            .flatMapLatest {
                combine(musicRepository.localPlaylists(), _sort, _sortDescending) { playlists, sort, descending ->
                    LibraryListState(items = playlists.sortedWith(playlistComparator(sort, descending)), isLoading = false)
                }.asLibraryState()
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryListState())

    fun setSort(value: PlaylistSortType) {
        _sort.value = value
    }

    fun toggleSortDescending() {
        _sortDescending.value = !_sortDescending.value
    }

    /** Restarts the backing query after [LibraryListState.error]. */
    fun retry() {
        retryTrigger.value += 1L
    }

    /** Creates a new empty local playlist; blank names are ignored. */
    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { musicRepository.createLocalPlaylist(trimmed) }
        }
    }

    private fun playlistComparator(
        sort: PlaylistSortType,
        descending: Boolean,
    ): Comparator<Playlist> {
        val base: Comparator<Playlist> =
            when (sort) {
                PlaylistSortType.NAME -> titleComparator<Playlist> { it.name }
                PlaylistSortType.CREATED -> compareBy { it.playlist.createdAt }
                PlaylistSortType.UPDATED -> compareBy { it.playlist.lastUpdatedAt }
            }
        return if (descending) base.reversed() else base
    }

    companion object {
        /** Plain factory — the view model resolves everything through [ServiceLocator]. */
        val Factory: ViewModelProvider.Factory =
            viewModelFactory { initializer { LibraryPlaylistsViewModel() } }
    }
}
