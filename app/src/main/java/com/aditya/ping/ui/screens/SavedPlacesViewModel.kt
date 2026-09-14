package com.aditya.ping.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aditya.ping.data.SavedPlaceEntity
import com.aditya.ping.data.SavedPlaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedPlacesViewModel(
    private val repo: SavedPlaceRepository,
    private val appContext: Context,
) : ViewModel() {

    val places: StateFlow<List<SavedPlaceEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, lat: Double, lng: Double, label: String, radius: Int) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        repo.insert(
            SavedPlaceEntity(
                name = name.trim(),
                lat = lat, lng = lng,
                addressLabel = label,
                radiusMeters = radius,
            ),
        )
    }

    fun delete(id: Long) = viewModelScope.launch {
        repo.deleteById(id)
    }

    class Factory(
        private val repo: SavedPlaceRepository,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SavedPlacesViewModel(repo, appContext) as T
    }
}
