package com.aditya.ping.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aditya.ping.data.ReminderEntity
import com.aditya.ping.data.ReminderRepository
import com.aditya.ping.util.AlarmScheduler
import com.aditya.ping.util.NagScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repo: ReminderRepository,
    private val appContext: Context,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val reminders: StateFlow<List<ReminderEntity>> =
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) repo.observeAll()
            else repo.search(query)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        repo.setEnabled(id, enabled)
        val reminder = repo.getById(id) ?: return@launch
        if (enabled && reminder.dueAt != null) {
            AlarmScheduler.schedule(appContext, reminder)
        } else {
            AlarmScheduler.cancel(appContext, id)
            NagScheduler.cancel(appContext, id)
        }
    }

    fun delete(id: Long) = viewModelScope.launch {
        AlarmScheduler.cancel(appContext, id)
        NagScheduler.cancel(appContext, id)
        repo.deleteById(id)
    }

    class Factory(
        private val repo: ReminderRepository,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repo, appContext) as T
    }
}
