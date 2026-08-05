package org.feelm.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "feelm_settings")

/**
 * Display preferences.
 *
 * The theme is a stored choice rather than a mirror of the system setting:
 * Feelm is paper by default on the web and the dark room is something you ask
 * for, and an app that silently disagrees with the site at 8pm is the same
 * product wearing two faces.
 */
class SettingsStore(private val context: Context) {

    private val darkThemeKey = booleanPreferencesKey("dark_theme")

    val darkTheme: Flow<Boolean> =
        context.settingsDataStore.data.map { it[darkThemeKey] ?: false }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { it[darkThemeKey] = enabled }
    }
}
