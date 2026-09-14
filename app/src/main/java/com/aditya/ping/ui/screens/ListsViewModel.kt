package com.aditya.ping.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aditya.ping.data.ReminderListEntity
import com.aditya.ping.data.ReminderListRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListsViewModel(
    private val repo: ReminderListRepository,
    private val appContext: Context,
) : ViewModel() {

    val lists: StateFlow<List<ReminderListEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, color: Int) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        repo.insert(ReminderListEntity(name = name.trim(), color = color))
    }

    fun delete(id: Long) = viewModelScope.launch {
        repo.deleteById(id)
    }

    class Factory(
        private val repo: ReminderListRepository,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ListsViewModel(repo, appContext) as T
    }
}
