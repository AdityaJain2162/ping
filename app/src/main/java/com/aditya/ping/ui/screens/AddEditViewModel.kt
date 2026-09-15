package com.aditya.ping.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aditya.ping.data.ReminderEntity
import com.aditya.ping.data.ReminderRepository
import com.aditya.ping.util.AlarmScheduler
import com.aditya.ping.util.NagScheduler
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
    /** epoch millis for time trigger, null = no time trigger */
    val dueAt: Long? = null,
    /** if true, fires as full-screen alarm instead of notification */
    val isAlarm: Boolean = false,
    /** snooze interval in minutes */
    val snoozeMinutes: Int = 10,
    /** recurrence type: 0=none, 1=daily, 2=weekly, 3=weekdays, 4=weekends, 5=monthly, 6=yearly, 7=custom */
    val recurrenceType: Int = 0,
    /** custom recurrence interval (e.g., every N days) */
    val recurrenceInterval: Int = 1,
    /** recurrence end date epoch millis, null = no end */
    val recurrenceEndDate: Long? = null,
    /** if true, keep nagging with persistent notifications until marked done */
    val nagMode: Boolean = false,
    /** nag interval in minutes */
    val nagIntervalMinutes: Int = 15,
    /** combined trigger mode: 0=OR (fire on either time or location), 1=AND (both required) */
    val triggerMode: Int = 0,
    /** quick action type: 0=none, 1=call, 2=whatsapp, 3=open app, 4=navigate, 5=url */
    val quickActionType: Int = 0,
    /** quick action data: phone number, package name, URL */
    val quickActionData: String = "",
    /** quick action message: WhatsApp/SMS message body */
    val quickActionMessage: String = "",
    /** custom ringtone URI for alarms, empty = default */
    val ringtoneUri: String = "",
    /** anti-sleep dismiss mode: 0=none, 1=math challenge, 2=long-press 3s */
    val antiSleepDismiss: Int = 0,
    val isEdit: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class AddEditViewModel(
    private val repo: ReminderRepository,
    private val appContext: Context,
) : ViewModel() {

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
                    dueAt = r.dueAt, isAlarm = r.isAlarm, snoozeMinutes = r.snoozeMinutes,
                    recurrenceType = r.recurrenceType, recurrenceInterval = r.recurrenceInterval,
                    recurrenceEndDate = r.recurrenceEndDate,
                    nagMode = r.nagMode, nagIntervalMinutes = r.nagIntervalMinutes,
                    triggerMode = r.triggerMode,
                    quickActionType = r.quickActionType,
                    quickActionData = r.quickActionData,
                    quickActionMessage = r.quickActionMessage,
                    ringtoneUri = r.ringtoneUri,
                    antiSleepDismiss = r.antiSleepDismiss,
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
    fun onDueAtChange(v: Long?) = _state.update { it.copy(dueAt = v) }
    fun onAlarmToggle(v: Boolean) = _state.update { it.copy(isAlarm = v) }
    fun onSnoozeChange(v: Int) = _state.update { it.copy(snoozeMinutes = v.coerceIn(1, 60)) }
    fun onRecurrenceTypeChange(v: Int) = _state.update { it.copy(recurrenceType = v) }
    fun onRecurrenceIntervalChange(v: Int) = _state.update { it.copy(recurrenceInterval = v.coerceAtLeast(1)) }
    fun onRecurrenceEndDateChange(v: Long?) = _state.update { it.copy(recurrenceEndDate = v) }
    fun onNagModeToggle(v: Boolean) = _state.update { it.copy(nagMode = v) }
    fun onNagIntervalChange(v: Int) = _state.update { it.copy(nagIntervalMinutes = v.coerceIn(1, 120)) }
    fun onTriggerModeChange(v: Int) = _state.update { it.copy(triggerMode = v) }
    fun onQuickActionTypeChange(v: Int) = _state.update { it.copy(quickActionType = v) }
    fun onQuickActionDataChange(v: String) = _state.update { it.copy(quickActionData = v) }
    fun onQuickActionMessageChange(v: String) = _state.update { it.copy(quickActionMessage = v) }
    fun onRingtoneUriChange(v: String) = _state.update { it.copy(ringtoneUri = v) }
    fun onAntiSleepChange(v: Int) = _state.update { it.copy(antiSleepDismiss = v) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.title.isBlank()) return@launch
        val hasLocation = s.lat != 0.0 || s.lng != 0.0
        val hasTime = s.dueAt != null
        if (!hasLocation && !hasTime) return@launch
        // Alarm requires a time trigger
        if (s.isAlarm && !hasTime) return@launch

        _state.update { it.copy(saving = true) }
        val entity = ReminderEntity(
            id = if (s.isEdit) s.id else 0,
            title = s.title.trim(),
            note = s.note.trim(),
            lat = s.lat, lng = s.lng,
            addressLabel = s.addressLabel,
            radiusMeters = s.radiusMeters,
            triggerType = s.triggerType,
            dueAt = s.dueAt,
            isAlarm = s.isAlarm,
            snoozeMinutes = s.snoozeMinutes,
            recurrenceType = if (s.dueAt != null) s.recurrenceType else 0,
            recurrenceInterval = s.recurrenceInterval,
            recurrenceEndDate = s.recurrenceEndDate,
            nagMode = s.nagMode,
            nagIntervalMinutes = s.nagIntervalMinutes,
            triggerMode = if (hasLocation && hasTime) s.triggerMode else 0,
            quickActionType = s.quickActionType,
            quickActionData = s.quickActionData.trim(),
            quickActionMessage = s.quickActionMessage.trim(),
            ringtoneUri = s.ringtoneUri,
            antiSleepDismiss = s.antiSleepDismiss,
        )
        val id = if (s.isEdit) {
            repo.update(entity)
            s.id
        } else {
            repo.insert(entity)
        }

        val saved = entity.copy(id = id)
        // Always cancel old nag when saving — it will be rescheduled when the alarm fires
        NagScheduler.cancel(appContext, id)
        if (saved.dueAt != null && saved.enabled) {
            AlarmScheduler.schedule(appContext, saved)
        } else if (s.isEdit) {
            AlarmScheduler.cancel(appContext, s.id)
        }

        _state.update { it.copy(saving = false, saved = true) }
    }

    class Factory(
        private val repo: ReminderRepository,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AddEditViewModel(repo, appContext) as T
    }
}
