package com.aditya.ping.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.aditya.ping.R
import com.aditya.ping.data.PingDatabase
import com.aditya.ping.util.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (id < 0) return

        val title = intent.getStringExtra(EXTRA_TITLE) ?: context.getString(R.string.notif_time_title)
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()

        showNotification(context, id.toInt(), title, note)

        // Mark as fired in DB (cooldown tracking)
        CoroutineScope(Dispatchers.IO).launch {
            PingDatabase.get(context).reminderDao().markFired(id, System.currentTimeMillis())
        }
    }

    private fun showNotification(context: Context, notifId: Int, title: String, note: String) {
        val builder = NotificationCompat.Builder(context, NotificationChannels.GEOFENCE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

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
