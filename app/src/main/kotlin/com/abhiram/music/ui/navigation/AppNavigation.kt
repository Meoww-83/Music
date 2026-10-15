package com.abhiram.music.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.abhiram.music.playback.PlayerState
import com.abhiram.music.playback.RepeatMode
import com.abhiram.music.ui.component.BottomNavigation
import com.abhiram.music.ui.player.Lyrics
import com.abhiram.music.ui.player.MiniPlayer
import com.abhiram.music.ui.player.Player
import com.abhiram.music.ui.player.Queue
import com.abhiram.music.ui.player.SleepTimer
import com.abhiram.music.ui.player.StatsForNerds
import com.abhiram.music.ui.player.rememberPlayerController
import com.abhiram.music.ui.screens.detail.AlbumDetailScreen
import com.abhiram.music.ui.screens.detail.ArtistDetailScreen
import com.abhiram.music.ui.screens.detail.BuiltInPlaylistDetailScreen
import com.abhiram.music.ui.screens.detail.LocalPlaylistDetailScreen
import com.abhiram.music.ui.screens.detail.PlaylistDetailScreen
import com.abhiram.music.ui.screens.home.HomeScreen
import com.abhiram.music.ui.screens.library.AlbumsScreen
import com.abhiram.music.ui.screens.library.ArtistsScreen
import com.abhiram.music.ui.screens.library.PlaylistsScreen
import com.abhiram.music.ui.screens.library.SongsScreen
import com.abhiram.music.ui.screens.search.SearchScreen
import com.abhiram.music.ui.screens.settings.About
import com.abhiram.music.ui.screens.settings.CacheSettings
import com.abhiram.music.ui.screens.settings.DatabaseSettings
import com.abhiram.music.ui.screens.settings.GeneralSettings
import com.abhiram.music.ui.screens.settings.GestureSettings
import com.abhiram.music.ui.screens.settings.OtherSettings
import com.abhiram.music.ui.screens.settings.PlayerSettings
import com.abhiram.music.ui.screens.settings.SettingsScreen

/**
 * The app-wide navigation host: [AppNavigationGraph] plus the two floating chrome layers that
 * overlay it — the collapsed [MiniPlayer] and the liquid-glass [BottomNavigation] pill.
 *
 * Both layers live in the Scaffold `bottomBar` slot so the graph is automatically inset by their
 * measured height: content never hides behind them, while the panes themselves keep floating over
 * the backdrop (they sample it through [com.abhiram.music.ui.glass.GlassBackdrop], installed once
 * in [com.abhiram.music.MainActivity]).
 *
 * The Scaffold content window insets are zeroed because every screen already applies its own
 * status/navigation-bar padding — the host must not double-inset.
 *
 * Screens never touch the [NavHostController]; they receive `onNavigate(route: String)` and
 * `onBack: () -> Unit` callbacks so they stay previewable and independent of navigation.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val navigate: (String) -> Unit = { route -> navController.navigateTo(route) }

    val controller = rememberPlayerController()
    val playerState by controller.state.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // The bar only hosts the five main sections; every deeper screen gets the full width.
    val showBottomBar = currentRoute in Routes.bottomBar
    // The collapsed player follows the queue everywhere except the full-screen player itself.
    val showMiniPlayer = playerState.current != null && currentRoute != Routes.PLAYER

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = {
            Column {
                if (showMiniPlayer) {
                    MiniPlayer(
                        playerState = playerState,
                        onOpen = { navigate(Routes.PLAYER) },
                        onPlayPause = controller::togglePlayPause,
                        onNext = controller::seekToNext,
                        onLike = controller::toggleLike,
                        // The bar below already absorbs the navigation-bar inset when present.
                        modifier = if (showBottomBar) Modifier else Modifier.navigationBarsPadding(),
                    )
                }
                if (showBottomBar) {
                    BottomNavigation(
                        currentRoute = currentRoute,
                        onNavigate = navigate,
                    )
                }
            }
        },
    ) { innerPadding ->
        AppNavigationGraph(
            navController = navController,
            navigate = navigate,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        )
    }
}

/**
 * The route table: every destination of [Routes] bound to the screen composable that implements
 * it. Split out from [AppNavigation] so the floating chrome can wrap it without touching the graph.
 */
