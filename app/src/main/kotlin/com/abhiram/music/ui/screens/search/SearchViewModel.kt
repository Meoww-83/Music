package com.abhiram.music.ui.screens.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.abhiram.innertube.YouTube
import com.abhiram.innertube.models.AlbumItem
import com.abhiram.innertube.models.ArtistItem
import com.abhiram.innertube.models.EpisodeItem
import com.abhiram.innertube.models.PlaylistItem
import com.abhiram.innertube.models.PodcastItem
import com.abhiram.innertube.models.SearchSuggestions
import com.abhiram.innertube.models.SongItem
import com.abhiram.innertube.models.YTItem
import com.abhiram.innertube.models.filterExplicit
import com.abhiram.music.database.DatabaseDao
import com.abhiram.music.database.entities.SearchHistory
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.repository.MusicRepository
import com.abhiram.music.repository.YouTubeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Filter chips offered on the search results screen, each mapped onto the real InnerTube
 * search filter (`YouTube.SearchFilter`). [ALL] performs an unfiltered search whose flat item
 * list is grouped into sections client side.
 */
enum class SearchFilterType(
    val label: String,
    val filter: YouTube.SearchFilter?,
) {
    ALL("All", null),
    SONGS("Songs", YouTube.SearchFilter.FILTER_SONG),
    VIDEOS("Videos", YouTube.SearchFilter.FILTER_VIDEO),
    ALBUMS("Albums", YouTube.SearchFilter.FILTER_ALBUM),
    ARTISTS("Artists", YouTube.SearchFilter.FILTER_ARTIST),
    PLAYLISTS("Playlists", YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST),
    ;
}

/** A titled group of results produced by the [SearchFilterType.ALL] search. */
data class SearchSection(
    val title: String,
    val items: List<YTItem>,
)

/** Cached page of a filtered search together with its pending continuation token. */
data class FilteredResults(
    val items: List<YTItem> = emptyList(),
    val continuation: String? = null,
    val loaded: Boolean = false,
)

/**
 * State holder for the search screen.
 *
 * Owns the query/debounced suggestion pipeline, the summary ("All") search and the per-filter
 * result pages with infinite scroll continuations. Persists committed queries through
 * `MusicRepository.pushSearchHistory()` (guarded by the search-history preference) and resolves
 * everything through `ServiceLocator` — no DI framework involved.
 */
