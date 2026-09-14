package com.aditya.ping.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aditya.ping.data.ReminderEntity
import com.aditya.ping.service.AlarmReceiver

/**
 * Schedules and cancels time-based reminders via [AlarmManager].
 * Uses [AlarmManager.setExactAndAllowWhileIdle] for Doze-mode compatibility.
 */
object AlarmScheduler {

    private fun pendingIntent(context: Context, reminder: ReminderEntity): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(AlarmReceiver.EXTRA_TITLE, reminder.title)
            putExtra(AlarmReceiver.EXTRA_NOTE, reminder.note)
            putExtra(AlarmReceiver.EXTRA_IS_ALARM, reminder.isAlarm)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, reminder.id.toInt(), intent, flags)
    }

    fun schedule(context: Context, reminder: ReminderEntity) {
        val dueAt = reminder.dueAt ?: return
        if (dueAt <= System.currentTimeMillis()) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // On Android 12+ (S), check if exact alarms are permitted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Fall back to inexact alarm — will fire approximately on time.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pendingIntent(context, reminder))
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pendingIntent(context, reminder))
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(context, reminderId.toInt(), intent, flags) ?: return
        am.cancel(pi)
    }

    /**
     * Re-schedules all enabled time-based reminders. Called after reboot or
     * when toggling a reminder back on.
     */
    suspend fun rescheduleAll(context: Context, reminders: List<ReminderEntity>) {
        reminders.filter { it.enabled && it.dueAt != null && it.dueAt > System.currentTimeMillis() }
            .forEach { schedule(context, it) }
    }
}
