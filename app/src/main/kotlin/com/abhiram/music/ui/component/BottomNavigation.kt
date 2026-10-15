package com.abhiram.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.abhiram.music.ui.glass.LiquidGlassSurface
import com.abhiram.music.ui.navigation.Routes
import com.abhiram.music.ui.theme.expressiveShapes

/**
 * Floating liquid-glass pill bottom bar with the five Material 3 Expressive destinations
 * (Home, Songs, Playlists, Search, Settings — see [Routes.bottomBar]).
 *
 * The bar is meant to be overlaid at the bottom of a screen (`BoxScope.align(Alignment
 * .BottomCenter)` or a `Scaffold` `bottomBar` slot); it insets itself for the navigation bar
 * and floats above the content with a small margin. The selected destination is highlighted by
 * a bouncy morphing pill (colour + icon scale on springs) that turns static when the system
 * animator scale is 0.
 *
 * Navigation always flows through [onNavigate] so screens stay navigation-agnostic (the app's
 * navigation contract); [navController] is optional and only used as a *fallback* source of the
 * active route when [currentRoute] is `null`.
 *
 * @param navController Optional controller used only to derive the active route when
 * [currentRoute] is `null`.
 * @param currentRoute Route of the currently visible destination (compare against [Routes]);
 * `null` shows nothing selected.
 * @param onNavigate Invoked with the destination route when an item is tapped.
 * @param modifier Modifier applied to the bar root (alignment, padding…).
 */
@Composable
fun BottomNavigation(
    navController: NavHostController? = null,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberScaffoldReduceMotion()
    val activeRoute = currentRoute ?: navController?.currentBackStackEntry?.destination?.route

    LiquidGlassSurface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.expressiveShapes.pill,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomDestinations.forEach { destination ->
                BottomNavigationItem(
                    destination = destination,
                    selected = destination.route == activeRoute,
                    reduceMotion = reduceMotion,
                    onClick = { onNavigate(destination.route) },
                )
            }
        }
    }
}

/** One destination of [BottomNavigation]. */
private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/** The fixed set of top-level destinations shown by [BottomNavigation]. */
private val bottomDestinations: List<BottomDestination> = listOf(
    BottomDestination(Routes.HOME, "Home", Icons.Rounded.Home),
    BottomDestination(Routes.SONGS, "Songs", Icons.Rounded.MusicNote),
    BottomDestination(Routes.PLAYLISTS, "Playlists", Icons.Rounded.PlaylistPlay),
    BottomDestination(Routes.SEARCH, "Search", Icons.Rounded.Search),
    BottomDestination(Routes.SETTINGS, "Settings", Icons.Rounded.Settings),
)

/**
 * A single ≥48 dp destination cell: icon over a small label, wrapped in a morphing pill that
 * springs in when selected (and snaps instantly under reduced motion).
 */
@Composable
private fun RowScope.BottomNavigationItem(
    destination: BottomDestination,
    selected: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit,
) {
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = if (reduceMotion) snap() else springSpec,
        label = "navItemContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = if (reduceMotion) snap() else springSpec,
        label = "navItemContent",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = if (reduceMotion) snap() else springSpec,
        label = "navItemIconScale",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clip(MaterialTheme.expressiveShapes.pill)
            .background(container)
            .selectable(
                selected = selected,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
