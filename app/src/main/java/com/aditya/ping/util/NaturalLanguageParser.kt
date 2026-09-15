package com.aditya.ping.util

import java.util.Calendar
import java.util.Locale

/**
 * On-device natural language parser for quick-add.
 * Parses free text like "remind me to buy milk tomorrow at 5 PM every week"
 * and returns structured reminder fields. No cloud, no ML — pure regex.
 */
object NaturalLanguageParser {

    data class ParsedReminder(
        val title: String,
        val dueAt: Long? = null,
        val recurrenceType: Int = 0, // 0=none, 1=daily, 2=weekly, 3=weekdays, 4=weekends
        val isAlarm: Boolean = false,
        val triggerType: Int? = null, // 0=arrive, 1=leave
        val addressLabel: String = "",
    )

    private val TIME_REGEX = Regex(
        """\bat\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm|noon|midnight)?""",
        RegexOption.IGNORE_CASE,
    )
    private val TOMORROW_REGEX = Regex("""\btomorrow\b""", RegexOption.IGNORE_CASE)
    private val TODAY_REGEX = Regex("""\btoday\b""", RegexOption.IGNORE_CASE)
    private val WEEKDAY_REGEX = Regex(
        """\b(next\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val RECUR_DAILY = Regex("""\b(every\s+day|daily)\b""", RegexOption.IGNORE_CASE)
    private val RECUR_WEEKLY = Regex("""\b(every\s+week|weekly)\b""", RegexOption.IGNORE_CASE)
    private val RECUR_WEEKDAYS = Regex("""\b(weekdays|every\s+weekday)\b""", RegexOption.IGNORE_CASE)
    private val RECUR_WEEKENDS = Regex("""\b(weekends|every\s+weekend)\b""", RegexOption.IGNORE_CASE)
    private val ALARM_REGEX = Regex("""\b(alarm|ring|wake\s+me)\b""", RegexOption.IGNORE_CASE)
    private val ARRIVE_REGEX = Regex("""\b(when\s+i\s+arrive|arrive\s+at|arriving\s+at|at\s+the\s+)\b""", RegexOption.IGNORE_CASE)
    private val LEAVE_REGEX = Regex("""\b(when\s+i\s+leave|leave\s+|leaving\s+)\b""", RegexOption.IGNORE_CASE)
    private val LOCATION_AT = Regex("""\bat\s+([a-z][a-z\s]{2,}?)(?=\s+(?:tomorrow|today|every|at\s+\d)|$)""", RegexOption.IGNORE_CASE)

    private val WEEKDAY_MAP = mapOf(
        "monday" to Calendar.MONDAY, "mon" to Calendar.MONDAY,
        "tuesday" to Calendar.TUESDAY, "tue" to Calendar.TUESDAY,
        "wednesday" to Calendar.WEDNESDAY, "wed" to Calendar.WEDNESDAY,
        "thursday" to Calendar.THURSDAY, "thu" to Calendar.THURSDAY,
        "friday" to Calendar.FRIDAY, "fri" to Calendar.FRIDAY,
        "saturday" to Calendar.SATURDAY, "sat" to Calendar.SATURDAY,
        "sunday" to Calendar.SUNDAY, "sun" to Calendar.SUNDAY,
    )

    fun parse(input: String): ParsedReminder {
        val text = input.trim()
        if (text.isBlank()) return ParsedReminder(title = "")

        // Extract time
        var hour: Int? = null
        var minute: Int? = null
        val timeMatch = TIME_REGEX.find(text)
        if (timeMatch != null) {
            hour = timeMatch.groupValues[1].toIntOrNull()
            minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
            val ampm = timeMatch.groupValues[3].lowercase(Locale.ROOT)
            when (ampm) {
                "pm" -> if (hour != null && hour < 12) hour += 12
                "am" -> if (hour == 12) hour = 0
                "noon" -> { hour = 12; minute = 0 }
                "midnight" -> { hour = 0; minute = 0 }
            }
        }

        // Determine date
        val cal = Calendar.getInstance()
        val hasDate = TOMORROW_REGEX.containsMatchIn(text) ||
            TODAY_REGEX.containsMatchIn(text) ||
            WEEKDAY_REGEX.containsMatchIn(text)

        if (TOMORROW_REGEX.containsMatchIn(text)) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        } else if (TODAY_REGEX.containsMatchIn(text)) {
            // today — no change
        } else {
            val weekdayMatch = WEEKDAY_REGEX.find(text)
            if (weekdayMatch != null) {
                val targetDay = WEEKDAY_MAP[weekdayMatch.groupValues[2].lowercase(Locale.ROOT)]
                if (targetDay != null) {
                    val today = cal.get(Calendar.DAY_OF_WEEK)
                    var diff = (targetDay - today + 7) % 7
                    if (diff == 0) diff = 7 // next week, not today
                    if (weekdayMatch.groupValues[1].isNotBlank()) diff += 7 // "next Monday"
                    cal.add(Calendar.DAY_OF_YEAR, diff)
                }
            }
        }

        var dueAt: Long? = null
        if (hour != null) {
            val h = hour!!
            val m = minute ?: 0
            if (!hasDate && !TODAY_REGEX.containsMatchIn(text) && !TOMORROW_REGEX.containsMatchIn(text)) {
                // Time only — default to today if time hasn't passed, else tomorrow
                val now = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                if (cal.timeInMillis <= now.timeInMillis) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            } else {
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            dueAt = cal.timeInMillis
        }

        // Recurrence
        var recurrence = 0
        if (RECUR_DAILY.containsMatchIn(text)) recurrence = 1
        else if (RECUR_WEEKLY.containsMatchIn(text)) recurrence = 2
        else if (RECUR_WEEKDAYS.containsMatchIn(text)) recurrence = 3
        else if (RECUR_WEEKENDS.containsMatchIn(text)) recurrence = 4

        // Alarm
        val isAlarm = ALARM_REGEX.containsMatchIn(text)

        // Location trigger
        var triggerType: Int? = null
        var addressLabel = ""
        if (ARRIVE_REGEX.containsMatchIn(text)) {
            triggerType = 0
        } else if (LEAVE_REGEX.containsMatchIn(text)) {
            triggerType = 1
        }
        if (triggerType != null) {
            val locMatch = LOCATION_AT.find(text)
            if (locMatch != null) {
                addressLabel = locMatch.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            }
        }

        // Title: strip the parsed parts, keep the rest
        var title = text
        listOf(
            TIME_REGEX, TOMORROW_REGEX, TODAY_REGEX, WEEKDAY_REGEX,
            RECUR_DAILY, RECUR_WEEKLY, RECUR_WEEKDAYS, RECUR_WEEKENDS,
            ALARM_REGEX, ARRIVE_REGEX, LEAVE_REGEX, LOCATION_AT,
            Regex("""\b(remind\s+me\s+to|remind\s+me|reminder\s+to)\b""", RegexOption.IGNORE_CASE),
        ).forEach { r ->
            title = title.replace(r, "")
        }
        title = title.replace(Regex("""\s+"""), " ").trim().trim(',', '.', '!')
        if (title.isBlank()) title = text

        return ParsedReminder(
            title = title,
            dueAt = dueAt,
            recurrenceType = recurrence,
            isAlarm = isAlarm,
            triggerType = triggerType,
            addressLabel = addressLabel,
        )
    }
}
