package com.aditya.ping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aditya.ping.data.ThemePrefs
import com.aditya.ping.data.ThemeRepository
import com.aditya.ping.domain.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val themeRepo: ThemeRepository) : ViewModel() {

    val themePrefs: StateFlow<ThemePrefs> = themeRepo.themePrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemePrefs())

    val themeMode: StateFlow<ThemeMode> =
        themeRepo.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun setTheme(mode: ThemeMode) = viewModelScope.launch {
        themeRepo.setThemeMode(mode)
    }

    fun setAccentName(name: String) = viewModelScope.launch {
        themeRepo.setAccentName(name)
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        themeRepo.setDynamicColor(enabled)
    }

    fun setAnimationsEnabled(enabled: Boolean) = viewModelScope.launch {
        themeRepo.setAnimationsEnabled(enabled)
    }

    fun setHapticFeedback(enabled: Boolean) = viewModelScope.launch {
        themeRepo.setHapticFeedback(enabled)
    }

    fun setHapticIntensity(intensity: String) = viewModelScope.launch {
        themeRepo.setHapticIntensity(intensity)
    }

    class Factory(private val themeRepo: ThemeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(themeRepo) as T
    }
}
