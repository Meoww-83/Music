package com.abhiram.music.repository

import com.abhiram.innertube.YouTube
import com.abhiram.innertube.models.AlbumItem
import com.abhiram.innertube.models.BrowseEndpoint
import com.abhiram.innertube.models.SearchSuggestions
import com.abhiram.innertube.models.SongItem
import com.abhiram.innertube.models.WatchEndpoint
import com.abhiram.innertube.models.YouTubeLocale
import com.abhiram.innertube.pages.AlbumPage
import com.abhiram.innertube.pages.ArtistItemsContinuationPage
import com.abhiram.innertube.pages.ArtistItemsPage
import com.abhiram.innertube.pages.ArtistPage
import com.abhiram.innertube.pages.ChartsPage
import com.abhiram.innertube.pages.ExplorePage
import com.abhiram.innertube.pages.HomePage
import com.abhiram.innertube.pages.MoodAndGenres
import com.abhiram.innertube.pages.NextResult
import com.abhiram.innertube.pages.PlaylistContinuationPage
import com.abhiram.innertube.pages.PlaylistPage
import com.abhiram.innertube.pages.RelatedPage
import com.abhiram.innertube.pages.SearchResult
import com.abhiram.innertube.pages.SearchSummaryPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Thin suspend wrappers around the [YouTube] Innertube entry point.
 *
 * * Every method runs on [Dispatchers.IO] and returns the *real* innertube `Result<Page>` — no
 *   parallel models.
 * * The Innertube locale ([YouTube.locale]) is set once from [Locale.getDefault] at construction.
 * * The first pages of `home`, `artist`, `album` and `playlist` are cached in memory for
 *   [PAGE_CACHE_TTL_MILLIS] (5 minutes) to keep tab switches and recompositions off the network.
 *   Continuation pages are never cached.
 */
class YouTubeRepository {

    init {
        val locale = Locale.getDefault()
        YouTube.locale =
            YouTubeLocale(
                gl = locale.country.ifBlank { "US" },
                hl = locale.language.ifBlank { "en" },
            )
    }

    /* ---------------------------------------------------------------------------------------
     *  Search
     * ------------------------------------------------------------------------------------ */

    /**
     * Searches YouTube Music.
     *
     * @param filter the Innertube search filter; `null` falls back to
     * [YouTube.SearchFilter.FILTER_SONG] (the songs tab).
     * @param continuation continuation token from a previous [SearchResult]; when present,
     * [query] and [filter] are ignored.
     * @param recordSearchHistory whether the query is written to the *YouTube account* search
     * history (leave `false` for background metadata lookups).
     */
    suspend fun search(
        query: String,
        filter: YouTube.SearchFilter? = null,
        continuation: String? = null,
        recordSearchHistory: Boolean = false,
    ): Result<SearchResult> =
        withContext(Dispatchers.IO) {
            if (continuation != null) {
                YouTube.searchContinuation(continuation, recordSearchHistory)
            } else {
                YouTube.search(query, filter ?: YouTube.SearchFilter.FILTER_SONG, recordSearchHistory)
            }
        }

    /** Autocomplete queries + recommended items for the search bar. */
    suspend fun suggestions(query: String): Result<SearchSuggestions> =
        withContext(Dispatchers.IO) { YouTube.searchSuggestions(query) }

    /** "Top result" style grouped search page; used for the unfiltered search results view. */
    suspend fun searchSummary(query: String, recordSearchHistory: Boolean = false): Result<SearchSummaryPage> =
        withContext(Dispatchers.IO) { YouTube.searchSummary(query, recordSearchHistory) }

    /* ---------------------------------------------------------------------------------------
     *  Artist (cached)
     * ------------------------------------------------------------------------------------ */

    /** Artist overview page (header + sections). Cached for [PAGE_CACHE_TTL_MILLIS]. */
    suspend fun artist(id: String): Result<ArtistPage> =
        withContext(Dispatchers.IO) {
            cached("artist:$id") { YouTube.artist(id) }
        }

    /** One tab of an artist page (songs / albums / videos / playlists), via its browse endpoint. */
    suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage> =
        withContext(Dispatchers.IO) { YouTube.artistItems(endpoint) }

    /** Next page of an [ArtistItemsPage] tab. */
    suspend fun artistItemsContinuation(continuation: String): Result<ArtistItemsContinuationPage> =
        withContext(Dispatchers.IO) { YouTube.artistItemsContinuation(continuation) }

    /* ---------------------------------------------------------------------------------------
     *  Album (cached)
     * ------------------------------------------------------------------------------------ */

    /**
     * Album page including its songs (innertube follows the album playlist continuations
     * internally). Cached for [PAGE_CACHE_TTL_MILLIS].
     */
    suspend fun album(browseId: String): Result<AlbumPage> =
        withContext(Dispatchers.IO) {
            cached("album:$browseId") { YouTube.album(browseId) }
        }

