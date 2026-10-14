package com.abhiram.music.constants

/**
 * Central Timber tag constants so log output is greppable and consistent across the app.
 * (All logging goes through `Timber.tag(...)`; never `println`.)
 */
object Logs {
    const val APP: String = "Music/App"
    const val DI: String = "Music/DI"
    const val REPOSITORY: String = "Music/Repository"
    const val YOUTUBE_REPOSITORY: String = "Music/YouTubeRepository"
    const val PLAYBACK: String = "Music/Playback"
    const val SLEEP_TIMER: String = "Music/SleepTimer"
    const val STATS: String = "Music/PlaybackStats"
    const val SERVICE: String = "Music/PlayerService"
    const val DATABASE: String = "Music/Database"
    const val UPDATE: String = "Music/UpdateChecker"
}
