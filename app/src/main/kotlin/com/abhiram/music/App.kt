package com.abhiram.music

import android.app.Application
import com.abhiram.music.di.ServiceLocator
import timber.log.Timber

/**
 * Application entry point.
 *
 * Initialises the manual dependency container (Room, DataStore, repositories) and plants Timber.
 * Playback starts lazily from the UI through [com.abhiram.music.playback.PlayerControllerProvider]
 * — nothing heavy happens on the main thread during start-up.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
        ServiceLocator.init(this)
    }

    /** Minimal release tree: keeps logs out of logcat in release builds. */
    private class CrashReportingTree : Timber.Tree() {
        override fun log(
            priority: Int,
            tag: String?,
            message: String,
            throwable: Throwable?,
        ) {
            // Intentionally empty: release builds stay quiet, crashes go to the system handler.
        }
    }
}
