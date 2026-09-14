package com.aditya.ping.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aditya.ping.data.AutomationRepository
import com.aditya.ping.util.AutomationExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for Bluetooth device connections and triggers automations.
 * Trigger type: 4=bluetooth_connect
 */
class BluetoothTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> handleConnect(context, intent)
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> handleDisconnect(context, intent)
        }
    }

    private fun handleConnect(context: Context, intent: Intent) {
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
        val deviceName = device.name ?: ""
        val deviceAddr = device.address ?: ""
        Log.d("BtTrigger", "Connected: $deviceName ($deviceAddr)")

        CoroutineScope(Dispatchers.IO).launch {
            val repo = AutomationRepository.from(context)
            val automations = repo.getByTriggerType(4)
            automations.forEach { automation ->
                val trigger = automation.triggerData
                if (trigger.isBlank() ||
                    trigger.equals(deviceName, ignoreCase = true) ||
                    trigger.equals(deviceAddr, ignoreCase = true)
                ) {
                    AutomationExecutor.execute(context, automation)
                }
            }
        }
    }

    private fun handleDisconnect(context: Context, intent: Intent) {
        // Could add trigger type for disconnect in the future
    }
}
