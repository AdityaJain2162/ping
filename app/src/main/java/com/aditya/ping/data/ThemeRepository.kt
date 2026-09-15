package com.aditya.ping.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aditya.ping.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class ThemePrefs(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val accentName: String = "Coral",
    val dynamicColor: Boolean = false,
    val animationsEnabled: Boolean = true,
    val hapticFeedback: Boolean = true,
    val hapticIntensity: String = "MEDIUM",
)

class ThemeRepository(private val context: Context) {

    private val modeKey = intPreferencesKey("theme_mode")
    private val accentKey = stringPreferencesKey("accent_name")
    private val dynamicKey = booleanPreferencesKey("dynamic_color")
    private val animKey = booleanPreferencesKey("animations_enabled")
    private val hapticKey = booleanPreferencesKey("haptic_feedback")
    private val hapticIntensityKey = stringPreferencesKey("haptic_intensity")

    val themePrefs: Flow<ThemePrefs> = context.themeDataStore.data.map { prefs ->
        ThemePrefs(
            mode = ThemeMode.fromOrdinalSafe(prefs[modeKey] ?: ThemeMode.SYSTEM.ordinal),
            accentName = prefs[accentKey] ?: "Coral",
            dynamicColor = prefs[dynamicKey] ?: false,
            animationsEnabled = prefs[animKey] ?: true,
            hapticFeedback = prefs[hapticKey] ?: true,
            hapticIntensity = prefs[hapticIntensityKey] ?: "MEDIUM",
        )
    }

    val themeMode: Flow<ThemeMode> = themePrefs.map { it.mode }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[modeKey] = mode.ordinal }
    }

    suspend fun setAccentName(name: String) {
        context.themeDataStore.edit { it[accentKey] = name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.themeDataStore.edit { it[dynamicKey] = enabled }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.themeDataStore.edit { it[animKey] = enabled }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.themeDataStore.edit { it[hapticKey] = enabled }
    }

    suspend fun setHapticIntensity(intensity: String) {
        context.themeDataStore.edit { it[hapticIntensityKey] = intensity }
    }
}
