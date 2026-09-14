package com.aditya.ping

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.aditya.ping.util.NotificationChannels

class PingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AdInitializer.init(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                NotificationChannels.GEOFENCE,
                getString(R.string.notif_channel_geofence),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = getString(R.string.notif_channel_geofence_desc) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationChannels.SERVICE,
                getString(R.string.notif_channel_service),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.notif_channel_service_desc) },
        )
    }
}
