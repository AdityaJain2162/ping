package com.aditya.ping.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.aditya.ping.R
import com.aditya.ping.data.PingDatabase
import com.aditya.ping.util.NagScheduler
import com.aditya.ping.util.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NagReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (id < 0) return

        CoroutineScope(Dispatchers.IO).launch {
            val dao = PingDatabase.get(context).reminderDao()
            val reminder = dao.getById(id)

            // Only nag if reminder is still enabled, not completed, and nag mode is on
            if (reminder == null || !reminder.enabled || reminder.completed || !reminder.nagMode) return@launch

            showNagNotification(context, id.toInt(), reminder.title, reminder.note, reminder.id)

            // Schedule the next nag
            NagScheduler.scheduleNext(context, reminder)
        }
    }

    private fun showNagNotification(context: Context, notifId: Int, title: String, note: String, reminderId: Long) {
        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationActionReceiver.EXTRA_ACTION, NotificationActionReceiver.ACTION_DONE)
        }
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationActionReceiver.EXTRA_ACTION, NotificationActionReceiver.ACTION_SNOOZE)
        }
        val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val donePi = PendingIntent.getBroadcast(context, notifId, doneIntent, flag)
        val snoozePi = PendingIntent.getBroadcast(context, notifId + 10000, snoozeIntent, flag)

        val builder = NotificationCompat.Builder(context, NotificationChannels.GEOFENCE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOngoing(false)
            .addAction(0, context.getString(R.string.notif_action_done), donePi)
            .addAction(0, context.getString(R.string.notif_action_snooze), snoozePi)

        if (note.isNotBlank()) builder.setContentText(note)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, builder.build())
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_NOTE = "note"
    }
}
