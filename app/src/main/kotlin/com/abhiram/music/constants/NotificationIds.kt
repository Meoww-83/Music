package com.abhiram.music.constants

/**
 * Notification ids and channel ids shared between the UI and
 * [com.abhiram.music.service.PlayerService].
 */
object NotificationIds {
    /**
     * Id of the Media3 player (foreground) notification. Must stay stable for the lifetime of
     * the service — MediaSessionService updates the notification in place.
     */
    const val PLAYER: Int = 1

    /** Id used by the sleep-timer completion / expiry notification, if the service posts one. */
    const val SLEEP_TIMER: Int = 2
}

/** Notification channel ids created by the playback service. */
object NotificationChannels {
    const val PLAYER: String = "playback"
    const val PLAYER_NAME: String = "Playback"
}
