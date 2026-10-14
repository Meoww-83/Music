# MUSIC APP — MASTER CONTRACT (read before writing any code)

Project root: `/root/android`  (target dir, all source lives here)
App package / namespace / applicationId: **`com.abhiram.music`**
Innertube module namespace/package: **`com.abhiram.innertube`** (already copied at
`/root/android/innertube`, source at `innertube/src/main/kotlin/com/abhiram/innertube/`).

## 0. HARD RULES (violating these fails the whole job)

1. **NEVER compile / build / run gradle / javac / kotlinc on this machine.** Source files only.
2. **NEVER use `/tmp`.** All source lives under `/root/android`. Scratch/reference material is in
   `/root/ref` (reference clone of Metrolist, material3 AAR classes, etc.).
3. **Do not `git add/commit/push`.** The user pushes manually.
4. **Own only your files.** The file-ownership table below is authoritative. Never edit a file you
   do not own; never create files in a package you do not own.
5. **No cross-agent dependencies on unfinished work**: only use APIs listed in this contract (or
   APIs you own). If you need something outside your ownership, use exactly the signature given
   here — it *will* exist when integration happens.
6. Every Kotlin file starts with `package com.abhiram.music....` (or `com.abhiram.innertube....`
   inside the innertube module only).
7. Kotlin/Compose style: explicit types on public API, KDoc on public composables, no `TODO()`,
   no placeholder stubs that throw. Every file must be complete, self-consistent code.
8. Min SDK 26 (Android 8), target/latest SDK 37 compile, Material 3 **Expressive** design language
   + **Liquid Glass** floating layers (glass = API 33+, graceful fallback below).

## 1. REFERENCE MATERIAL

* `/root/ref/Metrolist` — full GPL-3 YouTube Music client (same innertube lineage). Use it for
  *patterns*: how requests are made, how Room entities/DAOs look, how a Media3 service is built.
  Adapt, do not blindly copy (its package is `com.metrolist.music`, it has features we don't want:
  cast, discord, listentogether, recognition, protobuf, hilt).
* `/root/ref/Metrolist/gradle/libs.versions.toml` — our catalog is a subset of this.
* Innertube API (READ THE SOURCES): `/root/android/innertube/src/main/kotlin/com/abhiram/innertube/`
  * `YouTube` object (`YouTube.kt`) is the single entry point: `search`, `searchSummary`,
    `searchSuggestions`, `album`, `albumSongs`, `artist`, `artistItems`, `playlist`,
    `playlistContinuation`, `home`, `explore`, `newReleaseAlbums`, `moodAndGenres`,
    `getChartsPage`, `lyrics`, `related`, `next`, `queue`, `player`, `visitorData`,
    `addSongToLibrary`, `removeSongFromLibrary`, `toggleSongLibrary`, `likeVideo`, `likePlaylist`,
    `feedback`, `createPlaylist`, `deletePlaylist`, `renamePlaylist`, `addToPlaylist`,
    `removeFromPlaylist`, `getMediaInfo`, `transcript`, `accountInfo`.
  * Models: `models/YTItem.kt` (`SongItem`, `AlbumItem`, `ArtistItem`, `PlaylistItem`,
    `PodcastItem`, `EpisodeItem`, `Artist`, `Album`), `models/YouTubeClient.kt`,
    `models/response/PlayerResponse.kt`, `pages/*Page.kt`.
  * All calls are `suspend` and return `kotlin.Result<T>` (except `player` which returns a Ktor
    `HttpResponse` — check `Player.kt`/`YouTube.player` before using it).

## 2. FILE OWNERSHIP TABLE (each row = one agent's exclusive area)