@Composable
private fun AppNavigationGraph(
    navController: NavHostController,
    navigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val back: () -> Unit = { navController.popBackStackCompat() }
    // Resolved again inside the graph so the player destination below is self-contained
    // (the host above keeps its own copy for the mini player chrome).
    val controller = rememberPlayerController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = { slideInHorizontally(springMedium(), initialOffsetX = { it / 8 }) },
        exitTransition = { slideOutHorizontally(springMedium(), targetOffsetX = { -it / 8 }) },
        popEnterTransition = { slideInHorizontally(springMedium(), initialOffsetX = { -it / 8 }) },
        popExitTransition = { slideOutHorizontally(springMedium(), targetOffsetX = { it / 8 }) },
    ) {
        /* --------------------------------------------------------------------- root */

        composable(Routes.HOME) { HomeScreen(onNavigate = navigate) }
        composable(Routes.SONGS) { SongsScreen(onNavigate = navigate) }
        composable(Routes.ARTISTS) { ArtistsScreen(onNavigate = navigate) }
        composable(Routes.ALBUMS) { AlbumsScreen(onNavigate = navigate) }
        composable(Routes.PLAYLISTS) { PlaylistsScreen(onNavigate = navigate) }
        composable(Routes.SEARCH) { SearchScreen(onBack = back, onNavigate = navigate) }
        composable(Routes.SETTINGS) { SettingsScreen(onNavigate = navigate, onBack = back) }

        /* ------------------------------------------------------------------ details */

        composable(
            route = Routes.ARTIST,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            ArtistDetailScreen(
                id = entry.arguments?.getString("id").orEmpty(),
                onNavigate = navigate,
                onBack = back,
            )
        }
        composable(
            route = Routes.ALBUM,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            AlbumDetailScreen(
                id = entry.arguments?.getString("id").orEmpty(),
                onNavigate = navigate,
                onBack = back,
            )
        }
        composable(
            route = Routes.PLAYLIST,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            PlaylistDetailScreen(
                id = entry.arguments?.getString("id").orEmpty(),
                onNavigate = navigate,
                onBack = back,
            )
        }
        composable(
            route = Routes.LOCAL_PLAYLIST,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            LocalPlaylistDetailScreen(
                id = entry.arguments?.getString("id").orEmpty(),
                onNavigate = navigate,
                onBack = back,
            )
        }
        composable(
            route = Routes.BUILT_IN_PLAYLIST,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            BuiltInPlaylistDetailScreen(
                id = entry.arguments?.getString("id").orEmpty(),
                onNavigate = navigate,
                onBack = back,
            )
        }

        /* -------------------------------------------------------- settings sub-pages */

        composable(Routes.SETTINGS_GENERAL) { GeneralSettings(onBack = back) }
        composable(Routes.SETTINGS_PLAYER) { PlayerSettings(onBack = back) }
        composable(Routes.SETTINGS_GESTURE) { GestureSettings(onBack = back) }
        composable(Routes.SETTINGS_CACHE) { CacheSettings(onBack = back) }
        composable(Routes.SETTINGS_DATABASE) { DatabaseSettings(onBack = back) }
        composable(Routes.SETTINGS_OTHER) { OtherSettings(onBack = back) }
        composable(Routes.SETTINGS_ABOUT) { About(onBack = back) }

        /* ------------------------------------------------------------- player routes */

        composable(Routes.PLAYER) {
            val state by controller.state.collectAsStateWithLifecycle()

            Player(
                playerState = state,
                onClose = back,
                onPlayPause = controller::togglePlayPause,
                onNext = controller::seekToNext,
                onPrevious = controller::seekToPrevious,
                onLike = controller::toggleLike,
                onShuffle = controller::toggleShuffle,
                onRepeat = { controller.setRepeatMode(state.repeatMode.next()) },
                onSeek = controller::seekTo,
                onArtistClick = { artist -> artist.id?.let { id -> navigate(Routes.artist(id)) } },
                onLyricsClick = { navigate(Routes.PLAYER_LYRICS) },
                onQueueClick = { navigate(Routes.PLAYER_QUEUE) },
                onSleepTimerClick = { navigate(Routes.PLAYER_SLEEP_TIMER) },
                onStatsClick = { navigate(Routes.PLAYER_STATS) },
                onNavigate = navigate,
            )
        }

        composable(Routes.PLAYER_LYRICS) {
            PlayerPanel(title = "Lyrics", onBack = back) { state -> Lyrics(playerState = state) }
        }
        composable(Routes.PLAYER_QUEUE) {
            PlayerPanel(title = "Queue", onBack = back) { state -> Queue(playerState = state) }
        }
        composable(Routes.PLAYER_SLEEP_TIMER) {
            PlayerPanel(title = "Sleep timer", onBack = back) { state ->
                SleepTimer(playerState = state, onDismiss = back)
            }
        }
        composable(Routes.PLAYER_STATS) {
            PlayerPanel(title = "Stats for nerds", onBack = back) { state ->
                StatsForNerds(playerState = state, onDismiss = back)
            }
        }
    }
}

/**
 * Hosts one of the player's secondary pages (lyrics / queue / sleep timer / stats) against the
 * live [PlayerController] state, so every destination above stays a one-liner.
 */
@Composable
private fun PlayerPanel(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (state: PlayerState) -> Unit,
) {
    val controller = rememberPlayerController()
    val state by controller.state.collectAsStateWithLifecycle()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                content(state)
            }
        }
    }
}

/** The medium-bouncy spring used for push/pop transitions between destinations. */
private fun springMedium(): androidx.compose.animation.core.SpringSpec<IntOffset> = spring(
    dampingRatio = Spring.DampingRatioMediumLowBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/** OFF → ALL → ONE → OFF, the order used by the player's repeat button. */
private fun RepeatMode.next(): RepeatMode = when (this) {
    RepeatMode.OFF -> RepeatMode.ALL
    RepeatMode.ALL -> RepeatMode.ONE
    RepeatMode.ONE -> RepeatMode.OFF
}

/**
 * Navigates to [route], avoiding duplicate copies of the same destination on the stack
 * (double taps on a bottom-navigation item must not stack two copies of the screen).
 */
private fun NavHostController.navigateTo(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) { launchSingleTop = true }
}

/** Pops the back stack, returning false when nothing could be popped (root screen). */
private fun NavHostController.popBackStackCompat(): Boolean =
    runCatching { popBackStack() }.getOrDefault(false)
