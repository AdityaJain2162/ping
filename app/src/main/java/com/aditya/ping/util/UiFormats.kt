package com.aditya.ping.util

import android.content.Context
import com.aditya.ping.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UiFormats {
    private val reminderDateFormat = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())
    private val historyDateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val timeOnlyFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun formatReminderDate(timestamp: Long): String = reminderDateFormat.format(Date(timestamp))
    fun formatHistoryDate(timestamp: Long): String = historyDateFormat.format(Date(timestamp))
    fun formatDateOnly(timestamp: Long): String = dateOnlyFormat.format(Date(timestamp))
    fun formatTimeOnly(timestamp: Long): String = timeOnlyFormat.format(Date(timestamp))

    /**
     * Returns a human-readable "time remaining" string for a reminder due
     * within the next 7 days (e.g. "in 2 hours", "in 3 days").
     * Returns null if the reminder is overdue or more than a week away.
     */
    fun formatTimeRemaining(context: Context, dueAt: Long, now: Long = System.currentTimeMillis()): String? {
        val diff = dueAt - now
        if (diff <= 0) return null // overdue — handled separately
        val minutes = diff / 60_000L
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days >= 7 -> null // more than a week — don't show
            days >= 1 -> context.getString(R.string.time_in_days, days.toInt())
            hours >= 1 -> context.getString(R.string.time_in_hours, hours.toInt())
            minutes >= 1 -> context.getString(R.string.time_in_minutes, minutes.toInt())
            else -> context.getString(R.string.time_in_less_than_minute)
        }
    }
}