| ID | Package(s) you own (under `app/src/main/kotlin/`) | Extra files |
|----|---------------------------------------------------|-------------|
| DB | `com.abhiram.music.database.**` | — |
| SVC | `com.abhiram.music.service.**`, `com.abhiram.music.utils.RingBuffer.kt`, `com.abhiram.music.utils.YoutubeRadio.kt` | — |
| CTL | `com.abhiram.music.playback.**` (except `PlayerController.kt` which is frozen), `com.abhiram.music.di.**`, `com.abhiram.music.repository.**`, `com.abhiram.music.constants.**` | — |
| THM | `com.abhiram.music.ui.theme.**`, `com.abhiram.music.ui.glass.**` | — |
| CMP-A | `com.abhiram.music.ui.component` *(scaffolds, dialogs, indicators, misc — list in §6)* | — |
| CMP-B | `com.abhiram.music.ui.menu.**` | — |
| CMP-C | `com.abhiram.music.ui.component.items.**` | — |
| CMP-D | `com.abhiram.music.ui.component.screen.**` (album/artist/playlist/built-in/local/home components, §6) | — |
| CMP-E | `com.abhiram.music.ui.player.**` | — |
| CMP-F | `com.abhiram.music.ui.screens.search.**`, `com.abhiram.music.ui.screens.settings.**` | — |
| SCR-L | `com.abhiram.music.ui.screens.library.**`, `com.abhiram.music.ui.screens.home.**` | — |
| SCR-D | `com.abhiram.music.ui.screens.detail.**` (artist/album/playlist/local/built-in detail assembly + viewmodels) | — |
| INFRA | — | `.github/workflows/*.yml`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/**` (base resources), `README.md`, `app/lint.xml` |
| LEAD | `com.abhiram.music` (root: `MainActivity.kt`, `App.kt`), `com.abhiram.music.ui.navigation.**`, `model/**`, `playback/PlayerController.kt`, `ui/component` root files listed as frozen in §6, `TODO.md`, gradle files | |

If a file you need does not exist yet and is not yours — it is being written by its owner in
parallel. Code against the contract; do not create it yourself.

## 3. DATA LAYER CONTRACT

### 3.1 `model/MediaMetadata.kt` (FROZEN — already written by LEAD)
`MediaMetadata(id, title, artists: List<MediaMetadata.Artist>, duration: Int, thumbnailUrl?,
album?, setVideoId?, musicVideoType?, explicit, liked, likedDate, inLibrary, libraryAddToken,
libraryRemoveToken, suggestedBy, isEpisode, uploadEntityId)` + converters
`SongItem.toMediaMetadata()`, `EpisodeItem.toMediaMetadata()`, `Song.toMediaMetadata()`.

### 3.2 `playback/PlayerController.kt` (FROZEN — already written by LEAD)
`PlayerController`, `PlayerState`, `RepeatMode`, `PlaybackError`, `PlayerControllerProvider`,
`NoOpPlayerController`.

> **Play entry point rule:** never call `PlayerControllerProvider.controller?.play…` directly for
> the *first* play — the service may not be running yet. Use
> `PlayerControllerProvider.with(context) { it.playAll(items, index) }` or
> `PlayerControllerProvider.playAll(context, items, index, shuffle)` (cold-starts the service and
> replays the action once it installs its controller).

### 3.3 Room — owner DB, package `com.abhiram.music.database`

`MusicDatabase` (abstract class, `@Database(version = 1, exportSchema = true)`,
entities = all below, `@ConstructedBy?` no — use standard `Room.databaseBuilder`).

Entities (`database/entities/*.kt`), all with `@Entity`:

```kotlin
@Entity SongEntity(id: String PK, title: String, duration: Int, thumbnailUrl: String?,
  albumId: String?, albumName: String?, explicit: Boolean, liked: Boolean?, likedDate: LocalDateTime?,
  inLibrary: LocalDateTime?, libraryAddToken: String?, libraryRemoveToken: String?,
  isVideo: Boolean, isEpisode: Boolean, uploadEntityId: String?, totalPlayTimeMs: Long = 0)
@Entity ArtistEntity(id: String PK, name: String, thumbnailUrl: String?, subscriberCountText: String?)
@Entity AlbumEntity(id: String PK, title: String, thumbnailUrl: String?, year: Int?, songCountText: String?)
@Entity SongArtistMap(songId: String, artistId: String, sortIndex: Int, PK(songId, artistId))
@Entity SongAlbumMap(songId: String, albumId: String, sortIndex: Int, PK(songId, albumId))
@Entity AlbumArtistMap(albumId: String, artistId: String, sortIndex: Int, PK(albumId, artistId))
@Entity PlaylistEntity(id: String PK, name: String, thumbnailUrl: String?, description: String?,
  browseId: String?, /* null => local playlist */ createdAt: LocalDateTime, lastUpdatedAt: LocalDateTime)
@Entity PlaylistSongMap(playlistId: String, songId: String, id: Int /*order*/, setVideoId: String?,
  addedAt: LocalDateTime, PK(playlistId, songId))
@Entity SearchHistory(id: Long? auto PK, query: String, createdAt: LocalDateTime)
@Entity Event(id: Long? auto PK, songId: String, timestamp: Long, playDuration: Long)
@Entity FormatEntity(songId: String PK, itag: Int?, mimeType: String?, bitrate: Long?,
  loudnessDb: Double?, contentLength: Long?, lastModified: String?)
