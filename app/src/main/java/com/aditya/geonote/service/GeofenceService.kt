package com.aditya.geonote.service

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aditya.geonote.MainActivity
import com.aditya.geonote.R
import com.aditya.geonote.data.ReminderEntity
import com.aditya.geonote.data.ReminderRepository
import com.aditya.geonote.util.LocationUtil
import com.aditya.geonote.util.NotificationChannels
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground service that monitors the user's location and fires a notification
 * when the user enters or leaves a reminder's radius.
 *
 * Uses the FusedLocationProviderClient with a ~60s interval to balance battery
 * and responsiveness. Each enabled reminder is checked against the latest fix.
 */
class GeofenceService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repo: ReminderRepository
    private lateinit var fused: FusedLocationProviderClient
    private var monitoringJob: Job? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            checkReminders(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repo = ReminderRepository.from(this)
        fused = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildServiceNotification(0))
        startMonitoring()
        return START_STICKY
    }

    @androidx.annotation.RequiresPermission(anyOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ])
    private fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        monitoringJob = scope.launch {
            val reminders = repo.getEnabled()
            withContext(Dispatchers.Main) {
                refreshForegroundNotification(reminders.size)
                if (hasLocationPermission()) {
                    val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60_000L)
                        .setMinUpdateIntervalMillis(30_000L)
                        .build()
                    fused.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
                }
            }
        }
    }

    private fun checkReminders(location: Location) {
        scope.launch {
            val reminders = repo.getEnabled()
            val now = System.currentTimeMillis()
            for (r in reminders) {
                if (isInCooldown(r, now)) continue
                val dist = LocationUtil.distanceMeters(location.latitude, location.longitude, r.lat, r.lng)
                val inside = dist <= r.radiusMeters
                val shouldFire = when (r.triggerType) {
                    TRIGGER_ARRIVE -> inside
                    TRIGGER_LEAVE -> !inside
                    else -> false
                }
                if (shouldFire) {
                    fireNotification(r)
                    repo.markFired(r.id, now)
                }
            }
        }
    }

    private fun isInCooldown(r: ReminderEntity, now: Long): Boolean {
        if (r.lastFiredAt == 0L) return false
        // 10-minute cooldown to avoid repeated fires while lingering at the boundary.
        return now - r.lastFiredAt < COOLDOWN_MS
    }

    private fun fireNotification(r: ReminderEntity) {
        val title = if (r.triggerType == TRIGGER_ARRIVE)
            getString(R.string.notif_arrival_title)
        else
            getString(R.string.notif_departure_title)

        val text = if (r.note.isNotBlank()) "${r.title} — ${r.note}" else r.title

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(
            this, r.id.toInt(), openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notif = NotificationCompat.Builder(this, NotificationChannels.GEOFENCE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(r.id.toInt(), notif)
    }

    private fun buildServiceNotification(reminderCount: Int): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, NotificationChannels.SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notif_service_title))
            .setContentText(getString(R.string.notif_service_text, reminderCount))
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun refreshForegroundNotification(count: Int) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIF_ID, buildServiceNotification(count))
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        if (hasLocationPermission()) {
            fused.removeLocationUpdates(locationCallback)
        }
        monitoringJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIF_ID = 1001
        const val TRIGGER_ARRIVE = 0
        const val TRIGGER_LEAVE = 1
        private const val COOLDOWN_MS = 10 * 60 * 1000L
    }
}