class SearchViewModel : ViewModel() {
    private val musicRepository: MusicRepository = ServiceLocator.container.musicRepository
    private val youTubeRepository: YouTubeRepository = ServiceLocator.container.youTubeRepository
    private val preferences = ServiceLocator.container.preferences
    private val dao: DatabaseDao = ServiceLocator.container.dao

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** The query whose results are currently rendered (set when the user commits a search). */
    private val _resultsQuery = MutableStateFlow("")
    val resultsQuery: StateFlow<String> = _resultsQuery.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilterType.ALL)
    val filter: StateFlow<SearchFilterType> = _filter.asStateFlow()

    /** Latest suggestion payload for the debounced query, `null` while idle. */
    private val _suggestions = MutableStateFlow<SearchSuggestions?>(null)
    val suggestions: StateFlow<SearchSuggestions?> = _suggestions.asStateFlow()

    /** Recently persisted searches, newest first. */
    val history: StateFlow<List<SearchHistory>> =
        musicRepository
            .searchHistory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Sections of the "All" (summary) search. */
    var sections: List<SearchSection> by mutableStateOf(emptyList())
        private set

    /** Per-filter result pages, keyed by chip. */
    val filteredResults = mutableStateMapOf<SearchFilterType, FilteredResults>()

    var isLoading: Boolean by mutableStateOf(false)
        private set

    var isLoadingMore: Boolean by mutableStateOf(false)
        private set

    /** Human readable failure of the most recent request, `null` when healthy. */
    var errorMessage: String? by mutableStateOf(null)
        private set

    /** Playable items of the currently displayed results, in play order. */
    var playableItems: List<SongItem> by mutableStateOf(emptyList())
        private set

    init {
        // Debounced suggestion pipeline (300 ms).
        viewModelScope.launch {
            _query
                .debounce(300L)
                .distinctUntilChanged()
                .collectLatest { q ->
                    if (q.isBlank()) {
                        _suggestions.value = null
                    } else {
                        youTubeRepository
                            .suggestions(q)
                            .onSuccess { _suggestions.value = it }
                            .onFailure { _suggestions.value = null }
                    }
                }
        }

        // Result pipeline: re-search whenever the committed query or the filter chip changes.
        viewModelScope.launch {
            combine(_resultsQuery, _filter) { q, f -> q to f }
                .distinctUntilChanged()
                .collectLatest { (q, f) ->
                    if (q.isBlank()) {
                        clearResults()
                    } else {
                        performSearch(q, f)
                    }
                }
        }
    }

    /** Updates the raw query text and restarts the suggestion debounce. */
    fun onQueryChange(value: String) {
        _query.value = value
    }

    /** Clears the query text and the inline suggestions. */
    fun clearQuery() {
        _query.value = ""
        _suggestions.value = null
    }

    /**
     * Commits [value] (defaults to the current query) as the active search: persists it to the
     * search history when enabled, resets the filter chip and loads fresh results.
     */
    fun commitSearch(value: String = _query.value) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        _query.value = trimmed
        _suggestions.value = null
        viewModelScope.launch {
            val historyEnabled =
                runCatching { preferences.searchHistoryEnabled.first() }.getOrDefault(true)
            if (historyEnabled) {
                musicRepository.pushSearchHistory(trimmed)
            }
        }
        _filter.value = SearchFilterType.ALL
        _resultsQuery.value = trimmed
    }

    /** Switches the active filter chip, reusing cached pages when available. */
    fun setFilter(type: SearchFilterType) {
        _filter.value = type
    }

    /** Requests the next continuation page of the active filtered search. */
    fun loadMore() {
        val q = _resultsQuery.value
        val f = _filter.value
        if (q.isBlank() || f == SearchFilterType.ALL) return
        val current = filteredResults[f] ?: return
        val continuation = current.continuation ?: return
        if (isLoadingMore || isLoading) return
        isLoadingMore = true
        viewModelScope.launch {
            youTubeRepository
                .search(q, f.filter, continuation)
                .onSuccess { result ->
                    val hideExplicit =
                        runCatching { preferences.hideExplicit.first() }.getOrDefault(false)
                    val merged = (current.items + result.items.filterExplicit(hideExplicit))
                        .distinctBy { it.id }
                    filteredResults[f] =
                        FilteredResults(items = merged, continuation = result.continuation, loaded = true)
                    playableItems = playableFrom(merged)
                    errorMessage = null
                }
            isLoadingMore = false
        }
    }

    /** Deletes a single recent-search entry. */
    fun deleteHistory(query: String) {
        viewModelScope.launch { dao.deleteSearchHistory(query) }
    }

    /**
     * Loads the results for one (query, filter) combination: the "All" filter renders grouped
     * sections, every other chip keeps a cached, continuation-paged result list.
     */
    private suspend fun performSearch(
        query: String,
        filterType: SearchFilterType,
    ) {
        val hideExplicit = runCatching { preferences.hideExplicit.first() }.getOrDefault(false)

        if (filterType == SearchFilterType.ALL) {
            isLoading = true
            errorMessage = null
            youTubeRepository
                .search(query, null, null)
                .onSuccess { result ->
                    val items = result.items.filterExplicit(hideExplicit)
                    sections = groupIntoSections(items)
                    playableItems = playableFrom(items)
                    errorMessage = null
                }.onFailure { throwable ->
                    errorMessage = throwable.message ?: "Something went wrong"
                }
            isLoading = false
            return
        }

        val cached = filteredResults[filterType]
        if (cached != null && cached.loaded) {
            playableItems = playableFrom(cached.items)
            return
        }
        isLoading = true
        errorMessage = null
        youTubeRepository
            .search(query, filterType.filter, null)
            .onSuccess { result ->
                val items = result.items.filterExplicit(hideExplicit).distinctBy { it.id }
                filteredResults[filterType] =
                    FilteredResults(items = items, continuation = result.continuation, loaded = true)
                playableItems = playableFrom(items)
                errorMessage = null
            }.onFailure { throwable ->
                errorMessage = throwable.message ?: "Something went wrong"
            }
        isLoading = false
    }

    private fun clearResults() {
        sections = emptyList()
        filteredResults.clear()
        playableItems = emptyList()
        errorMessage = null
        isLoading = false
        isLoadingMore = false
    }

    companion object {
        /** Plain factory — the view model resolves everything through [ServiceLocator]. */
        val Factory: ViewModelProvider.Factory =
            viewModelFactory { initializer { SearchViewModel() } }

        private fun groupIntoSections(items: List<YTItem>): List<SearchSection> {
            val songs = mutableListOf<YTItem>()
            val videos = mutableListOf<YTItem>()
            val albums = mutableListOf<YTItem>()
            val artists = mutableListOf<YTItem>()
            val playlists = mutableListOf<YTItem>()
            val other = mutableListOf<YTItem>()
            items.forEach { item ->
                val bucket =
                    when (item) {
                        is SongItem -> if (item.isVideoSong) videos else songs
                        is EpisodeItem -> songs
                        is AlbumItem -> albums
                        is ArtistItem -> artists
                        is PlaylistItem -> playlists
                        is PodcastItem -> playlists
                        else -> other
                    }
                bucket.add(item)
            }
            return buildList {
                addIfNotEmpty("Songs", songs)
                addIfNotEmpty("Videos", videos)
                addIfNotEmpty("Albums", albums)
                addIfNotEmpty("Artists", artists)
                addIfNotEmpty("Playlists", playlists)
                addIfNotEmpty("Other", other)
            }
        }

        private fun MutableList<SearchSection>.addIfNotEmpty(
            title: String,
            items: List<YTItem>,
        ) {
            if (items.isNotEmpty()) add(SearchSection(title, items))
        }

        private fun playableFrom(items: List<YTItem>): List<SongItem> =
            items.mapNotNull { item ->
                when (item) {
                    is SongItem -> item
                    is EpisodeItem -> item.asSongItem()
                    else -> null
                }
            }
    }
}
