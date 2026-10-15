package com.abhiram.music.ui.screens.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.abhiram.innertube.models.AlbumItem
import com.abhiram.innertube.models.SongItem
import com.abhiram.innertube.models.YTItem
import com.abhiram.innertube.pages.HomePage
import com.abhiram.music.di.ServiceLocator
import com.abhiram.music.repository.YouTubeRepository
import com.abhiram.music.repository.quickPicksSection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Everything the home screen renders, as one immutable snapshot.
 *
 * @property isLoading `true` while the *first* load runs (the screen shows a shimmer skeleton).
 * @property isRefreshing `true` while a pull-to-refresh reload runs over already visible content.
 * @property error Human readable failure of the most recent load, `null` when healthy.
 * @property quickPicks Songs of the "Quick picks" carousel, detached from [sections] so the
 * screen can render it in its own slot.
 * @property quickPicksTitle Localised title of the quick-picks carousel as reported by YouTube.
 * @property newReleases Albums of the "New releases" grid (`YouTube.newReleaseAlbums()`).
 * @property sections Every other home carousel, in server order.
 * @property chips Filter chips offered by the home page (may be empty).
 */
@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val quickPicks: List<SongItem> = emptyList(),
    val quickPicksTitle: String = "Quick picks",
    val newReleases: List<AlbumItem> = emptyList(),
    val sections: List<HomePage.Section> = emptyList(),
    val chips: List<HomePage.Chip> = emptyList(),
) {
    /** True once at least one carousel can be drawn — drives shimmer vs. content. */
    val hasContent: Boolean
        get() = quickPicks.isNotEmpty() || newReleases.isNotEmpty() || sections.isNotEmpty()
}

/**
 * State holder for the home screen (route `home`).
 *
 * Loads `YouTubeRepository.home()` (carousels + quick picks) and
 * `YouTubeRepository.newReleaseAlbums()` in parallel, applies the `hideExplicit` preference and
 * exposes the result as a single [HomeUiState] [StateFlow]. Everything resolves through
 * [ServiceLocator] — no DI framework, matching the rest of the app.
 *
 * The home repository caches its first page for five minutes, so [refresh] is what actually
 * forces new content (it is wired to pull-to-refresh and to the error retry button).
 */
class HomeViewModel : ViewModel() {

    private val youTubeRepository: YouTubeRepository = ServiceLocator.container.youTubeRepository
    private val preferences = ServiceLocator.container.preferences

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Home chip currently applied (`null` = the default, unfiltered home page). */
    private val _selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    val selectedChip: StateFlow<HomePage.Chip?> = _selectedChip.asStateFlow()

    /** Guards against overlapping loads (pull-to-refresh + retry + chip switch). */
    private var loadJob: Job? = null

    init {
        refresh()
    }

    /**
     * Reloads the home page and the new-release grid.
     *
     * The first successful load sets [HomeUiState.isLoading] (skeleton); later calls set
     * [HomeUiState.isRefreshing] (pull-to-refresh spinner) and keep the current content on
     * screen until the new page arrives. Failures surface through [HomeUiState.error] while
     * keeping previously loaded content, so a flaky refresh never blanks the screen.
     */
    fun refresh() {
        // A chip switch or a fresh pull must win over an in-flight load — cancel and restart.
        loadJob?.cancel()
        val firstLoad = !_uiState.value.hasContent
        _uiState.update {
            it.copy(
                isLoading = firstLoad,
                isRefreshing = !firstLoad,
                error = null,
            )
        }
        loadJob = viewModelScope.launch {
            val hideExplicit = runCatching { preferences.hideExplicit.first() }.getOrDefault(false)

            // Home page and new releases are independent requests — run them concurrently.
            // The selected chip re-browses `FEmusic_home` with its `params` token; new releases
            // only load alongside the default (unchipped) page so every refresh stays cheap.
            val chipParams = _selectedChip.value?.endpoint?.params
            val (homeResult, releasesResult) =
                coroutineScope {
                    val homeDeferred =
                        async { requestResult { youTubeRepository.home(params = chipParams) } }
                    val releasesDeferred =
                        async {
                            if (chipParams == null) {
                                requestResult { youTubeRepository.newReleaseAlbums() }
                            } else {
                                Result.success(emptyList<AlbumItem>())
                            }
                        }
                    homeDeferred.await() to releasesDeferred.await()
                }

            homeResult.fold(
                onSuccess = { rawPage ->
                    val page = rawPage.filterExplicit(hideExplicit)
                    val quickSection = page.quickPicksSection()
                    val quickPicks = quickSection?.items?.songs().orEmpty()
                    // Keep the chip row visible when a chipped page returns no header chips.
                    val chips = page.chips.orEmpty().ifEmpty { _uiState.value.chips }
                    _uiState.value =
                        HomeUiState(
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            quickPicks = quickPicks,
                            quickPicksTitle = quickSection?.title ?: DEFAULT_QUICK_PICKS_TITLE,
                            newReleases = releasesResult.getOrDefault(emptyList()),
                            sections = page.sections.filter { it !== quickSection },
                            chips = chips,
                        )
                },
                onFailure = { throwable ->
                    // Keep whatever we already show; only a totally empty screen is an error page.
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = throwable.message ?: DEFAULT_ERROR,
                        )
                    }
                },
            )
        }
    }

    /**
     * Applies (or clears) a home filter [chip]: tapping the active chip returns to the default
     * page, tapping another one re-browses `FEmusic_home` with that chip's params. The reload
     * keeps the current content on screen ([HomeUiState.isRefreshing]) until the new page arrives,
     * so chip switches never flash a skeleton.
     */
    fun selectChip(chip: HomePage.Chip?) {
        val target = if (chip == _selectedChip.value) null else chip
        if (target == null && _selectedChip.value == null) return
        _selectedChip.value = target
        refresh()
    }

    /**
     * Runs a repository call, converting thrown network failures into a failed [Result] while
     * still propagating coroutine cancellation.
     */
    private suspend fun <T> requestResult(block: suspend () -> Result<T>): Result<T> =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }

    /** Songs inside a mixed carousel, in server order (episodes excluded). */
    private fun List<YTItem>.songs(): List<SongItem> =
        filterIsInstance<SongItem>().filterNot { it.isEpisode }

    companion object {
        /** Fallback heading when the response carries no explicit quick-picks label. */
        private const val DEFAULT_QUICK_PICKS_TITLE = "Quick picks"

        /** Fallback failure text for responses without a message. */
        private const val DEFAULT_ERROR = "Something went wrong"

        /** Plain factory — the view model resolves everything through [ServiceLocator]. */
        val Factory: ViewModelProvider.Factory =
            viewModelFactory { initializer { HomeViewModel() } }
    }
}