    /**
     * Songs of an album identified by its *playlist* id (`OLAK5uy_…` / `VL…`).
     *
     * @param album optional header item used to fill in artists/cover for sparse responses.
     * Continuations are followed internally by innertube.
     */
    suspend fun albumSongs(playlistId: String, album: AlbumItem? = null): Result<List<SongItem>> =
        withContext(Dispatchers.IO) { YouTube.albumSongs(playlistId, album) }

    /* ---------------------------------------------------------------------------------------
     *  Playlist (cached)
     * ------------------------------------------------------------------------------------ */

    /** Playlist page (header + first songs). Cached for [PAGE_CACHE_TTL_MILLIS]. */
    suspend fun playlist(id: String): Result<PlaylistPage> =
        withContext(Dispatchers.IO) {
            cached("playlist:$id") { YouTube.playlist(id) }
        }

    /** Next page of a playlist. */
    suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage> =
        withContext(Dispatchers.IO) { YouTube.playlistContinuation(continuation) }

    /* ---------------------------------------------------------------------------------------
     *  Home / Explore / Charts
     * ------------------------------------------------------------------------------------ */

    /**
     * Home page carousels. The un-continued first page is cached for
     * [PAGE_CACHE_TTL_MILLIS]; continuation pages always hit the network.
     */
    suspend fun home(continuation: String? = null, params: String? = null): Result<HomePage> =
        withContext(Dispatchers.IO) {
            if (continuation == null && params == null) {
                cached("home") { YouTube.home() }
            } else {
                YouTube.home(continuation, params)
            }
        }

    /** Explore tab (new releases + moods & genres). */
    suspend fun explore(): Result<ExplorePage> =
        withContext(Dispatchers.IO) { YouTube.explore() }

    /** Charts tab. */
    suspend fun charts(continuation: String? = null): Result<ChartsPage> =
        withContext(Dispatchers.IO) { YouTube.getChartsPage(continuation) }

    /** "New releases albums" grid. */
    suspend fun newReleaseAlbums(): Result<List<AlbumItem>> =
        withContext(Dispatchers.IO) { YouTube.newReleaseAlbums() }

    /** "Moods & genres" category list. */
    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> =
        withContext(Dispatchers.IO) { YouTube.moodAndGenres() }

    /* ---------------------------------------------------------------------------------------
     *  Watch-page helpers
     * ------------------------------------------------------------------------------------ */

    /** "Related" tab of a watch page (videos + albums + artists + playlists). */
    suspend fun related(endpoint: BrowseEndpoint): Result<RelatedPage> =
        withContext(Dispatchers.IO) { YouTube.related(endpoint) }

    /** Autoplay queue / radio for a watch endpoint, including its continuation token. */
    suspend fun next(endpoint: WatchEndpoint, continuation: String? = null): Result<NextResult> =
        withContext(Dispatchers.IO) { YouTube.next(endpoint, continuation) }

    /** Resolves metadata for a set of video ids and/or a playlist id (queue reconstruction). */
    suspend fun queue(videoIds: List<String>? = null, playlistId: String? = null): Result<List<SongItem>> =
        withContext(Dispatchers.IO) { YouTube.queue(videoIds, playlistId) }

    /** Synced lyrics text for a `MUSIC_video_{videoId}` browse endpoint; `null` when absent. */
    suspend fun lyrics(endpoint: BrowseEndpoint): Result<String?> =
        withContext(Dispatchers.IO) { YouTube.lyrics(endpoint) }

    /* ---------------------------------------------------------------------------------------
     *  Cache
     * ------------------------------------------------------------------------------------ */

    /** Drops every cached page (e.g. on logout or explicit refresh). */
    fun clearCache() = pageCache.clear()

    private class CacheEntry(val value: Any, val expiresAtMillis: Long)

    private val pageCache = ConcurrentHashMap<String, CacheEntry>()

    /** Returns the cached success value when fresh, otherwise runs [fetch] and caches successes. */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> cached(key: String, fetch: suspend () -> Result<T>): Result<T> {
        val now = System.currentTimeMillis()
        pageCache[key]?.let { entry ->
            if (entry.expiresAtMillis > now) return entry.value as Result<T>
        }
        val result = fetch()
        // Only successful pages are cached; failures must retry.
        result.onSuccess {
            pageCache[key] = CacheEntry(Result.success(it), now + PAGE_CACHE_TTL_MILLIS)
        }
        return result
    }

    private companion object {
        /** Freshness window for first-page home/artist/album/playlist responses. */
        const val PAGE_CACHE_TTL_MILLIS: Long = 5L * 60 * 1000
    }
}
