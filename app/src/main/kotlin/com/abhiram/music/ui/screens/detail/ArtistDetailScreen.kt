package com.abhiram.music.ui.screens.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.ui.navigation.Routes
import com.abhiram.music.ui.component.screen.ArtistOverview
import com.abhiram.music.ui.component.screen.ArtistScreen
import com.abhiram.music.ui.menu.MediaItemMenu

/** Index of the "Overview" tab inside the artist screen's pill strip. */
private const val TAB_OVERVIEW = 0

/** Index of the "Songs" tab inside the artist screen's pill strip. */
private const val TAB_SONGS = 1

/**
 * Artist detail screen — route `artist/{id}`.
 *
 * Assembles the CMP-D [ArtistScreen] (banner + pill tab strip + action row) with its
 * [ArtistOverview] tab body: an online-first [com.abhiram.music.ui.screens.detail.ArtistDetailViewModel]
 * feeds the InnerTube page and falls back to the Room cache when the page cannot be fetched, so
 * the screen stays useful offline. Long-pressed rows open the shared [MediaItemMenu]; the
 * header's ⋮ opens the screen overflow (share). Play-all, shuffle and row playback are handled
 * by the CMP-D components through the frozen `PlayerControllerProvider`.
 *
 * @param id artist browse/channel id.
 * @param onNavigate navigates to a deeper route (`artist/{id}`, `album/{id}`, `player/stats`).
 * @param onBack pops back to the previous destination.
 * @param modifier sizing modifier.
 */
@Composable
fun ArtistDetailScreen(
    id: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ArtistDetailViewModel = viewModel(factory = ArtistDetailViewModel.factory(id))
    val state by viewModel.state.collectAsState()

    var selectedTab by rememberSaveable(id) { mutableStateOf(TAB_OVERVIEW) }
    var menuSong by remember { mutableStateOf<MediaMetadata?>(null) }
    var overflowOpen by remember { mutableStateOf(false) }

    val share = rememberShareAction(
        link = "https://music.youtube.com/channel/$id",
        title = state.online?.artist?.title?.ifBlank { "Artist" } ?: "Artist",
    )
    val openMenu: (MediaMetadata) -> Unit = { song -> menuSong = song }

    // The tab strip only exists while the online page can fill both tabs; the local fallback
    // renders its cached songs directly without tabs.
    val online = state.online
    val tabs = if (online != null) listOf("Overview", "Songs") else emptyList()
    val activeTab = selectedTab.coerceIn(tabs.indices)

    Box(modifier = modifier.fillMaxSize()) {
        DetailScaffold(
            onBack = onBack,
            refreshing = state.isLoading && online != null,
            onRefresh = viewModel::refresh,
        ) {
            if (online != null) {
                ArtistScreen(
                    artist = online.artist,
                    error = state.error,
                    onRetry = viewModel::refresh,
                    tabs = tabs,
                    selectedTab = activeTab,
                    onTabSelected = { selectedTab = it },
                    songs = if (activeTab == TAB_SONGS) state.songs else emptyList(),
                    onMenu = { overflowOpen = true },
                    onSongLongClick = openMenu,
                    onSongMenu = openMenu,
                    content = {
                        if (activeTab == TAB_OVERVIEW) {
                            ArtistOverview(
                                biography = online.description,
                                topSongs = state.topSongs,
                                relatedArtists = state.relatedArtists,
                                shareLink = online.artist.shareLink,
                                subscriberCount = online.subscriberCountText,
                                monthlyListeners = online.monthlyListenerCount,
                                onSongLongClick = openMenu,
                                onArtistClick = { artist ->
                                    artist.id?.let { artistId -> onNavigate(Routes.artist(artistId)) }
                                },
                                onShowAllTopSongs = { selectedTab = TAB_SONGS },
                                onShare = share,
                            )
                        }
                    },
                )
            } else {
                // Local-only (or still loading): the Room artist when present, otherwise a blank
                // placeholder item so the shared screen can render its shimmer state.
                val local = state.local
                if (local != null) {
                    ArtistScreen(
                        artist = local,
                        songs = state.localSongs,
                        onSongLongClick = openMenu,
                        onSongMenu = openMenu,
                    )
                } else {
                    ArtistScreen(
                        artist = placeholderArtistItem(id),
                        isLoading = state.isLoading,
                        onSongLongClick = openMenu,
                        onSongMenu = openMenu,
                    )
                }
            }
        }

        DetailOverflowMenu(
            expanded = overflowOpen,
            onDismissRequest = { overflowOpen = false },
            items = listOf(DetailOverflowItem(label = "Share", onClick = share)),
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }

    MediaItemMenu(
        item = menuSong ?: return,
        onDismiss = { menuSong = null },
        onArtistClick = { artist -> artist.id?.let { artistId -> onNavigate(Routes.artist(artistId)) } },
        onAlbumClick = { album -> onNavigate(Routes.album(album.id)) },
        onShowStats = { onNavigate(Routes.PLAYER_STATS) },
    )
}
