package com.abhiram.music.model

/**
 * Built-in (system) playlists exposed by the library screens and the built-in playlist detail
 * screen. [id] doubles as the navigation route argument (`builtInPlaylist/{id}`).
 */
enum class BuiltInPlaylist(
    val id: String,
    val title: String,
) {
    FAVORITES("favorites", "Favorites"),
    OFFLINE("offline", "Offline"),
    DOWNLOADED("downloaded", "Downloaded"),
    HISTORY("history", "History"),
    TOP("top", "Top played"),
    CACHE("cache", "Cache"),
    ;

    companion object {
        fun fromId(id: String?): BuiltInPlaylist? = entries.firstOrNull { it.id == id }
    }
}
