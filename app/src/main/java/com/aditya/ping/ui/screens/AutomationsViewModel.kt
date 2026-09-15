package com.aditya.ping.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aditya.ping.data.AutomationEntity
import com.aditya.ping.data.AutomationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AutomationsViewModel(
    private val repo: AutomationRepository,
) : ViewModel() {

    val automations: StateFlow<List<AutomationEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _pendingUndo = MutableStateFlow<AutomationEntity?>(null)
    val pendingUndo: StateFlow<AutomationEntity?> = _pendingUndo

    fun add(automation: AutomationEntity) = viewModelScope.launch {
        repo.insert(automation)
    }

    fun update(automation: AutomationEntity) = viewModelScope.launch {
        repo.update(automation)
    }

    fun clone(automation: AutomationEntity) = viewModelScope.launch {
        repo.insert(automation.copy(id = 0, name = "${automation.name} (copy)"))
    }

    fun toggleEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        repo.setEnabled(id, enabled)
    }

    fun delete(id: Long) = viewModelScope.launch {
        val automation = automations.value.find { it.id == id }
        if (automation != null) {
            _pendingUndo.value = automation
        }
        repo.delete(id)
    }

    fun undoDelete() = viewModelScope.launch {
        val automation = _pendingUndo.value ?: return@launch
        repo.insert(automation)
        _pendingUndo.value = null
    }

    fun clearUndo() {
        _pendingUndo.value = null
    }

    class Factory(
        private val repo: AutomationRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AutomationsViewModel(repo) as T
    }
}
