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
    /** quick action type: 0=none, 1=call, 2=whatsapp, 3=open app, 4=navigate, 5=url */
    val quickActionType: Int = 0,
    /** quick action data: phone number, package name, URL, or label */
    val quickActionData: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val enabled: Boolean = true,
    /** last fired epoch millis (cooldown) */
    val lastFiredAt: Long = 0L,
)
