package com.abhiram.music.repository

import com.abhiram.innertube.models.AlbumItem
import com.abhiram.innertube.models.ArtistItem
import com.abhiram.innertube.models.EpisodeItem
import com.abhiram.innertube.models.PlaylistItem
import com.abhiram.innertube.models.PodcastItem
import com.abhiram.innertube.models.SongItem
import com.abhiram.innertube.models.YTItem
import com.abhiram.innertube.pages.AlbumPage
import com.abhiram.innertube.pages.ArtistItemsPage
import com.abhiram.innertube.pages.ArtistPage
import com.abhiram.innertube.pages.HomePage
import com.abhiram.innertube.pages.PlaylistContinuationPage
import com.abhiram.innertube.pages.PlaylistPage
import com.abhiram.innertube.pages.RelatedPage
import com.abhiram.innertube.pages.SearchResult
import com.abhiram.innertube.pages.SearchSummaryPage
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.model.toMediaMetadata

/*
 * Mappers between InnerTube page/item models and the app-facing `MediaMetadata` model.
 *
 * Screens never parse innertube responses themselves — they call these helpers (or the typed
 * per-item slices below) on the `Result<Page>` values returned by [YouTubeRepository].
 */

/* -------------------------------------------------------------------------------------------
 *  Single items
 * ---------------------------------------------------------------------------------------- */

/**
 * Converts a playable innertube item (song, video or episode) to [MediaMetadata].
 * Returns `null` for non-playable items (albums, artists, playlists, podcasts).
 */
fun YTItem.toMediaMetadataOrNull(): MediaMetadata? =
    when (this) {
        is SongItem -> toMediaMetadata()
        is EpisodeItem -> toMediaMetadata()
        else -> null
    }

/* -------------------------------------------------------------------------------------------
 *  Lists
 * ---------------------------------------------------------------------------------------- */

/**
 * Maps a heterogeneous [YTItem] list to [MediaMetadata], dropping non-playable entries
 * (albums, artists, playlists, podcasts). [SongItem] and [EpisodeItem] are [YTItem] subtypes,
 * so this single extension covers every list the parsers produce.
 */
fun List<YTItem>.toMediaMetadataList(): List<MediaMetadata> = mapNotNull { it.toMediaMetadataOrNull() }

/* -------------------------------------------------------------------------------------------
 *  Typed slices of a mixed item list (search results, home sections, artist pages…)
 * ---------------------------------------------------------------------------------------- */

/** Songs only (excludes episodes, which are listed separately by [episodes]). */
fun List<YTItem>.songs(): List<SongItem> =
    filterIsInstance<SongItem>().filterNot { it.isEpisode }

/** Music videos only (songs whose `musicVideoType` is not ATV). */
fun List<YTItem>.videos(): List<SongItem> =
    filterIsInstance<SongItem>().filterNot { it.isEpisode }.filter { it.isVideoSong }

fun List<YTItem>.albums(): List<AlbumItem> = filterIsInstance<AlbumItem>()

fun List<YTItem>.artists(): List<ArtistItem> = filterIsInstance<ArtistItem>()

fun List<YTItem>.playlists(): List<PlaylistItem> = filterIsInstance<PlaylistItem>()

fun List<YTItem>.podcasts(): List<PodcastItem> = filterIsInstance<PodcastItem>()

fun List<YTItem>.episodes(): List<EpisodeItem> = filterIsInstance<EpisodeItem>()

/* -------------------------------------------------------------------------------------------
 *  Page-level conveniences
 * ---------------------------------------------------------------------------------------- */

/** All playable [MediaMetadata] contained anywhere in this search result. */
fun SearchResult.toMediaMetadataList(): List<MediaMetadata> = items.toMediaMetadataList()

fun SearchResult.songs(): List<SongItem> = items.songs()

fun SearchResult.videos(): List<SongItem> = items.videos()

fun SearchResult.albums(): List<AlbumItem> = items.albums()

fun SearchResult.artists(): List<ArtistItem> = items.artists()

fun SearchResult.playlists(): List<PlaylistItem> = items.playlists()

fun SearchResult.podcasts(): List<PodcastItem> = items.podcasts()

fun SearchResult.episodes(): List<EpisodeItem> = items.episodes()

/** Flattens all summaries of a "top result" style page into one ordered item list. */
fun SearchSummaryPage.allItems(): List<YTItem> = summaries.flatMap { it.items }

/** Items of a single titled section of [SearchSummaryPage], if that section exists. */
fun SearchSummaryPage.section(title: String): List<YTItem> =
    summaries.firstOrNull { it.title == title }?.items.orEmpty()

/** Flattens all summaries into playable [MediaMetadata]. */
fun SearchSummaryPage.toMediaMetadataList(): List<MediaMetadata> = allItems().toMediaMetadataList()

/** Every item of every home carousel, in page order. */
fun HomePage.items(): List<YTItem> = sections.flatMap { it.items }

/** Playable items across all home carousels (quick picks, songs, videos, episodes…). */
fun HomePage.toMediaMetadataList(): List<MediaMetadata> = items().toMediaMetadataList()

/** The first home carousel whose items contain at least one song — i.e. "Quick picks". */
fun HomePage.quickPicksSection(): HomePage.Section? =
    sections.firstOrNull { section -> section.items.any { it is SongItem && !it.isEpisode } }

/** All songs of an album page, in track order. */
fun AlbumPage.toMediaMetadataList(): List<MediaMetadata> = songs.toMediaMetadataList()

/** All songs of a playlist page, in playlist order. */
fun PlaylistPage.toMediaMetadataList(): List<MediaMetadata> = songs.toMediaMetadataList()

/** Songs of one playlist continuation page. */
fun PlaylistContinuationPage.toMediaMetadataList(): List<MediaMetadata> = songs.toMediaMetadataList()

/** Every playable item across all artist page sections (songs, videos, albums…). */
fun ArtistPage.items(): List<YTItem> = sections.flatMap { it.items }

/** Songs scattered over the artist page's sections (albums/playlists are not expanded). */
fun ArtistPage.songs(): List<SongItem> = items().songs()

/** Artist page items of a single section by exact title, or `null` when absent. */
fun ArtistPage.section(title: String): List<YTItem>? =
    sections.firstOrNull { it.title == title }?.items

/** Playable items of an artist "songs"/"videos"/"albums" tab page. */
fun ArtistItemsPage.toMediaMetadataList(): List<MediaMetadata> = items.toMediaMetadataList()

/** Related-page songs, converted for the "play all"/radio affordances. */
fun RelatedPage.toMediaMetadataList(): List<MediaMetadata> = songs.toMediaMetadataList()

/** Everything on the related page, songs first then albums/artists/playlists. */
fun RelatedPage.allItems(): List<YTItem> = songs + albums + artists + playlists
