package com.aditya.ping.util

import java.util.Calendar

/**
 * Smart snooze presets that speak human language.
 * Instead of "15 minutes", users see "Soon", "Later today", "This evening", "Tomorrow morning".
 */
object SmartSnooze {

    data class SnoozeOption(
        val label: String,
        /** Returns the timestamp to snooze to */
        val calculate: () -> Long,
    )

    /**
     * Returns the available snooze options for the current time.
     * Options that would be in the past are filtered out.
     */
    fun options(): List<SnoozeOption> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        val soon = now + 15 * 60_000L

        val laterToday = (cal.clone() as Calendar).apply {
            add(Calendar.HOUR_OF_DAY, 3)
        }.timeInMillis

        val thisEvening = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        val tomorrowMorning = (cal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val tomorrowMidday = (cal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return listOf(
            SnoozeOption("Soon (15 min)") { soon },
            SnoozeOption("Later today (3 hr)") { laterToday },
            SnoozeOption("This evening (6 PM)") { thisEvening },
            SnoozeOption("Tomorrow morning (9 AM)") { tomorrowMorning },
            SnoozeOption("Tomorrow midday (12 PM)") { tomorrowMidday },
        ).filter { it.calculate() > now }
    }
}
