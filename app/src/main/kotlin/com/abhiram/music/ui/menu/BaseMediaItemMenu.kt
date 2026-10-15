package com.abhiram.music.ui.menu

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.playback.PlayerController
import com.abhiram.music.playback.PlayerControllerProvider
import com.abhiram.music.playback.PlayerState
import com.abhiram.music.ui.component.TextFieldDialog
import com.abhiram.music.ui.theme.expressiveShapes
import com.abhiram.music.utils.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Shared foundation for every media-item menu (owner CMP-B).
 *
 * Hosts a [ModalBottomSheet] with expressive corners ([com.abhiram.music.ui.theme.expressiveShapes]),
 * a drag handle, an item header (thumbnail + title + artist/duration + optional trailing slot) and
 * a scrollable content area capped at ~70 % of the screen so long menus never fill the display.
 * Children of [content] should be [MenuEntry]s (≥ 56 dp, staggered spring-in) or the internal
 * shared-action helpers from this file.
 *
 * @param item The track the menu describes.
 * @param onDismiss Invoked when the sheet should close (scrim tap, back, action performed...).
 * @param modifier Modifier applied to the sheet.
 * @param headerTrailing Optional slot on the right of the header (like button, artwork state...).
 * @param content Menu body, rendered inside the scrollable area.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseMediaItemMenu(
    item: MediaMetadata,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    headerTrailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val stagger = remember { MenuStaggerClock() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = MaterialTheme.expressiveShapes.pill,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { MenuDragHandle() },
    ) {
        CompositionLocalProvider(LocalMenuStagger provides stagger) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                MediaItemMenuHeader(item = item, trailing = headerTrailing)
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    content()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------------------------
 *  Header
 * ---------------------------------------------------------------------------------------- */

@Composable
private fun MediaItemMenuHeader(
    item: MediaMetadata,
    trailing: (@Composable RowScope.() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.expressiveShapes.cookie)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (item.thumbnailUrl != null) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(item.artists.joinToString(", ") { it.name }.ifEmpty { "Unknown artist" })
                    if (item.duration > 0) {
                        append("  •  ")
                        append(item.duration.formatDuration())
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.let {
            Spacer(modifier = Modifier.width(8.dp))
            it()
        }
    }
}

@Composable
private fun MenuDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
    )
}

/* -------------------------------------------------------------------------------------------
 *  Shared plumbing used by every concrete menu
 * ---------------------------------------------------------------------------------------- */

/** Resolves the live [PlayerController], `null` while the playback service is not up. */
@Composable
internal fun rememberMenuPlayerController(): PlayerController? {
    val controller by PlayerControllerProvider.controller.collectAsState()
    return controller
}

/**
 * Queue [StateFlow] that is safe to collect while the controller may be absent: falls back to an
 * empty [PlayerState].
 */
internal fun queueStateFlow(controller: PlayerController?): StateFlow<PlayerState> =
    controller?.state ?: MutableStateFlow(PlayerState())

