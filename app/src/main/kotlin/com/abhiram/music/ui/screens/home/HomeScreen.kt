package com.abhiram.music.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.innertube.models.AlbumItem
import com.abhiram.innertube.models.ArtistItem
import com.abhiram.innertube.models.PlaylistItem
import com.abhiram.innertube.models.SongItem
import com.abhiram.innertube.pages.HomePage
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.model.toMediaMetadata
import com.abhiram.music.playback.PlayerControllerProvider
import com.abhiram.music.ui.component.TextPlaceholder
import com.abhiram.music.ui.component.screen.HomeAlbums
import com.abhiram.music.ui.component.screen.HomeArtistList
import com.abhiram.music.ui.component.screen.HomePlaylists
import com.abhiram.music.ui.component.screen.HomeSongs
import com.abhiram.music.ui.component.screen.QuickPicks
import com.abhiram.music.ui.navigation.Routes
import com.abhiram.music.ui.screens.library.MediaMenuHost

/**
 * Home screen (route `home`).
 *
 * Layout follows the design language (§9): the **top 40 % of the screen is the current dynamic
 * colour fading into pure black**, with the chunky "Home" title, the home filter chips and the
 * content carousels stacked on top of it. The screen assembles the CMP-D home components —
 * [QuickPicks], [HomeSongs], [HomeAlbums], [HomeArtistList] and [HomePlaylists] — one row per
 * server carousel, in page order, plus a "New releases" album row from
 * `YouTubeRepository.newReleaseAlbums()`.
 *
 * Behaviour: pull-to-refresh reloads the page ([HomeViewModel.refresh], which keeps the current
 * content on screen until the new page arrives), the first load draws the components' shimmer
 * skeletons, a failed first load shows a retry state and a failed *refresh* shows an inline
 * retry banner above the content. Song taps build the queue through
 * [PlayerControllerProvider.playAll] (cold start, no shuffle), long-pressing a song opens the
 * shared [com.abhiram.music.ui.menu.MediaItemMenu], and album/artist/playlist cards navigate
 * with [Routes].
 *
 * @param onNavigate Invoked with a destination route (`album/{id}`, `artist/{id}`,
 * `playlist/{id}`…) for entries that leave the screen.
 * @param modifier Modifier applied to the screen root.
 * @param viewModel View model holding the home page, chips and refresh state.
 */
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val context = LocalContext.current

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedChip by viewModel.selectedChip.collectAsStateWithLifecycle()

    var menuTarget by remember { mutableStateOf<MediaMetadata?>(null) }

    /** Replaces the queue with [songs] and starts at [index] (no shuffle, §3.2 cold start). */
    fun playSongs(songs: List<MediaMetadata>, index: Int) {
        if (songs.isEmpty()) return
        PlayerControllerProvider.playAll(context, songs, index, false)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Top 40 % dynamic colour → pure black, above a solid black base (§9).
        Box(modifier = Modifier.matchParentSize().background(Color.Black))
        Box(modifier = Modifier.matchParentSize().background(homeBackgroundBrush()))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.weight(1f))
            }

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when {
                    state.isLoading && !state.hasContent -> HomeLoadingSkeleton()

                    state.error != null && !state.hasContent ->
                        HomeErrorState(
                            message = state.error.orEmpty(),
                            onRetry = viewModel::refresh,
                        )

                    else -> HomeContent(
                        state = state,
                        selectedChip = selectedChip,
                        onChipClick = viewModel::selectChip,
                        onRetryRefresh = viewModel::refresh,
                        onSongClick = ::playSongs,
                        onSongLongClick = { song -> menuTarget = song },
                        onNavigate = onNavigate,
                    )
                }
            }
        }

        MediaMenuHost(
            item = menuTarget,
            onDismiss = { menuTarget = null },
            onNavigate = onNavigate,
        )
    }
}

/* =============================================================================================
 *  Background
 * ========================================================================================== */

/**
 * The home gradient: the current dynamic colour ([MaterialTheme.colorScheme.primary]) at the very
 * top, fading to transparent — i.e. the solid black base — by the 40 % mark, and pure black for
 * the rest of the screen (design rule §9).
 */
@Composable
private fun homeBackgroundBrush(): Brush =
    Brush.verticalGradient(
        0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
        0.40f to Color.Transparent,
        1f to Color.Transparent,
    )

/* =============================================================================================
 *  Loading / failure states
 * ========================================================================================== */

/**
 * First-load skeleton: every CMP-D home component renders its own shimmer when `isLoading` and
 * empty, so the skeleton is simply the content layout with empty data.
 */
@Composable
private fun HomeLoadingSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        QuickPicks(songs = emptyList(), isLoading = true)
        HomeSongs(songs = emptyList(), isLoading = true, title = "Songs")
        HomeAlbums(
            albums = emptyList(),
            isLoading = true,
            title = "Albums",
            onAlbumClick = { },
        )
        HomeArtistList(
            artists = emptyList(),
            isLoading = true,
            title = "Artists",
            onArtistClick = { },
        )
    }
}

/**
 * Full-screen failure state shown when the very first load failed: headline, the underlying
 * message and a ≥48 dp retry button.
 *
 * @param message Failure message produced by [HomeViewModel].
 * @param onRetry Invoked by the retry button.
 * @param modifier Modifier applied to the state.
 */
