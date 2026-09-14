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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

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

    /** Stats for the header bar */
    val stats: StateFlow<HomeStats> =
        reminders.map { list ->
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val endOfToday = cal.timeInMillis

            val active = list.count { it.enabled }
            val overdue = list.count {
                it.enabled && it.dueAt != null && it.dueAt < now
            }
            val dueToday = list.count {
                it.enabled && it.dueAt != null && it.dueAt in startOfToday..endOfToday
            }
            val completed = list.count { !it.enabled }
            HomeStats(active = active, overdue = overdue, dueToday = dueToday, completed = completed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeStats(0, 0, 0, 0))

    /** Sectioned reminders for grouped display */
    val sections: StateFlow<List<ReminderSection>> =
        reminders.map { list ->
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val endOfToday = cal.timeInMillis

            val overdueList = list.filter {
                it.enabled && it.dueAt != null && it.dueAt < now
            }.sortedBy { it.dueAt }

            val dueTodayList = list.filter {
                it.enabled && it.dueAt != null && it.dueAt in now..endOfToday
            }.sortedBy { it.dueAt }

            val locationList = list.filter {
                it.enabled && (it.lat != 0.0 || it.lng != 0.0) && (it.dueAt == null || it.dueAt > endOfToday)
            }

            val upcomingList = list.filter {
                it.enabled && it.dueAt != null && it.dueAt > endOfToday && (it.lat == 0.0 && it.lng == 0.0)
            }.sortedBy { it.dueAt }

            val laterList = list.filter {
                it.enabled && it.dueAt == null && (it.lat == 0.0 && it.lng == 0.0)
            }

            val completedList = list.filter { !it.enabled }

            buildList {
                if (overdueList.isNotEmpty()) add(ReminderSection("Overdue", overdueList, isOverdue = true))
                if (dueTodayList.isNotEmpty()) add(ReminderSection("Due Today", dueTodayList))
                if (locationList.isNotEmpty()) add(ReminderSection("Location", locationList))
                if (upcomingList.isNotEmpty()) add(ReminderSection("Upcoming", upcomingList))
                if (laterList.isNotEmpty()) add(ReminderSection("Later", laterList))
                if (completedList.isNotEmpty()) add(ReminderSection("Completed", completedList))
            }
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

data class HomeStats(
    val active: Int,
    val overdue: Int,
    val dueToday: Int,
    val completed: Int,
)

data class ReminderSection(
    val title: String,
    val reminders: List<ReminderEntity>,
    val isOverdue: Boolean = false,
)
