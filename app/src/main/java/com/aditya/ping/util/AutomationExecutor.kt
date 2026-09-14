package com.aditya.ping.util

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aditya.ping.R
import com.aditya.ping.data.AutomationEntity

/**
 * Executes the action associated with an automation.
 * Action types: 0=notification, 1=call, 2=whatsapp, 3=sms, 4=open_app, 5=navigate,
 *              6=url, 7=toggle_wifi, 8=toggle_bluetooth, 9=silent_mode, 10=volume, 11=webhook
 */
object AutomationExecutor {

    private const val TAG = "AutomationExecutor"

    fun execute(context: Context, automation: AutomationEntity): Boolean {
        Log.d(TAG, "Executing automation: ${automation.name} action=${automation.actionType}")
        return when (automation.actionType) {
            0 -> showNotification(context, automation)
            1 -> dialNumber(context, automation.actionData)
            2 -> openWhatsApp(context, automation.actionData, automation.actionMessage)
            3 -> sendSms(context, automation.actionData, automation.actionMessage)
            4 -> openApp(context, automation.actionData)
            5 -> navigate(context, automation.triggerData)
            6 -> openUrl(context, automation.actionData)
            7 -> toggleWifi(context)
            8 -> toggleBluetooth(context)
            9 -> toggleSilentMode(context)
            10 -> setVolume(context, automation.actionData)
            11 -> fireWebhook(automation.actionData)
            else -> false
        }
    }

    private fun showNotification(context: Context, automation: AutomationEntity): Boolean {
        val notifId = automation.id.toInt() + 50000
        val notif = NotificationCompat.Builder(context, NotificationChannels.GEOFENCE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(automation.name)
            .setContentText("Automation triggered")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        manager.notify(notifId, notif)
        return true
    }

    private fun dialNumber(context: Context, number: String): Boolean {
        return try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) { false }
    }

    private fun openWhatsApp(context: Context, number: String, message: String): Boolean {
        return try {
            val url = if (message.isNotBlank()) "https://wa.me/$number?text=${Uri.encode(message)}"
            else "https://wa.me/$number"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                `package` = "com.whatsapp"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (e: Exception) { false }
    }

    private fun sendSms(context: Context, number: String, message: String): Boolean {
        return try {
            val uri = if (message.isNotBlank()) "smsto:$number?body=${Uri.encode(message)}"
            else "smsto:$number"
            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) { false }
    }

    private fun openApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }

    private fun navigate(context: Context, triggerData: String): Boolean {
        return try {
            val parts = triggerData.split(",")
            if (parts.size < 2) return false
            val lat = parts[0].toDoubleOrNull() ?: return false
            val lng = parts[1].toDoubleOrNull() ?: return false
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) { false }
    }

    private fun openUrl(context: Context, url: String): Boolean {
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) { false }
    }

    private fun toggleWifi(context: Context): Boolean {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
            } else {
                context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            true
        } catch (e: Exception) { false }
    }

    private fun toggleBluetooth(context: Context): Boolean {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return false
            val adapter = bluetoothManager.adapter ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION")
                if (adapter.isEnabled) adapter.disable() else adapter.enable()
            } else {
                context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            true
        } catch (e: Exception) { false }
    }

    private fun toggleSilentMode(context: Context): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                audioManager.ringerMode = if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL)
                    AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
            } else {
                if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                } else {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
            }
            true
        } catch (e: Exception) { false }
    }

    private fun setVolume(context: Context, levelStr: String): Boolean {
        return try {
            val level = levelStr.toIntOrNull() ?: return false
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = (level * max / 100).coerceIn(0, max)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            true
        } catch (e: Exception) { false }
    }

    private fun fireWebhook(url: String): Boolean {
        if (url.isBlank()) return false
        Thread {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.responseCode
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Webhook failed: ${e.message}")
            }
        }.start()
        return true
    }
}
