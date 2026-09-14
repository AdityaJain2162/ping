package com.aditya.ping.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    /** trigger type: 0=location_arrive, 1=location_leave, 2=wifi_connect, 3=wifi_disconnect,
     *  4=bluetooth_connect, 5=nfc_tag, 6=webhook */
    val triggerType: Int,
    /** trigger data: lat,lng,radius for location; SSID for wifi; device address for bluetooth;
     *  NFC tag ID for NFC; webhook path for webhook */
    val triggerData: String,
    /** action type: 0=notification, 1=call, 2=whatsapp, 3=sms, 4=open_app, 5=navigate, 6=url,
     *  7=toggle_wifi, 8=toggle_bluetooth, 9=silent_mode, 10=volume, 11=webhook */
    val actionType: Int,
    /** action data: phone number, package name, URL, volume level, webhook URL */
    val actionData: String = "",
    /** action message for WhatsApp/SMS */
    val actionMessage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
