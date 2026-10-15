package com.abhiram.music.ui.screens.library

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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhiram.music.model.MediaMetadata
import com.abhiram.music.model.toMediaMetadata
import com.abhiram.music.playback.NoOpPlayerController
import com.abhiram.music.playback.PlayerControllerProvider
import com.abhiram.music.ui.component.MusicBars
import com.abhiram.music.ui.component.SwipeToActionBox
import com.abhiram.music.ui.component.TooltipIconButton
import com.abhiram.music.ui.component.items.LocalSongItem
import com.abhiram.music.utils.formatDuration

/**
 * Library songs tab (route `songs`).
 *
 * Filter chips (All / Liked / Recent) above a sort chip row (title / date added / duration /
 * artist + direction toggle) and a sortable list of [LocalSongItem] rows: the active track shows
 * [MusicBars], swiping a row left/right likes or unlikes it ([SwipeToActionBox]), a long press or
 * the ⋮ button opens the shared [com.abhiram.music.ui.menu.MediaItemMenu], and the floating glass
 * buttons play or shuffle everything. Shimmer, empty and error states are all handled.
 *
 * @param onNavigate Invoked with a destination route (`artist/{id}`, `album/{id}`,
 * `localPlaylist/{id}`…) for entries that leave the tab.
 * @param modifier Modifier applied to the screen root.
 * @param viewModel View model holding the Room flows and the chip state.
 */
@Composable
fun SongsScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibrarySongsViewModel = viewModel(factory = LibrarySongsViewModel.Factory),
) {
    val context = LocalContext.current

    val state by viewModel.state.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val sortDescending by viewModel.sortDescending.collectAsStateWithLifecycle()

    val controller by PlayerControllerProvider.controller.collectAsStateWithLifecycle()
    val playerState by (controller?.state ?: NoOpPlayerController.state).collectAsStateWithLifecycle()

    val error = state.error
    val songs = state.items
    val isLoading = state.isLoading
    val activeId = playerState.current?.id

    val playable = remember(songs) { songs.map { it.toMediaMetadata() } }
    val listState = rememberLazyListState()
    var menuTarget by remember { mutableStateOf<MediaMetadata?>(null) }

    /** Replaces the queue with the visible list and starts at [index]. */
    fun playFrom(index: Int, shuffle: Boolean) {
        if (playable.isEmpty()) return
        PlayerControllerProvider.playAll(context, playable, index, shuffle)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        LibraryScreenHeader(
            title = "Songs",
            count = songs.size,
            unitLabel = "songs",
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
        )

        LibraryChipRow(
            options = SongFilter.entries.map { ChipOption(it, it.label) },
            key = filter,
            onSelected = viewModel::setFilter,
        )

        LibraryChipRow(
            options = SongSortType.entries.map { ChipOption(it, it.label) },
            key = sort,
            onSelected = viewModel::setSort,
        ) {
            SortDirectionButton(
                descending = sortDescending,
                onClick = viewModel::toggleSortDescending,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                isLoading -> LibraryLoadingList(modifier = Modifier.align(Alignment.TopCenter))

                error != null -> LibraryErrorState(
                    message = error,
                    onRetry = viewModel::retry,
                    modifier = Modifier.align(Alignment.Center),
                )

                songs.isEmpty() ->
                    LibraryEmptyState(
                        text =
                            when (filter) {
                                SongFilter.LIKED -> "No liked songs yet — swipe a song to add one"
                                SongFilter.RECENT -> "Nothing played recently"
                                SongFilter.ALL -> "Your library is empty — search for music first"
                            },
                        icon = Icons.Rounded.MusicNote,
                        modifier = Modifier.align(Alignment.Center),
                    )

                else ->
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(
                            items = songs,
                            key = { _, song -> song.id },
                        ) { index, song ->
                            val isActive = song.id == activeId
                            SwipeToActionBox(
                                onAction = { viewModel.toggleLike(song) },
                                actionIcon =
                                    if (song.song.liked == true) {
                                        Icons.Rounded.Favorite
                                    } else {
                                        Icons.Rounded.FavoriteBorder
                                    },
                                actionColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                LocalSongItem(
                                    song = song,
                                    onClick = {
                                        if (isActive) {
                                            PlayerControllerProvider.with(context) { it.togglePlayPause() }
                                        } else {
                                            playFrom(index, shuffle = false)
                                        }
                                    },
                                    onLongClick = { menuTarget = song.toMediaMetadata() },
                                    leadingContent = {
                                        if (isActive) {
                                            MusicBars(isPlaying = playerState.isPlaying)
                                        } else {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    trailingContent = {
                                        Text(
                                            text = song.duration.formatDuration(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        TooltipIconButton(
                                            contentDescription = "More options",
                                            onClick = { menuTarget = song.toMediaMetadata() },
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MoreVert,
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
            }

            if (!isLoading && error == null && songs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GlassActionButton(
                        icon = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle all",
                        onClick = { playFrom(0, shuffle = true) },
                    )
                    GlassActionButton(
                        icon = Icons.Rounded.PlayArrow,
                        contentDescription = "Play all",
                        onClick = { playFrom(0, shuffle = false) },
                    )
                }
            }
        }
    }

    MediaMenuHost(
        item = menuTarget,
        onDismiss = { menuTarget = null },
        onNavigate = onNavigate,
    )
}
