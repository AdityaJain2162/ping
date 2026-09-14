package com.adityajain.geonote

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.adityajain.geonote.util.NotificationChannels
import com.google.android.gms.ads.MobileAds

class GeoNoteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // AdMob initialization — safe to call at app start; uses test app ID from manifest.
        MobileAds.initialize(this) {}
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