@Composable
private fun HomeErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Couldn't load your home page",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Inline retry banner shown *above* already loaded content when a pull-to-refresh failed, so a
 * flaky network never blanks a populated screen.
 */
@Composable
private fun HomeRefreshErrorBanner(
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/* =============================================================================================
 *  Content
 * ========================================================================================== */

/**
 * The scrollable home body: optional refresh-error banner, filter chips, [QuickPicks], the
 * "New releases" album row and one row group per server carousel.
 */
@Composable
private fun HomeContent(
    state: HomeUiState,
    selectedChip: HomePage.Chip?,
    onChipClick: (HomePage.Chip) -> Unit,
    onRetryRefresh: () -> Unit,
    onSongClick: (List<MediaMetadata>, Int) -> Unit,
    onSongLongClick: (MediaMetadata) -> Unit,
    onNavigate: (String) -> Unit,
) {
    val quickPicks = remember(state.quickPicks) { state.quickPicks.map { it.toMediaMetadata() } }
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.error != null && state.hasContent) {
            item(key = "refreshError") {
                HomeRefreshErrorBanner(
                    message = state.error.orEmpty(),
                    onRetry = onRetryRefresh,
                )
            }
        }

        if (state.chips.isNotEmpty()) {
            item(key = "chips") {
                HomeChipRow(
                    chips = state.chips,
                    selected = selectedChip,
                    onSelected = onChipClick,
                )
            }
        }

        if (quickPicks.isNotEmpty()) {
            item(key = "quickPicks") {
                QuickPicks(
                    songs = quickPicks,
                    title = state.quickPicksTitle,
                    onSongClick = { index -> onSongClick(quickPicks, index) },
                    onSongLongClick = onSongLongClick,
                )
            }
        }

        if (state.newReleases.isNotEmpty()) {
            item(key = "newReleases") {
                HomeAlbums(
                    albums = state.newReleases,
                    title = "New releases",
                    onAlbumClick = { album -> onNavigate(Routes.album(album.browseId)) },
                )
            }
        }

        state.sections.forEachIndexed { index, section ->
            item(key = "section-$index") {
                HomeSectionRows(
                    section = section,
                    onSongClick = onSongClick,
                    onSongLongClick = onSongLongClick,
                    onNavigate = onNavigate,
                )
            }
        }

        if (!state.hasContent && state.error == null) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TextPlaceholder(text = "Nothing to show yet — pull down to refresh")
                }
            }
        }
    }
}

/**
 * Renders one server carousel: its items are grouped by type and each non-empty group becomes a
 * row. The first (leading) group takes the carousel's own title, any additional groups fall back
 * to their generic row title.
 *
 * @param section Carousel as parsed by innertube (`HomePage.Section`).
 * @param onSongClick Receives the row's songs and the tapped index for playback.
 * @param onSongLongClick Opens the shared media menu for a song.
 * @param onNavigate Receives `album/{id}` / `artist/{id}` / `playlist/{id}` routes.
 */
@Composable
private fun HomeSectionRows(
    section: HomePage.Section,
    onSongClick: (List<MediaMetadata>, Int) -> Unit,
    onSongLongClick: (MediaMetadata) -> Unit,
    onNavigate: (String) -> Unit,
) {
    val songs = remember(section) {
        section.items
            .filterIsInstance<SongItem>()
            .filterNot { it.isEpisode }
            .map { it.toMediaMetadata() }
    }
    val albums = remember(section) { section.items.filterIsInstance<AlbumItem>() }
    val artists = remember(section) { section.items.filterIsInstance<ArtistItem>() }
    val playlists = remember(section) { section.items.filterIsInstance<PlaylistItem>() }

    // The leading non-empty group inherits the server carousel title; the rest use row defaults.
    var titleTaken = false
    fun nextTitle(default: String): String =
        if (titleTaken) {
            default
        } else {
            titleTaken = true
            section.title
        }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (songs.isNotEmpty()) {
            HomeSongs(
                songs = songs,
                title = nextTitle("Songs"),
                onSongClick = { index -> onSongClick(songs, index) },
                onSongLongClick = onSongLongClick,
            )
        }
        if (albums.isNotEmpty()) {
            HomeAlbums(
                albums = albums,
                title = nextTitle("Albums"),
                onAlbumClick = { album -> onNavigate(Routes.album(album.browseId)) },
            )
        }
        if (artists.isNotEmpty()) {
            HomeArtistList(
                artists = artists,
                title = nextTitle("Artists"),
                onArtistClick = { artist -> onNavigate(Routes.artist(artist.id)) },
            )
        }
        if (playlists.isNotEmpty()) {
            HomePlaylists(
                playlists = playlists,
                title = nextTitle("Playlists"),
                onPlaylistClick = { playlist -> onNavigate(Routes.playlist(playlist.id)) },
            )
        }
    }
}

/**
 * Horizontally scrolling home filter chips (parsed from the page header). Tapping a chip applies
 * its browse params, tapping the active chip returns to the default page.
 */
@Composable
private fun HomeChipRow(
    chips: List<HomePage.Chip>,
    selected: HomePage.Chip?,
    onSelected: (HomePage.Chip) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEach { chip ->
            FilterChip(
                selected = chip == selected,
                onClick = { onSelected(chip) },
                label = {
                    Text(
                        text = chip.title,
                        fontWeight = if (chip == selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}
