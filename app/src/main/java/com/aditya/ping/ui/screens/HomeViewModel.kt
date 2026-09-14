package com.aditya.ping.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aditya.ping.data.ReminderEntity
import com.aditya.ping.data.ReminderRepository
import com.aditya.ping.util.AlarmScheduler
import com.aditya.ping.util.NagScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repo: ReminderRepository,
    private val appContext: Context,
) : ViewModel() {

    val reminders: StateFlow<List<ReminderEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
