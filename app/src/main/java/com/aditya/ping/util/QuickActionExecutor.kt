package com.aditya.ping.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.aditya.ping.data.ReminderEntity

/**
 * Executes a quick action associated with a reminder.
 * Types: 0=none, 1=call, 2=whatsapp, 3=open app, 4=navigate, 5=url, 6=sms, 7=whatsapp_group
 */
object QuickActionExecutor {

    fun execute(context: Context, reminder: ReminderEntity): Boolean {
        if (reminder.quickActionType == 0 || reminder.quickActionData.isBlank()) return false

        val intent = createIntent(reminder) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun createIntent(reminder: ReminderEntity): Intent? {
        val data = reminder.quickActionData
        val message = reminder.quickActionMessage
        return when (reminder.quickActionType) {
            // Call
            1 -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:$data"))
            // WhatsApp message (with optional pre-filled message)
            2 -> {
                val url = if (message.isNotBlank()) {
                    "https://wa.me/$data?text=${Uri.encode(message)}"
                } else {
                    "https://wa.me/$data"
                }
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    `package` = "com.whatsapp"
                }
            }
            // Open app
            3 -> Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                `package` = data
            }
            // Navigate to location
            4 -> Intent(Intent.ACTION_VIEW, Uri.parse(
                "geo:${reminder.lat},${reminder.lng}?q=${reminder.lat},${reminder.lng}(${reminder.title})"
            ))
            // Open URL — normalize to https:// if no scheme present
            5 -> {
                val url = if (data.startsWith("http://") || data.startsWith("https://")) data
                else "https://$data"
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }
            // SMS (with optional pre-filled message)
            6 -> {
                val uri = if (message.isNotBlank()) {
                    "smsto:$data?body=${Uri.encode(message)}"
                } else {
                    "smsto:$data"
                }
                Intent(Intent.ACTION_SENDTO, Uri.parse(uri))
            }
            // WhatsApp group (data = group invite code, e.g., "abc123XYZ")
            7 -> {
                val url = if (data.startsWith("https://chat.whatsapp.com/")) data
                else "https://chat.whatsapp.com/$data"
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    `package` = "com.whatsapp"
                }
            }
            else -> null
        }
    }

    fun actionLabel(type: Int): String = when (type) {
        1 -> "Call"
        2 -> "WhatsApp"
        3 -> "Open app"
        4 -> "Navigate"
        5 -> "Open URL"
        6 -> "SMS"
        7 -> "WhatsApp Group"
        else -> ""
    }
}
