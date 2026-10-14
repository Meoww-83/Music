package com.abhiram.music.ui.navigation

/**
 * Every navigation route in the app — the single source of truth used by the NavHost and by
 * screens when they navigate deeper (`onNavigate(route)` callbacks).
 */
object Routes {
    const val HOME = "home"
    const val SONGS = "songs"
    const val ARTISTS = "artists"
    const val ALBUMS = "albums"
    const val PLAYLISTS = "playlists"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    const val ARTIST = "artist/{id}"
    fun artist(id: String) = "artist/$id"

    const val ALBUM = "album/{id}"
    fun album(id: String) = "album/$id"

    const val PLAYLIST = "playlist/{id}"
    fun playlist(id: String) = "playlist/$id"

    const val LOCAL_PLAYLIST = "localPlaylist/{id}"
    fun localPlaylist(id: String) = "localPlaylist/$id"

    const val BUILT_IN_PLAYLIST = "builtInPlaylist/{id}"
    fun builtInPlaylist(id: String) = "builtInPlaylist/$id"

    const val SETTINGS_GENERAL = "settings/general"
    const val SETTINGS_PLAYER = "settings/player"
    const val SETTINGS_GESTURE = "settings/gesture"
    const val SETTINGS_CACHE = "settings/cache"
    const val SETTINGS_DATABASE = "settings/database"
    const val SETTINGS_OTHER = "settings/other"
    const val SETTINGS_ABOUT = "settings/about"

    const val PLAYER = "player"
    const val PLAYER_LYRICS = "player/lyrics"
    const val PLAYER_QUEUE = "player/queue"
    const val PLAYER_SLEEP_TIMER = "player/sleepTimer"
    const val PLAYER_STATS = "player/stats"
    const val PLAYER_ERROR = "player/error"

    /** Destinations shown in the floating bottom navigation. */
    val bottomBar = listOf(HOME, SONGS, PLAYLISTS, SEARCH, SETTINGS)

    fun String.matchesBottomBar(): Boolean = this in bottomBar
}
