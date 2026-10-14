package com.abhiram.music.di

import android.content.Context
import com.abhiram.music.constants.Logs
import timber.log.Timber

/**
 * Process-wide holder for the single [AppContainer].
 *
 * [com.abhiram.music.App.onCreate] calls [init]; everything else (screens, services, workers)
 * resolves dependencies through [container].
 */
object ServiceLocator {

    @Volatile
    private var instance: AppContainer? = null

    /**
     * Creates the [AppContainer] exactly once. Safe to call from multiple threads; the context
     * is immediately reduced to its application context.
     *
     * @throws IllegalStateException never — repeated calls are ignored.
     */
    fun init(context: Context) {
        if (instance != null) return
        synchronized(this) {
            if (instance == null) {
                Timber.tag(Logs.DI).d("Initialising AppContainer")
                instance = AppContainer(context.applicationContext)
            }
        }
    }

    /**
     * The shared [AppContainer].
     *
     * @throws IllegalStateException if [init] has not run yet — this is always a wiring bug
     * (the `App` application class must call [init] in `onCreate`).
     */
    val container: AppContainer
        get() =
            instance
                ?: throw IllegalStateException(
                    "ServiceLocator.init(context) has not been called. " +
                        "Ensure com.abhiram.music.App.onCreate() runs before any component " +
                        "accesses ServiceLocator.container (e.g. when testing or from a " +
                        "custom Application class).",
                )
}
