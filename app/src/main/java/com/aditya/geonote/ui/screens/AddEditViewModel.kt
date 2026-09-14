package com.aditya.geonote.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aditya.geonote.data.ReminderEntity
import com.aditya.geonote.data.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditState(
    val id: Long = 0,
    val title: String = "",
    val note: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val addressLabel: String = "",
    val radiusMeters: Int = 150,
    val triggerType: Int = 0,
    val isEdit: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class AddEditViewModel(private val repo: ReminderRepository) : ViewModel() {

    private val _state = MutableStateFlow(AddEditState())
    val state: StateFlow<AddEditState> = _state.asStateFlow()

    fun load(id: Long) = viewModelScope.launch {
        if (id <= 0L) return@launch
        repo.getById(id)?.let { r ->
            _state.update {
                AddEditState(
                    id = r.id, title = r.title, note = r.note,
                    lat = r.lat, lng = r.lng, addressLabel = r.addressLabel,
                    radiusMeters = r.radiusMeters, triggerType = r.triggerType,
                    isEdit = true,
                )
            }
        }
    }

    fun onTitleChange(v: String) = _state.update { it.copy(title = v) }
    fun onNoteChange(v: String) = _state.update { it.copy(note = v) }
    fun onLocation(lat: Double, lng: Double, label: String) =
        _state.update { it.copy(lat = lat, lng = lng, addressLabel = label) }
    fun onRadiusChange(v: Int) = _state.update { it.copy(radiusMeters = v.coerceIn(50, 1000)) }
    fun onTriggerChange(v: Int) = _state.update { it.copy(triggerType = v) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.title.isBlank() || (s.lat == 0.0 && s.lng == 0.0)) return@launch
        _state.update { it.copy(saving = true) }
        val entity = ReminderEntity(
            id = if (s.isEdit) s.id else 0,
            title = s.title.trim(),
            note = s.note.trim(),
            lat = s.lat, lng = s.lng,
            addressLabel = s.addressLabel,
            radiusMeters = s.radiusMeters,
            triggerType = s.triggerType,
        )
        if (s.isEdit) repo.update(entity) else repo.insert(entity)
        _state.update { it.copy(saving = false, saved = true) }
    }

    class Factory(private val repo: ReminderRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AddEditViewModel(repo) as T
    }
}
