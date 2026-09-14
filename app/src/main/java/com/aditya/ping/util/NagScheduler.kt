package com.aditya.ping.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aditya.ping.data.ReminderEntity
import com.aditya.ping.service.NagReceiver

/**
 * Schedules recurring nag notifications for reminders with nagMode enabled.
 * Uses inexact alarms (nag timing doesn't need to be exact).
 */
object NagScheduler {

    private fun pendingIntent(context: Context, reminder: ReminderEntity): PendingIntent {
        val intent = Intent(context, NagReceiver::class.java).apply {
            putExtra(NagReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(NagReceiver.EXTRA_TITLE, reminder.title)
            putExtra(NagReceiver.EXTRA_NOTE, reminder.note)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        // Use a unique request code offset to avoid collision with AlarmScheduler
        return PendingIntent.getBroadcast(context, reminder.id.toInt() + NAG_REQUEST_CODE_OFFSET, intent, flags)
    }

    fun scheduleNext(context: Context, reminder: ReminderEntity) {
        if (!reminder.nagMode || !reminder.enabled) return

        val intervalMillis = reminder.nagIntervalMinutes * 60_000L
        val triggerAt = System.currentTimeMillis() + intervalMillis

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Inexact is fine for nagging — saves battery
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context, reminder))
    }

    fun cancel(context: Context, reminderId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NagReceiver::class.java)
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(context, reminderId.toInt() + NAG_REQUEST_CODE_OFFSET, intent, flags) ?: return
        am.cancel(pi)
    }

    private const val NAG_REQUEST_CODE_OFFSET = 100_000
}
