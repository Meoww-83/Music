package com.abhiram.music.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.abhiram.music.constants.AppPreferences
import com.abhiram.music.database.DatabaseDao
import com.abhiram.music.database.MusicDatabase
import com.abhiram.music.repository.MusicRepository
import com.abhiram.music.repository.YouTubeRepository

/**
 * The app's manual dependency container (no Hilt/Koin).
 *
 * Everything is constructed lazily on first access with the application [Context], so creating
 * the container during `Application.onCreate()` is cheap. Access it through
 * [ServiceLocator.container].
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** The Room database (single instance, owned by this container). */
    val database: MusicDatabase by lazy { MusicDatabase.build(appContext) }

    /** The single [DatabaseDao] used by repositories, the service and the trackers. */
    val dao: DatabaseDao by lazy { database.dao }

    /** Local library / playlists / history repository (local DB is the source of truth). */
    val musicRepository: MusicRepository by lazy { MusicRepository(dao) }

    /** Thin InnerTube (`YouTube`) wrappers returning the real innertube page types. */
    val youTubeRepository: YouTubeRepository by lazy { YouTubeRepository() }

    /** Typed app preferences over the shared `settings` DataStore. */
    val preferences: AppPreferences by lazy { AppPreferences(appPreferencesDataStore) }
}

/**
 * The single `settings` DataStore for the process.
 *
 * The `preferencesDataStore` delegate guarantees exactly one instance per property, which is
 * what DataStore's singleton semantics require.
 */
internal val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
