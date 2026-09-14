package com.aditya.ping.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log
import com.aditya.ping.data.AutomationRepository
import com.aditya.ping.util.AutomationExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for WiFi connectivity changes and triggers automations.
 * Trigger types: 2=wifi_connect, 3=wifi_disconnect
 */
class WifiTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WifiManager.NETWORK_STATE_CHANGED_ACTION) return

        val networkInfo = intent.getParcelableExtra<android.net.NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
            ?: return
        val ssid = extractSsid(context)

        Log.d("WifiTrigger", "State=${networkInfo.state} SSID=$ssid")

        CoroutineScope(Dispatchers.IO).launch {
            val repo = AutomationRepository.from(context)
            when (networkInfo.state) {
                android.net.NetworkInfo.State.CONNECTED -> {
                    val automations = repo.getByTriggerType(2)
                    automations.forEach { automation ->
                        if (automation.triggerData.isBlank() || automation.triggerData.equals(ssid, ignoreCase = true)) {
                            AutomationExecutor.execute(context, automation)
                        }
                    }
                }
                android.net.NetworkInfo.State.DISCONNECTED -> {
                    val automations = repo.getByTriggerType(3)
                    automations.forEach { automation ->
                        if (automation.triggerData.isBlank() || automation.triggerData.equals(ssid, ignoreCase = true)) {
                            AutomationExecutor.execute(context, automation)
                        }
                    }
                }
                else -> {}
            }
        }
    }

    private fun extractSsid(context: Context): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return ""
        val ssid = wifiManager.connectionInfo?.ssid ?: return ""
        // SSID comes wrapped in quotes: "MyWiFi" → MyWiFi
        return ssid.removePrefix("\"").removeSuffix("\"")
    }
}
