package com.aditya.ping.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aditya.ping.data.PingDatabase
import com.aditya.ping.util.AlarmScheduler
import com.aditya.ping.util.NagScheduler
import com.aditya.ping.util.RecurrenceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles notification action buttons: Done, Snooze, Defer to tomorrow.
 * Action type is passed via [EXTRA_ACTION].
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val action = intent.getIntExtra(EXTRA_ACTION, ACTION_DONE)
        if (reminderId < 0) return

        // Cancel the notification
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(reminderId.toInt())

        // Stop nagging
        NagScheduler.cancel(context, reminderId)

        CoroutineScope(Dispatchers.IO).launch {
            val dao = PingDatabase.get(context).reminderDao()
            val reminder = dao.getById(reminderId) ?: return@launch

            when (action) {
                ACTION_DONE -> {
                    // Mark completed + schedule next recurrence if recurring
                    val next = RecurrenceCalculator.nextOccurrence(reminder, System.currentTimeMillis())
                    if (next != null) {
                        dao.update(reminder.copy(dueAt = next, enabled = true, completed = false))
                        AlarmScheduler.schedule(context, reminder.copy(dueAt = next, completed = false))
                    } else {
                        dao.setCompleted(reminderId, true)
                        AlarmScheduler.cancel(context, reminderId)
                    }
                }
                ACTION_SNOOZE -> {
                    val snoozeMillis = reminder.snoozeMinutes * 60_000L
                    val newDueAt = System.currentTimeMillis() + snoozeMillis
                    dao.update(reminder.copy(dueAt = newDueAt))
                    AlarmScheduler.schedule(context, reminder.copy(dueAt = newDueAt))
                }
                ACTION_DEFER -> {
                    // Defer to tomorrow same time
                    val cal = java.util.Calendar.getInstance()
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    val newDueAt = cal.timeInMillis
                    dao.update(reminder.copy(dueAt = newDueAt))
                    AlarmScheduler.schedule(context, reminder.copy(dueAt = newDueAt))
                }
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_ACTION = "action_type"
        const val ACTION_DONE = 0
        const val ACTION_SNOOZE = 1
        const val ACTION_DEFER = 2
    }
}
