package com.aditya.ping.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aditya.ping.R
import com.aditya.ping.data.ReminderEntity
import com.aditya.ping.data.ReminderRepository
import com.aditya.ping.util.AlarmScheduler
import com.aditya.ping.util.NagScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class SortMode(val labelRes: Int) {
    BY_DUE_DATE(R.string.sort_by_due_date),
    BY_TITLE(R.string.sort_by_title),
    BY_CREATED(R.string.sort_by_created),
}

enum class HomeFilter(val labelRes: Int) {
    ALL(R.string.filter_all),
    ACTIVE(R.string.filter_active),
    LOCATION(R.string.filter_location),
    TIME(R.string.filter_time),
    OVERDUE(R.string.filter_overdue),
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repo: ReminderRepository,
    private val appContext: Context,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortMode = MutableStateFlow(SortMode.BY_DUE_DATE)
    val sortMode: StateFlow<SortMode> = _sortMode

    private val _filter = MutableStateFlow(HomeFilter.ALL)
    val filter: StateFlow<HomeFilter> = _filter

    private val _pendingUndo = MutableStateFlow<ReminderEntity?>(null)
    val pendingUndo: StateFlow<ReminderEntity?> = _pendingUndo

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

            val active = list.count { it.enabled && !it.completed }
            val overdue = list.count {
                it.enabled && !it.completed && it.dueAt != null && it.dueAt < now
            }
            val dueToday = list.count {
                it.enabled && !it.completed && it.dueAt != null && it.dueAt in startOfToday..endOfToday
            }
            val completed = list.count { it.completed }
            HomeStats(active = active, overdue = overdue, dueToday = dueToday, completed = completed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeStats(0, 0, 0, 0))

    /** Sectioned reminders for grouped display — completed reminders are NOT shown here */
    val sections: StateFlow<List<ReminderSection>> =
        combine(reminders, _sortMode, _filter) { list, sortMode, filter ->
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

            fun sortList(items: List<ReminderEntity>): List<ReminderEntity> = when (sortMode) {
                SortMode.BY_DUE_DATE -> items.sortedBy { it.dueAt ?: Long.MAX_VALUE }
                SortMode.BY_TITLE -> items.sortedBy { it.title.lowercase() }
                SortMode.BY_CREATED -> items.sortedByDescending { it.createdAt }
            }

            val filtered = when (filter) {
                HomeFilter.ALL -> list
                HomeFilter.ACTIVE -> list.filter { it.enabled && !it.completed }
                HomeFilter.LOCATION -> list.filter { it.lat != 0.0 || it.lng != 0.0 }
                HomeFilter.TIME -> list.filter { it.dueAt != null }
                HomeFilter.OVERDUE -> list.filter {
                    it.enabled && !it.completed && it.dueAt != null && it.dueAt < now
                }
            }

            val overdueList = sortList(filtered.filter {
                it.enabled && !it.completed && it.dueAt != null && it.dueAt < now
            })

            val dueTodayList = sortList(filtered.filter {
                it.enabled && !it.completed && it.dueAt != null && it.dueAt in now..endOfToday
            })

            val locationList = sortList(filtered.filter {
                it.enabled && !it.completed && (it.lat != 0.0 || it.lng != 0.0) && (it.dueAt == null || it.dueAt > endOfToday)
            })

            val upcomingList = sortList(filtered.filter {
                it.enabled && !it.completed && it.dueAt != null && it.dueAt > endOfToday && (it.lat == 0.0 && it.lng == 0.0)
            })

            val laterList = sortList(filtered.filter {
                it.enabled && !it.completed && it.dueAt == null && (it.lat == 0.0 && it.lng == 0.0)
            })

            // Disabled (but not completed) reminders — shown in a separate section
            val disabledList = sortList(filtered.filter { !it.enabled && !it.completed })

            buildList {
                if (overdueList.isNotEmpty()) add(ReminderSection("Overdue", overdueList, isOverdue = true))
                if (dueTodayList.isNotEmpty()) add(ReminderSection("Due Today", dueTodayList))
                if (locationList.isNotEmpty()) add(ReminderSection("Location", locationList))
                if (upcomingList.isNotEmpty()) add(ReminderSection("Upcoming", upcomingList))
                if (laterList.isNotEmpty()) add(ReminderSection("Later", laterList))
                if (disabledList.isNotEmpty()) add(ReminderSection("Disabled", disabledList))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** History of completed reminders from the last 3 months */
    val history: StateFlow<List<ReminderEntity>> =
        repo.observeHistory(threeMonthsAgo())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Count of completed reminders (for the "View History" badge) */
    val historyCount: StateFlow<Int> =
        history.map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        // Prune completed reminders older than 3 months on startup
        viewModelScope.launch {
            repo.pruneOldCompleted(threeMonthsAgo())
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSortModeChange(mode: SortMode) {
        _sortMode.value = mode
    }

    fun onFilterChange(filter: HomeFilter) {
        _filter.value = filter
    }

    fun markAllDone(reminderIds: List<Long>) = viewModelScope.launch {
        reminderIds.forEach { id ->
            val r = repo.getById(id)
            if (r != null && !r.completed) {
                repo.update(r.copy(completed = true, completedAt = System.currentTimeMillis()))
                AlarmScheduler.cancel(appContext, id)
            }
        }
    }

    fun deleteAll(reminderIds: List<Long>) = viewModelScope.launch {
        reminderIds.forEach { id -> repo.deleteById(id) }
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

    fun toggleCompleted(id: Long, completed: Boolean) = viewModelScope.launch {
        val timestamp = if (completed) System.currentTimeMillis() else null
        repo.setCompleted(id, completed, timestamp)
        if (completed) {
            AlarmScheduler.cancel(appContext, id)
            NagScheduler.cancel(appContext, id)
        } else {
            val reminder = repo.getById(id) ?: return@launch
            if (reminder.enabled && reminder.dueAt != null) {
                AlarmScheduler.schedule(appContext, reminder)
            }
        }
    }

    fun delete(id: Long) = viewModelScope.launch {
        val reminder = repo.getById(id)
        if (reminder != null) {
            _pendingUndo.value = reminder
        }
        AlarmScheduler.cancel(appContext, id)
        NagScheduler.cancel(appContext, id)
        repo.deleteById(id)
    }

    fun undoDelete() = viewModelScope.launch {
        val reminder = _pendingUndo.value ?: return@launch
        repo.insert(reminder)
        if (reminder.enabled && reminder.dueAt != null) {
            AlarmScheduler.schedule(appContext, reminder)
        }
        _pendingUndo.value = null
    }

    fun clone(id: Long) = viewModelScope.launch {
        val reminder = repo.getById(id) ?: return@launch
        val clone = reminder.copy(
            id = 0,
            title = "${reminder.title} (copy)",
            enabled = false, // Cloned reminders start disabled to avoid surprise alarms
            completed = false,
            completedAt = null,
            lastFiredAt = 0L,
            createdAt = System.currentTimeMillis(),
        )
        val newId = repo.insert(clone)
        val saved = clone.copy(id = newId)
        if (saved.enabled && saved.dueAt != null) {
            AlarmScheduler.schedule(appContext, saved)
        }
    }

    fun clearUndo() {
        _pendingUndo.value = null
    }

    private fun threeMonthsAgo(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -3)
        return cal.timeInMillis
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
