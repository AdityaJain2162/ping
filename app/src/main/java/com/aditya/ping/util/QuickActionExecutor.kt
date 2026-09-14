package com.aditya.ping.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.aditya.ping.data.ReminderEntity

/**
 * Executes a quick action associated with a reminder.
 * Types: 0=none, 1=call, 2=whatsapp, 3=open app, 4=navigate, 5=url
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
        return when (reminder.quickActionType) {
            // Call
            1 -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:${reminder.quickActionData}"))
            // WhatsApp message
            2 -> Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${reminder.quickActionData}"))
            // Open app
            3 -> Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                `package` = reminder.quickActionData
            }
            // Navigate to location
            4 -> Intent(Intent.ACTION_VIEW, Uri.parse(
                "geo:${reminder.lat},${reminder.lng}?q=${reminder.lat},${reminder.lng}(${reminder.title})"
            ))
            // Open URL
            5 -> Intent(Intent.ACTION_VIEW, Uri.parse(reminder.quickActionData))
            else -> null
        }
    }

    fun actionLabel(type: Int): String = when (type) {
        1 -> "Call"
        2 -> "WhatsApp"
        3 -> "Open app"
        4 -> "Navigate"
        5 -> "Open URL"
        else -> ""
    }
}
