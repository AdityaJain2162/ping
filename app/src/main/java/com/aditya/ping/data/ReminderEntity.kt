package com.aditya.ping.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val lat: Double,
    val lng: Double,
    val addressLabel: String = "",
    /** meters */
    val radiusMeters: Int = 150,
    /** 0 = on arrival, 1 = on departure */
    val triggerType: Int = 0,
    /** epoch millis when time-based reminder fires, null = no time trigger */
    val dueAt: Long? = null,
    /** if true, fires as a full-screen alarm instead of a notification */
    val isAlarm: Boolean = false,
    /** snooze interval in minutes, 0 = no snooze */
    val snoozeMinutes: Int = 10,
    /** recurrence type: 0=none, 1=daily, 2=weekly, 3=weekdays, 4=weekends, 5=monthly, 6=yearly, 7=custom */
    val recurrenceType: Int = 0,
    /** custom recurrence interval (e.g., every N days/weeks) — used when recurrenceType=7 */
    val recurrenceInterval: Int = 1,
    /** recurrence end date epoch millis, null = no end */
    val recurrenceEndDate: Long? = null,
    /** if true, keep nagging with persistent notifications until marked done */
    val nagMode: Boolean = false,
    /** nag interval in minutes */
    val nagIntervalMinutes: Int = 15,
    /** ID of the list this reminder belongs to, null = no list */
    val listId: Long? = null,
    /** combined trigger mode: 0=OR (fire on either time or location), 1=AND (both required) */
    val triggerMode: Int = 0,
    /** ID of the automation to run when this reminder fires, null = no automation */
    val automationId: Long? = null,
    /** custom ringtone URI for alarms, empty = default */
    val ringtoneUri: String = "",
    /** anti-sleep dismiss mode: 0=none, 1=math challenge, 2=long-press 3s */
    val antiSleepDismiss: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val enabled: Boolean = true,
    /** whether the user has marked this reminder as done (separate from enabled) */
    val completed: Boolean = false,
    /** epoch millis when the reminder was marked completed, for history pruning */
    val completedAt: Long? = null,
    /** last fired epoch millis (cooldown) */
    val lastFiredAt: Long = 0L,
)
