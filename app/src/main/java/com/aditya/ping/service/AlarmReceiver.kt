package com.aditya.ping.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aditya.ping.R
import com.aditya.ping.data.PingDatabase
import com.aditya.ping.ui.AlarmActivity
import com.aditya.ping.util.AlarmScheduler
import com.aditya.ping.util.NagScheduler
import com.aditya.ping.util.NotificationChannels
import com.aditya.ping.util.QuietHoursManager
import com.aditya.ping.util.RecurrenceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (id < 0) return

        Log.d("AlarmReceiver", "Received alarm for reminder id=$id")

        val title = intent.getStringExtra(EXTRA_TITLE) ?: context.getString(R.string.notif_time_title)
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()
        val isAlarm = intent.getBooleanExtra(EXTRA_IS_ALARM, false)

        Log.d("AlarmReceiver", "title=$title isAlarm=$isAlarm")

        // Check quiet hours — if active, defer non-alarm reminders to when quiet hours end
        CoroutineScope(Dispatchers.IO).launch {
            val dao = PingDatabase.get(context).reminderDao()
            val reminder = dao.getById(id)
            if (reminder == null || reminder.completed) return@launch

            val quietHours = QuietHoursManager(context)
            val isAlarmReminder = isAlarm

            if (!isAlarmReminder && quietHours.isCurrentlyQuiet()) {
                // Defer to end of quiet hours
                val deferTo = quietHours.nextQuietEndTimestamp()
                if (deferTo != null) {
                    val updated = reminder.copy(dueAt = deferTo)
                    dao.update(updated)
                    AlarmScheduler.schedule(context, updated)
                    return@launch
                }
            }

            // Fire immediately (alarm or non-quiet-hours)
            if (isAlarmReminder) {
                launchAlarmActivity(context, id, title, note)
            } else {
                showNotification(context, id.toInt(), title, note, reminder)
            }

            dao.markFired(id, System.currentTimeMillis())

            val next = RecurrenceCalculator.nextOccurrence(reminder, System.currentTimeMillis())
            if (next != null) {
                val updated = reminder.copy(dueAt = next, completed = false)
                dao.update(updated)
                AlarmScheduler.schedule(context, updated)
            }

            if (reminder.nagMode) {
                NagScheduler.scheduleNext(context, reminder)
            }
        }
    }

    private fun launchAlarmActivity(context: Context, id: Long, title: String, note: String) {
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(AlarmActivity.EXTRA_REMINDER_ID, id)
            putExtra(AlarmActivity.EXTRA_TITLE, title)
            putExtra(AlarmActivity.EXTRA_NOTE, note)
        }

        val fullScreenPi = PendingIntent.getActivity(
            context,
            id.toInt(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Use a full-screen intent notification — the Android-recommended way
        // to launch an activity from a BroadcastReceiver (avoids BAL block).
        // On locked devices this shows the full-screen alarm directly.
        // On unlocked devices it shows a heads-up notification that expands.
        val notifId = id.toInt() + 40000

        val builder = NotificationCompat.Builder(context, NotificationChannels.ALARM)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(note.ifBlank { context.getString(R.string.notif_channel_alarm) })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true)
            .setAutoCancel(true)
            .setOngoing(true)

        if (note.isNotBlank()) builder.setStyle(NotificationCompat.BigTextStyle().bigText(note))

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(notifId, builder.build())
            Log.d("AlarmReceiver", "Full-screen alarm notification posted for id=$id")
        } catch (e: SecurityException) {
            // Fallback: try direct activity launch (may fail with BAL, but worth trying)
            Log.w("AlarmReceiver", "Full-screen intent denied, trying direct launch: ${e.message}")
            try {
                context.startActivity(alarmIntent)
            } catch (e2: Exception) {
                Log.e("AlarmReceiver", "Direct launch also failed: ${e2.message}")
            }
        }
    }

    private fun showNotification(
        context: Context,
        notifId: Int,
        title: String,
        note: String,
        reminder: com.aditya.ping.data.ReminderEntity,
    ) {
        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, notifId.toLong())
            putExtra(NotificationActionReceiver.EXTRA_ACTION, NotificationActionReceiver.ACTION_DONE)
        }
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, notifId.toLong())
            putExtra(NotificationActionReceiver.EXTRA_ACTION, NotificationActionReceiver.ACTION_SNOOZE)
        }
        val deferIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, notifId.toLong())
            putExtra(NotificationActionReceiver.EXTRA_ACTION, NotificationActionReceiver.ACTION_DEFER)
        }

        val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val donePi = PendingIntent.getBroadcast(context, notifId, doneIntent, flag)
        val snoozePi = PendingIntent.getBroadcast(context, notifId + 10000, snoozeIntent, flag)
        val deferPi = PendingIntent.getBroadcast(context, notifId + 20000, deferIntent, flag)

        // Tapping the notification opens the app to the specific reminder
        val openIntent = Intent(context, com.aditya.ping.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(com.aditya.ping.MainActivity.EXTRA_OPEN_REMINDER_ID, reminder.id)
        }
        val openPi = PendingIntent.getActivity(
            context, notifId + 50000, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, NotificationChannels.ALARM)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentIntent(openPi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.notif_action_done), donePi)
            .addAction(0, context.getString(R.string.notif_action_snooze), snoozePi)
            .addAction(0, context.getString(R.string.notif_action_defer), deferPi)

        // Automation action button (if linked)
        if (reminder.automationId != null) {
            val autoRepo = com.aditya.ping.data.AutomationRepository.from(context)
            val automation = kotlinx.coroutines.runBlocking { autoRepo.getById(reminder.automationId) }
            if (automation != null && automation.enabled) {
                val autoIntent = Intent(context, AutomationActionReceiver::class.java).apply {
                    putExtra(AutomationActionReceiver.EXTRA_AUTOMATION_ID, automation.id)
                    putExtra(AutomationActionReceiver.EXTRA_REMINDER_ID, reminder.id)
                }
                val autoPi = PendingIntent.getBroadcast(
                    context, notifId + 30000, autoIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                builder.addAction(
                    0,
                    com.aditya.ping.util.AutomationExecutor.actionLabel(automation.actionType),
                    autoPi,
                )
            }
        }

        if (note.isNotBlank()) builder.setContentText(note)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, builder.build())
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_NOTE = "note"
        const val EXTRA_IS_ALARM = "is_alarm"
    }
}
