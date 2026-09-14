package com.aditya.ping.util

import com.aditya.ping.data.ReminderEntity
import java.util.Calendar

/**
 * Calculates the next occurrence of a recurring reminder.
 *
 * Recurrence types:
 *  0 = none (one-time)
 *  1 = daily
 *  2 = weekly
 *  3 = weekdays (Mon-Fri)
 *  4 = weekends (Sat-Sun)
 *  5 = monthly
 *  6 = yearly
 *  7 = custom (every N days)
 */
object RecurrenceCalculator {

    /**
     * Returns the next occurrence after [fromTime], or null if the recurrence
     * has ended (past [ReminderEntity.recurrenceEndDate]).
     */
    fun nextOccurrence(reminder: ReminderEntity, fromTime: Long): Long? {
        if (reminder.recurrenceType == 0) return null

        val end = reminder.recurrenceEndDate
        if (end != null && fromTime >= end) return null

        val cal = Calendar.getInstance().apply { timeInMillis = fromTime }

        val next = when (reminder.recurrenceType) {
            1 -> { // daily
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            2 -> { // weekly
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                cal.timeInMillis
            }
            3 -> { // weekdays
                var found = false
                while (!found) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    val day = cal.get(Calendar.DAY_OF_WEEK)
                    if (day in Calendar.MONDAY..Calendar.FRIDAY) found = true
                }
                cal.timeInMillis
            }
            4 -> { // weekends
                var found = false
                while (!found) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    val day = cal.get(Calendar.DAY_OF_WEEK)
                    if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) found = true
                }
                cal.timeInMillis
            }
            5 -> { // monthly
                cal.add(Calendar.MONTH, 1)
                cal.timeInMillis
            }
            6 -> { // yearly
                cal.add(Calendar.YEAR, 1)
                cal.timeInMillis
            }
            7 -> { // custom interval (days)
                cal.add(Calendar.DAY_OF_YEAR, reminder.recurrenceInterval.coerceAtLeast(1))
                cal.timeInMillis
            }
            else -> return null
        }

        if (end != null && next > end) return null
        return next
    }
}
