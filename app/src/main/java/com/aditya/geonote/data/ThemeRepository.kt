package com.aditya.geonote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aditya.geonote.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeRepository(private val context: Context) {

    private val key = intPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        ThemeMode.fromOrdinalSafe(prefs[key] ?: ThemeMode.SYSTEM.ordinal)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[key] = mode.ordinal }
    }
}