@Entity QueuedMediaItem(id: Int /*0..n order PK*/, mediaId: String, positionMs: Long?)
@Entity LyricsEntity(songId: String PK, plainLyrics: String?, syncedLyrics: String?, source: String?)
@Entity PlayCountEntity(songId: String PK, playCount: Int)
```

Relations (same package): `Song` (`@Embedded song`, `@Relation` artists → `SongArtistMap→ArtistEntity`,
album → `AlbumEntity`, exposing `orderedArtists`), `AlbumWithSongs`, `PlaylistWithSongs`,
`SongWithStats`, `EventWithSong`.

Converters (`database/Converters.kt`): `LocalDateTime ↔ Long` (epoch millis).

**DAO — `database/DatabaseDao.kt` (single `@Dao abstract class DatabaseDao`), MUST expose at least:**

```kotlin
// songs
suspend fun insert(song: SongEntity); suspend fun insert(songs: List<SongEntity>)
suspend fun insert(artists: List<ArtistEntity>); suspend fun insert(albums: List<AlbumEntity>)
suspend fun insert(map: SongArtistMap); suspend fun insert(maps: List<SongArtistMap>)
suspend fun insert(map: SongAlbumMap); suspend fun insert(maps: List<SongAlbumMap>)
suspend fun insert(map: AlbumArtistMap); suspend fun insert(maps: List<AlbumArtistMap>)
fun songById(id: String): Flow<Song?>
suspend fun songEntityById(id: String): SongEntity?
fun songs(): Flow<List<Song>>                                  // ordered by title
fun likedSongs(): Flow<List<Song>>
suspend fun songsByIds(ids: List<String>): List<Song>
suspend fun deleteSong(songId: String)
suspend fun updateLike(id: String, liked: Boolean?, likedDate: LocalDateTime?)
suspend fun addSongToLibrary(id: String, at: LocalDateTime = now())
suspend fun removeSongFromLibrary(id: String)
suspend fun increaseTotalPlayTime(id: String, ms: Long)
fun artists(): Flow<List<Artist>>; fun artistById(id: String): Flow<Artist?>
suspend fun artistEntityById(id: String): ArtistEntity?
fun albums(): Flow<List<Album>>; fun albumById(id: String): Flow<Album?>
suspend fun albumEntityById(id: String): AlbumEntity?
fun albumSongs(albumId: String): Flow<List<Song>>
fun artistSongs(artistId: String): Flow<List<Song>>
suspend fun songsOfAlbum(albumId: String): List<Song>
suspend fun songsOfArtist(artistId: String): List<Song>
// playlists (local + cache of remote)
fun playlists(): Flow<List<Playlist>>
fun playlistById(id: String): Flow<Playlist?>
suspend fun playlistEntityById(id: String): PlaylistEntity?
fun playlistSongs(playlistId: String): Flow<List<Song>>        // ordered by PlaylistSongMap.id
suspend fun playlistSongsList(playlistId: String): List<Song>
suspend fun insert(playlist: PlaylistEntity)
suspend fun deletePlaylist(playlistId: String)
suspend fun renamePlaylist(playlistId: String, name: String)
suspend fun updatePlaylist(playlist: PlaylistEntity)
suspend fun insert(map: PlaylistSongMap); suspend fun insert(maps: List<PlaylistSongMap>)
suspend fun deletePlaylistSong(playlistId: String, songId: String)
suspend fun clearPlaylist(playlistId: String)
suspend fun playlistSongMap(playlistId: String, songId: String): PlaylistSongMap?
suspend fun nextPlaylistSongOrder(playlistId: String): Int
// search history
fun searchHistory(): Flow<List<SearchHistory>>
suspend fun insert(searchHistory: SearchHistory)
suspend fun deleteSearchHistory(query: String); suspend fun clearSearchHistory()
// events / stats
suspend fun insert(event: Event)
fun events(): Flow<List<EventWithSong>>
fun totalPlayTime(): Flow<Long?>
// formats (stream metadata)
suspend fun insert(format: FormatEntity); suspend fun formatOf(songId: String): FormatEntity?
// queue persistence
suspend fun saveQueue(items: List<QueuedMediaItem>)
suspend fun getQueue(): List<QueuedMediaItem>
suspend fun clearQueue()
// lyrics
suspend fun insert(lyrics: LyricsEntity); suspend fun lyricsOf(songId: String): LyricsEntity?
// play counts
suspend fun incrementPlayCount(songId: String)
fun playCountOf(songId: String): Flow<Int>
```

(Implementation freedom: use `@Transaction` open functions, `@Query` with joins, paging is NOT
required. All methods that only read may be `Flow` or `suspend`.)

### 3.4 Repositories — owner CTL, package `com.abhiram.music.repository`

```kotlin
class MusicRepository @Inject-free constructor(private val dao: DatabaseDao)   // plain ctor, no Hilt
suspend fun toggleLike(item: MediaMetadata): Boolean     // returns new liked state
fun likedSongs(): Flow<List<Song>>
suspend fun addToLibrary(item: MediaMetadata)
fun localPlaylists(): Flow<List<Playlist>>
suspend fun createLocalPlaylist(name: String): PlaylistEntity
suspend fun addSongsToPlaylist(playlistId: String, items: List<MediaMetadata>)
suspend fun removeSongFromPlaylist(playlistId: String, songId: String)
fun songsInPlaylist(playlistId: String): Flow<List<Song>>
suspend fun addToRemotePlaylist(browseId: String, item: MediaMetadata)   // YouTube.addToPlaylist
suspend fun deleteRemotePlaylist(id: String)
fun searchHistory(): Flow<List<SearchHistory>>
suspend fun pushSearchHistory(query: String)
suspend fun lyrics(videoId: String): LyricsEntity?       // cache-first, then YouTube.lyrics()
```

```kotlin
class YouTubeRepository   // wraps the `YouTube` object, holds coroutine contexts
suspend fun search(query: String, filter: SearchFilter?, continuation: String?): Result<ItemsPage-like>
suspend fun suggestions(query: String): Result<SearchSuggestions>
suspend fun artist(id: String): Result<ArtistPage>
suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage>
suspend fun album(browseId: String): Result<AlbumPage>
suspend fun albumSongs(playlistId: String, continuation: String?): Result<...>
suspend fun playlist(id: String): Result<PlaylistPage>
suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage>
suspend fun home(): Result<HomePage>
suspend fun related(browseId: BrowseEndpoint): Result<RelatedPage>
suspend fun queue(videoIds: List<String>?, playlistId: String?): Result<...>
```
(Read the innertube `pages/*` and `YouTube.kt` signatures first — return the real types, do not
invent parallel models. This contract only fixes the *names*.)

### 3.5 DI + preferences — owner CTL, package `com.abhiram.music.di` / `.constants`

No Hilt/Koin. Manual service locator:
```kotlin
class AppContainer(context: Context) {
    val database: MusicDatabase
    val dao: DatabaseDao
    val musicRepository: MusicRepository
    val youTubeRepository: YouTubeRepository
    val preferences: AppPreferences
}
object ServiceLocator { fun init(context: Context); val container: AppContainer }
```
`constants/AppPreferences.kt` — typed wrapper over `DataStore<Preferences>`; **property names are
frozen** because settings screens and the service both use them:
```kotlin
class AppPreferences(private val ds: DataStore<Preferences>) {
    val darkMode: Flow<DarkMode>            // enum DarkMode { SYSTEM, LIGHT, DARK }
    val dynamicColor: Flow<Boolean>;   fun setDynamicColor(Boolean)
    val pureBlack: Flow<Boolean>;      fun setPureBlack(Boolean)
    val skipSilence: Flow<Boolean>;    fun setSkipSilence(Boolean)
    val normalization: Flow<Boolean>;  fun setNormalization(Boolean)
    val persistentQueue: Flow<Boolean>; fun setPersistentQueue(Boolean)
    val hideExplicit: Flow<Boolean>;   fun setHideExplicit(Boolean)
    val squigglyProgress: Flow<Boolean>; fun setSquigglyProgress(Boolean)
    val stopAtEnd: Flow<Boolean>;      fun setStopAtEnd(Boolean)
    val exoPlayerDiskCacheMaxSize: Flow<Long>  // bytes; -1L = unlimited, default 2 GiB
    fun setExoPlayerDiskCacheMaxSize(Long)
    val defaultSleepTimerMinutes: Flow<Int>; fun setDefaultSleepTimerMinutes(Int)
    val sleepTimerRemainingMs: Flow<Long?>;  fun setSleepTimerRemainingMs(Long?)
    val lastUpdateCheckSkippedVersion: Flow<String?>; fun setLastUpdateCheckSkippedVersion(String?)
    val searchHistoryEnabled: Flow<Boolean>; fun setSearchHistoryEnabled(Boolean)
    suspend fun setDarkMode(DarkMode)
}
enum class DarkMode { SYSTEM, LIGHT, DARK }
```
Access from composables via `remember { ServiceLocator.container.preferences }` (CTL may add a
convenience `rememberSettings()` in `di/Settings.kt`).

## 4. PLAYBACK CONTRACT

* Owner SVC: `com.abhiram.music.service.PlayerService` — a `MediaSessionService` (media3) that
  * creates `SimpleCache` in `cacheDir/exoplayer` with `StandaloneDatabaseProvider`, LRU evictor
    sized by the `exoPlayerDiskCacheMaxSize` pref (default 2 GiB), `NoOpCacheEvictor` when unlimited;
  * builds ExoPlayer with `DefaultMediaSourceFactory(createDataSourceFactory())`;
  * `createDataSourceFactory()` = `ResolvingDataSource(CacheDataSource.Factory(...))`;
  * `ResolvingDataSource` logic (spec §7): key = videoId; skip resolution for cached 512 KiB chunks;
    2-entry `RingBuffer` of resolved URIs; `YouTube.player(videoId, playlistId)` on
    `Dispatchers.IO` via `runBlocking`; validate `videoDetails.videoId`; on `OK` pick
    `highestQualityFormat`, persist `FormatEntity`, resolve URL, restrict range to 512 KiB;
    on `UNPLAYABLE`/`LOGIN_REQUIRED` throw the matching playback exception; otherwise generic error;
  * `DefaultHttpDataSource` upstream (16 s connect / 8 s read, Firefox-like UA);
  * audio attributes `USAGE_MEDIA`/`CONTENT_TYPE_MUSIC`, `setAudioBecomingNoisy(true)`,
    wake mode `WAKE_MODE_LOCAL`, repeat mode, skip-silence, `PlaybackStatsListener` (>5 s ⇒
    increase total play time, >30 s ⇒ insert `Event`), queue persistence via `QueuedMediaItem`,
    notification channel + MediaSession activity PendingIntent to `MainActivity`;
  * exposes its controller through `PlayerControllerProvider.install(...)` (§3.2) and clears it in
    `onDestroy`.
* Owner SVC also writes `utils/RingBuffer.kt` (fixed size 2, `(videoId -> url)` LRU-ish) and
  `utils/YoutubeRadio.kt` (continuation-driven queue extension: `process()` returns
  `List<MediaMetadata>` and re-arms its continuation token; when ≤3 items remain after the current
  one, the service appends radio items).
* **SVC implements `service/PlayerControllerImpl.kt`** (implements the frozen `PlayerController`),
  installs it through `PlayerControllerProvider.install(...)` when the service starts and clears it
  in `onDestroy`. It drives ExoPlayer directly (queue = ExoPlayer timeline).
* Owner CTL provides the utilities the service consumes — **these signatures are frozen**:
```kotlin
// package com.abhiram.music.playback  (CTL)
class SleepTimer(private val onExpire: suspend () -> Unit) {   // ticks every 500 ms
    val remainingMs: StateFlow<Long?>      // null = cancelled/finished
    val progress: StateFlow<Float?>        // 0f..1f remaining, null when off
    fun start(durationMillis: Long); fun cancel()
}
class PlaybackStatsTracker(private val dao: DatabaseDao) {
    // applies the spec rules: >5 s of playback => increase total play time, >30 s => insert Event
    fun onMediaItemTransition(songId: String?)
    fun onPositionChanged(positionMs: Long)
    fun reset()
}
```

## 5. THEME + LIQUID GLASS CONTRACT — owner THM

`com.abhiram.music.ui.theme`:
```kotlin
@Composable fun ExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    pureBlack: Boolean = false,
    reduceMotion: Boolean = false,          // defaults to `Settings.Global.ANIMATOR_DURATION_SCALE == 0`
    content: @Composable () -> Unit,
)
// sets MaterialTheme(colorScheme, typography = AppTypography, shapes = AppShapes,
//                    motionScheme = MotionScheme.expressive())  [+ reduced-motion scheme]
object AppTypography   // chunky display/headline: tight letterSpacing, heavy weights
object AppShapes       // MaterialShapes-based (MaterialTheme.shapes) — extra shape accessors:
val MaterialTheme.expressiveShapes: ExpressiveShapes   // pill, oval, cookie, squircle, bun, gem...
```
`dim` tokens — **M3 2025 dim roles are not part of the compose ColorScheme, we ship them**:
```kotlin
@Immutable data class DimTokens(
    val primaryDim: Color, val onPrimaryDim: Color,
    val primaryContainerDim: Color, val onPrimaryContainerDim: Color,
    val secondaryDim: Color, val tertiaryDim: Color,
    val surfaceDim: Color,                       // distinct from ColorScheme.surfaceDim
    val surfaceContainerLowestDim: Color, val surfaceContainerLowDim: Color,
    val surfaceContainerDim: Color, val surfaceContainerHighDim: Color,
    val surfaceContainerHighestDim: Color,
)
val ColorScheme.primaryDim: Color @Composable get() = LocalDimTokens.current.primaryDim
// …one @Composable extension property per field above, so call sites read
// `MaterialTheme.colorScheme.primaryDim` exactly as the design spec asks.
val MaterialTheme.dimTokens: DimTokens @Composable get() = LocalDimTokens.current
```

`com.abhiram.music.ui.glass` (Liquid Glass, strict 4-property formula: deep backdrop blur,
translucent tint, concentric border, deep shadow; floating layers only — never the app background):
```kotlin
val GlassBackdropSupported: Boolean                 // SDK_INT >= 33 && motion allowed
@Composable fun GlassBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit)
    // background+content layer; records itself so LiquidGlassSurface can sample it
@Composable fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = ExpressiveShapes.pill,
    tint: Color = Color.White.copy(alpha = 0.10f),          // 0.15f dark
    borderColor: Color = Color.White.copy(alpha = 0.20f),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 18.dp,
    blurRadius: Dp = 24.dp,
    enabled: Boolean = GlassBackdropSupported,               // false => tint+border fallback
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
)
fun Modifier.liquidGlass(...): Modifier     // convenience wrapper around LiquidGlassSurface
@Composable fun LiquidGlassTopBar(...)      // floating bottom-centered toolbar surface
```
Interaction rule: interactive children must subtly warp/brighten (graphicsLayer scale/alpha spring)
on hover/tap (pointerInput), honoring reduce-motion.

## 6. SHARED COMPONENTS (cross-group signatures that MUST match exactly)

Frozen signatures (CMP-A implements ★ files; other groups call them — do not redefine):

★ `com.abhiram.music.ui.component.TextPlaceholder(text: String, modifier: Modifier = Modifier)`
★ `com.abhiram.music.ui.component.ShimmerHost(modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable BoxScope.() -> Unit)`
★ `com.abhiram.music.ui.component.MusicBars(modifier: Modifier = Modifier, isPlaying: Boolean = true, barCount: Int = 4, color: Color = MaterialTheme.colorScheme.primary)`
★ `com.abhiram.music.ui.component.TooltipIconButton(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit)`
★ `com.abhiram.music.ui.component.PlaylistThumbnail(thumbnails: List<String>, modifier: Modifier = Modifier, shape: Shape = ExpressiveShapes.cookie, songCount: Int? = null)`
★ `com.abhiram.music.ui.component.SeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, squiggly: Boolean = true, bufferedPercentage: Int = 0)`
★ `com.abhiram.music.ui.component.TextFieldDialog(title: String, value: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "", label: String? = null, singleLine: Boolean = true)`
★ `com.abhiram.music.ui.component.ConfirmationDialog(title: String, text: String?, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier, confirmText: String = "OK", dismissText: String = "Cancel")`
★ `com.abhiram.music.ui.component.SwipeToActionBox(onAction: () -> Unit, modifier: Modifier = Modifier, actionIcon: ImageVector = Icons.Rounded.Check, actionColor: Color = MaterialTheme.colorScheme.primary, threshold: Dp = 96.dp, content: @Composable BoxScope.() -> Unit)`

CMP-A additionally owns (free signatures, but must follow the spec descriptions):
`BottomNavigation`, `ChipScaffold`, `CoverScaffold`, `HomeScaffold`, `TabScaffold`, `TabGroup`,
`MenuEntry` is NOT theirs (CMP-B owns `com.abhiram.music.ui.menu.MenuEntry`).

CMP-B owns `com.abhiram.music.ui.menu`:
`BaseMediaItemMenu`, `MediaItemMenu`, `QueuedMediaItemMenu`, `NonQueuedMediaItemMenu`,
`InPlaylistMediaItemMenu`, `InHistoryMediaItemMenu`, `MenuEntry` — bottom-sheet menus built on
`ModalBottomSheet`, every entry ≥48 dp, expressive spring on open.

CMP-C owns `com.abhiram.music.ui.component.items`: `AlbumItem`, `LocalAlbumItem`, `ArtistItem`,
`LocalArtistItem`, `ItemContainer`, `ItemPlaceholder`, `ListItemContainer`, `ListItemPlaceholder`,
`PlaylistItem`, `BuiltInPlaylistItem`, `LocalPlaylistItem`, `SongItem`, `LocalSongItem`,
`MediaSongItem`, `VideoItem` (grid + list variants, `onClick`/`onLongClick`/`menu` slots).

CMP-D owns `com.abhiram.music.ui.component.screen`: `AlbumScreen`, `AlbumSongs`, `ArtistScreen`,
`ArtistOverview`, `ArtistLocalSongs`, `BuiltInPlaylistScreen`, `BuiltInPlaylistSongs`,
`BuiltInPlaylistThumbnail`, `HomeAlbums`, `HomeArtistList`, `HomePlaylists`, `HomeSongs`,
`QuickPicks`, `LocalPlaylistScreen`, `LocalPlaylistSongs`, `PlaylistScreen`, `PlaylistSongList`.

CMP-E owns `com.abhiram.music.ui.player`: `Controls`, `Lyrics`, `MiniPlayer`, `PlaybackError`,
`Player`, `PlayerScaffold`, `Queue`, `SleepTimer`, `StatsForNerds`, `Thumbnail`.

CMP-F owns `com.abhiram.music.ui.screens.search` (`SearchInputField`, `SearchResults`,
`SearchScreen`) and `com.abhiram.music.ui.screens.settings` (`About`, `CacheSettings`,
`DatabaseSettings`, `GeneralSettings`, `GestureSettings`, `OtherSettings`, `PlayerSettings`,
`SettingsPage`, `SettingsScreen`, `SwitchSettingEntry`, `SettingsEntry`, `SettingsInformation`,
`SettingsProgress`).

### 6.1 Item component signatures (CMP-C implements these EXACTLY; other groups call them)

```kotlin
// package com.abhiram.music.ui.component.items
@Composable fun ItemContainer(          // square/grid cell
    title: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    thumbnail: @Composable () -> Unit, subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null, selected: Boolean = false,
    onLongClick: (() -> Unit)? = null, enabled: Boolean = true, sortIndex: Int? = null,
)
@Composable fun ListItemContainer(      // wide/list row
    title: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    thumbnail: @Composable () -> Unit, subtitle: String? = null,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,   // index, MusicBars, drag handle…
    trailingContent: (@Composable RowScope.() -> Unit)? = null,  // duration, menu…
    selected: Boolean = false, onLongClick: (() -> Unit)? = null, enabled: Boolean = true,
)
@Composable fun ItemPlaceholder(modifier: Modifier = Modifier)          // grid shimmer cell
@Composable fun ListItemPlaceholder(modifier: Modifier = Modifier)      // list shimmer row

// remote (innertube) models — alias the model import to avoid the name clash with the composable
@Composable fun SongItem(item: com.abhiram.innertube.models.SongItem, modifier: Modifier = Modifier,
    onClick: () -> Unit, onLongClick: (() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    leadingContent: (@Composable RowScope.() -> Unit)? = null, gridItem: Boolean = false)
@Composable fun VideoItem(item: com.abhiram.innertube.models.SongItem, modifier: Modifier = Modifier,
    onClick: () -> Unit, onLongClick: (() -> Unit)? = null, gridItem: Boolean = false)
@Composable fun AlbumItem(item: com.abhiram.innertube.models.AlbumItem, modifier: Modifier = Modifier,
    onClick: () -> Unit, onLongClick: (() -> Unit)? = null, gridItem: Boolean = true)
@Composable fun ArtistItem(item: com.abhiram.innertube.models.ArtistItem, modifier: Modifier = Modifier,
    onClick: () -> Unit, onLongClick: (() -> Unit)? = null, gridItem: Boolean = true)
@Composable fun PlaylistItem(item: com.abhiram.innertube.models.PlaylistItem, modifier: Modifier = Modifier,
    onClick: () -> Unit, onLongClick: (() -> Unit)? = null, gridItem: Boolean = true)

// local (Room) models — see §6.2
@Composable fun LocalSongItem(song: com.abhiram.music.database.entities.Song, modifier: Modifier = Modifier,
    onClick: () -> Unit, onLongClick: (() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    leadingContent: (@Composable RowScope.() -> Unit)? = null, gridItem: Boolean = false)
@Composable fun LocalAlbumItem(album: com.abhiram.music.database.entities.Album, modifier: Modifier = Modifier,
    onClick: () -> Unit, onLongClick: (() -> Unit)? = null, gridItem: Boolean = true)
@Composable fun LocalArtistItem(artist: com.abhiram.music.database.entities.Artist, modifier: Modifier = Modifier,
    onClick: () -> Unit, onLongClick: (() -> Unit)? = null, gridItem: Boolean = true)
@Composable fun LocalPlaylistItem(playlist: com.abhiram.music.database.entities.Playlist, modifier: Modifier = Modifier,
    onClick: () -> Unit, onLongClick: (() -> Unit)? = null, gridItem: Boolean = true)
@Composable fun BuiltInPlaylistItem(builtIn: com.abhiram.music.model.BuiltInPlaylist, modifier: Modifier = Modifier,
    onClick: () -> Unit, onLongClick: (() -> Unit)? = null, gridItem: Boolean = true)

// THE most used row: unified, plays through PlayerController, shows active state + menu slot
@Composable fun MediaSongItem(
    item: com.abhiram.music.model.MediaMetadata, modifier: Modifier = Modifier,
    onClick: () -> Unit, onLongClick: (() -> Unit)? = null,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    active: Boolean = false, playing: Boolean = false, gridItem: Boolean = false,
)
```

`BuiltInPlaylist` (LEAD owns `model/BuiltInPlaylist.kt`, written with the contract):
```kotlin
enum class BuiltInPlaylist(val id: String, val title: String) {
    FAVORITES("favorites", "Favorites"), OFFLINE("offline", "Offline"),
    DOWNLOADED("downloaded", "Downloaded"), HISTORY("history", "History"),
    TOP("top", "Top played"), CACHE("cache", "Cache");
}
```

### 6.2 Room relation wrappers (DB implements; UI consumes)

```kotlin
// package com.abhiram.music.database.entities
data class Song(@Embedded val song: SongEntity,
    @Relation(parentColumn="id", entityColumn="songId") val artists: List<SongArtistMap> = emptyList(), …) {
    val orderedArtists: List<ArtistEntity>  // resolved through SongArtistMap (DB may instead
                                            // declare @Relation directly to ArtistEntity)
    val id / title / duration / thumbnailUrl  // convenience accessors (delegate to song)
}
data class Artist(@Embedded val artist: ArtistEntity,
    @Relation(...) val songs: List<Song> = emptyList()) { val id; val name; val thumbnailUrl; val subscriberCountText }
data class Album(@Embedded val album: AlbumEntity,
    @Relation(...) val songs: List<Song> = emptyList()) { val id; val title; val thumbnailUrl; val year; val songCountText }
data class Playlist(@Embedded val playlist: PlaylistEntity,
    @Relation(...) val songs: List<Song> = emptyList()) { val id; val name; val thumbnailUrl; val description; val browseId }
data class SongWithStats(@Embedded val song: SongEntity, val playCount: Int, val totalPlayTimeMs: Long)
data class EventWithSong(@Embedded val event: Event, @Relation(...) val song: Song)
data class AlbumWithSongs(@Embedded val album: AlbumEntity, @Relation(...) val songs: List<Song>)
data class PlaylistWithSongs(@Embedded val playlist: PlaylistEntity, @Relation(...) val songs: List<Song>)
```
Relation wrappers must expose the convenience accessors shown (`id`, `title`/`name`,
`thumbnailUrl`) because item components use them directly.

## 7. SCREENS NOT OWNED BY COMPONENT GROUPS

* SCR-L: `ui/screens/library` → `SongsScreen`, `ArtistsScreen`, `AlbumsScreen`, `PlaylistsScreen`
  (chip/tab scaffold + items + viewmodels), `ui/screens/home` → `HomeScreen` (assembles
  `HomeScaffold` + `QuickPicks` + `HomeSongs` + `HomeAlbums` + `HomeArtistList` + `HomePlaylists`).
* SCR-D: `ui/screens/detail` → `ArtistDetailScreen`, `AlbumDetailScreen`, `PlaylistDetailScreen`,
  `LocalPlaylistDetailScreen`, `BuiltInPlaylistDetailScreen` + their ViewModels (thin wrappers that
  assemble the CMP-D components).

## 8. NAVIGATION (LEAD owns) — routes are fixed, screens must expose these entry points:

```
home, songs, artists, albums, playlists, search, settings,
artist/{id}, album/{id}, playlist/{id}, localPlaylist/{id}, builtInPlaylist/{id},
settings/general, settings/player, settings/gesture, settings/cache, settings/database,
settings/other, settings/about,
player, player/lyrics, player/queue, player/sleepTimer, player/stats, player/error
```
Every screen composable must be public and have a no-arg (or default-arg) constructor so it can be
wired into `NavHost`. Screens needing data take `id: String` as their first parameter.

## 9. DESIGN LANGUAGE (must be visible in the code you write)

* Expressive: spring-based motion (`spring(dampingRatio = Spring.DampingRatioMediumBouncy…)`),
  shape morphing between states (`animate*`), `animateContentSize()`, grouped controls inside a
  unified container, ≥48 dp touch targets, 4.5:1 text contrast.
* Liquid glass: floating bottom-centred toolbar, pill mini player, player pill control holder,
  update dialog — **not** the app background.
* Screens: Home top 40 % dynamic colour fading to black; Full player top 50 % dynamic colour from
  album art then pure black; mini player pill-shaped with circular album art + progress ring,
  play/pause, next, like, album title.
* `reduceMotion` handled globally in `ExpressiveTheme`.

## 10. INFRA (INFRA agent)

* `.github/workflows/build-debug.yml` — `actions/checkout@v4`, `actions/setup-java@v4` (temurin
  21), `gradle/actions/setup-gradle@v4`, then `./gradlew :app:assembleDebug`, upload APK artifact.
  Wrapper (`gradlew`, `gradle/wrapper/*`) already exists.
* `app/src/main/AndroidManifest.xml`: INTERNET, POST_NOTIFICATIONS, ACCESS_NETWORK_STATE, WAKE_LOCK,
  FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, FOREGROUND_SERVICE_DATA_SYNC?
  (only if used), `App` application, `MainActivity`, `service.PlayerService`
  (`android:foregroundServiceType="mediaPlayback"`, `exported=false`), theme/resource refs that
  actually exist.
* `app/src/main/res`: `values/strings.xml` (`app_name` = "Music"), `values/themes.xml`
  (Theme.Material3 base + splash), launcher adaptive icon built from XML vector (no PNG),
  `values/colors.xml`, night variants, `xml/backup_rules.xml`, `xml/network_security_config.xml`.
