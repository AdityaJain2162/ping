package com.aditya.ping.util

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
}