/** Opens an `ACTION_SEND` chooser for [item]; safe to call from any menu entry. */
@Composable
internal fun rememberShareAction(item: MediaMetadata): () -> Unit {
    val context = LocalContext.current
    return remember(context, item) {
        {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, item.shareLink)
                putExtra(Intent.EXTRA_TITLE, item.title)
            }
            runCatching {
                context.startActivity(Intent.createChooser(send, item.title))
            }.onFailure {
                Toast.makeText(context, "Unable to share this song", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * The menu action set shared by every concrete media-item menu: play next, add to queue, song
 * radio, like/unlike (through [com.abhiram.music.repository.MusicRepository.toggleLike]), add to
 * playlist (delegates to [onAddToPlaylist] which swaps the sheet pane), view artist/album, share
 * and show stats.
 *
 * Navigation goes through callbacks only — this package never imports a `NavController`.
 *
 * @param item The track the menu describes.
 * @param onDismiss Invoked when an action completes and the sheet should close.
 * @param onAddToPlaylist Swaps the sheet into the playlist picker pane.
 * @param modifier Modifier applied to the entries column.
 * @param showQueueActions False for menus whose item is already queued.
 * @param onArtistClick Receives the tapped [MediaMetadata.Artist] for navigation.
 * @param onAlbumClick Receives the album for navigation.
 * @param onNavigate Extra callback fired after any navigation so hosts can collapse overlays.
 * @param onSongRadio Starts a "song radio" queue seeded from this item.
 * @param onShowStats Opens the playback-stats dialog/screen for this item.
 * @param leading Optional menu-specific entries rendered before the shared ones.
 * @param trailing Optional menu-specific entries rendered after the shared ones.
 */
@Composable
internal fun SharedMediaItemActions(
    item: MediaMetadata,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    showQueueActions: Boolean = true,
    onArtistClick: ((MediaMetadata.Artist) -> Unit)? = null,
    onAlbumClick: ((MediaMetadata.Album) -> Unit)? = null,
    onNavigate: (() -> Unit)? = null,
    onSongRadio: (() -> Unit)? = null,
    onShowStats: (() -> Unit)? = null,
    leading: (@Composable ColumnScope.() -> Unit)? = null,
    trailing: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ServiceLocator.container.musicRepository }
    val share = rememberShareAction(item)

    fun navigate(action: () -> Unit) {
        action()
        onNavigate?.invoke()
        onDismiss()
    }

    Column(modifier = modifier) {
        leading?.invoke(this)

        if (showQueueActions) {
            MenuEntry(
                title = "Play next",
                icon = Icons.Rounded.PlaylistPlay,
                subtitle = "Plays after the current song",
            ) {
                navigate { PlayerControllerProvider.with(context) { it.playNext(item) } }
            }
            MenuEntry(
                title = "Add to queue",
                icon = Icons.Rounded.QueueMusic,
                subtitle = "Adds to the end of the queue",
            ) {
                navigate { PlayerControllerProvider.with(context) { it.addToQueue(item) } }
            }
        }

        onSongRadio?.let { radio ->
            MenuEntry(
                title = "Song radio",
                icon = Icons.Rounded.Radio,
                subtitle = "Endless mix around this song",
            ) {
                navigate { radio() }
            }
        }

        MenuEntry(
            title = if (item.liked) "Unlike" else "Like",
            icon = if (item.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        ) {
            onDismiss()
            scope.launch(Dispatchers.IO) { repository.toggleLike(item) }
        }

        MenuEntry(
            title = "Add to playlist",
            icon = Icons.Rounded.PlaylistAdd,
            subtitle = "Save to a local playlist",
        ) {
            onAddToPlaylist()
        }

        if (onArtistClick != null && item.artists.isNotEmpty()) {
            ArtistEntries(
                item = item,
                onArtistClick = onArtistClick,
                onNavigate = ::navigate,
            )
        }

        item.album?.let { album ->
            if (onAlbumClick != null) {
                MenuEntry(
                    title = if (item.isEpisode) "View podcast" else "View album",
                    icon = Icons.Rounded.Album,
                    subtitle = album.title,
                ) {
                    navigate { onAlbumClick(album) }
                }
            }
        }

        MenuEntry(
            title = "Share",
            icon = Icons.Rounded.Share,
        ) {
            onDismiss()
            share()
        }

        onShowStats?.let { showStats ->
            MenuEntry(
                title = "Show stats",
                icon = Icons.Rounded.Info,
                subtitle = "Format, bitrate and play counts",
            ) {
                navigate { showStats() }
            }
        }

        trailing?.invoke(this)
    }
}

/** "View artist" entry that cascades into one entry per artist when there are several. */
@Composable
private fun ArtistEntries(
    item: MediaMetadata,
    onArtistClick: (MediaMetadata.Artist) -> Unit,
    onNavigate: (() -> Unit) -> Unit,
) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    if (!expanded && item.artists.size == 1) {
        MenuEntry(
            title = "View artist",
            icon = Icons.Rounded.Person,
            subtitle = item.artists.first().name,
        ) {
            onNavigate { onArtistClick(item.artists.first()) }
        }
        return
    }
    MenuEntry(
        title = if (expanded) "Hide artists" else "View artist",
        icon = Icons.Rounded.Person,
        subtitle = item.artists.joinToString(", ") { it.name },
    ) {
        if (item.artists.size == 1) {
            onNavigate { onArtistClick(item.artists.first()) }
        } else {
            expanded = !expanded
        }
    }
    if (expanded) {
        item.artists.forEach { artist ->
            MenuEntry(
                title = artist.name,
                modifier = Modifier.padding(start = 20.dp),
            ) {
                onNavigate { onArtistClick(artist) }
            }
        }
    }
}

/**
 * Playlist picker pane: lists the local playlists from
 * [com.abhiram.music.repository.MusicRepository.localPlaylists] plus a "New playlist…" entry that
 * opens the frozen [com.abhiram.music.ui.component.TextFieldDialog]; picking either adds [item]
 * to the chosen playlist and closes the menu.
 *
 * @param item The track being added.
 * @param onDismiss Closes the whole menu once the item has been added.
 * @param onBack Returns to the actions pane.
 */
@Composable
internal fun AddToPlaylistPane(
    item: MediaMetadata,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val repository = remember { ServiceLocator.container.musicRepository }
    val playlists by repository.localPlaylists().collectAsState(initial = emptyList())
    var showNewPlaylistDialog by rememberSaveable { mutableStateOf(false) }

    if (showNewPlaylistDialog) {
        TextFieldDialog(
            title = "New playlist",
            value = "",
            onDismiss = { showNewPlaylistDialog = false },
            onConfirm = { name ->
                showNewPlaylistDialog = false
                val trimmed = name.trim()
                if (trimmed.isNotEmpty()) {
                    scope.launch(Dispatchers.IO) {
                        val playlist = repository.createLocalPlaylist(trimmed)
                        repository.addSongsToPlaylist(playlist.id, listOf(item))
                    }
                }
                onDismiss()
            },
            placeholder = "Playlist name",
            label = "Name",
        )
    }

    Column(modifier = modifier) {
        MenuEntry(
            title = "Add to playlist",
            icon = Icons.Rounded.ArrowBack,
            subtitle = "Pick a playlist or create a new one",
        ) {
            onBack()
        }
        MenuEntry(
            title = "New playlist…",
            icon = Icons.Rounded.PlaylistAdd,
        ) {
            showNewPlaylistDialog = true
        }
        if (playlists.isEmpty()) {
            Text(
                text = "No local playlists yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        } else {
            playlists.forEach { playlist ->
                MenuEntry(
                    title = playlist.name,
                    icon = Icons.Rounded.QueueMusic,
                ) {
                    scope.launch(Dispatchers.IO) {
                        repository.addSongsToPlaylist(playlist.id, listOf(item))
                    }
                    onDismiss()
                }
            }
        }
    }
}
