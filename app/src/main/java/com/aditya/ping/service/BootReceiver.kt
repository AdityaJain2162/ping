package com.aditya.ping.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aditya.ping.data.PingDatabase
import com.aditya.ping.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON" &&
            intent.action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) return

        val appContext = context.applicationContext

        // 1. Restart geofence monitoring service
        try {
            val service = Intent(appContext, GeofenceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(service)
            } else {
                appContext.startService(service)
            }
        } catch (e: Exception) {
            android.util.Log.e("BootReceiver", "Failed to start GeofenceService: ${e.message}")
        }

        // 2. Re-schedule all time-based reminders + alarms
        CoroutineScope(Dispatchers.IO).launch {
            val reminders = PingDatabase.get(appContext).reminderDao().getEnabledWithTimeTrigger()
            AlarmScheduler.rescheduleAll(appContext, reminders)
        }
    }
}
