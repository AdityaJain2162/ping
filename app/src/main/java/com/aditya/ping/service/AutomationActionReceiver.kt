package com.aditya.ping.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aditya.ping.data.AutomationRepository
import com.aditya.ping.data.PingDatabase
import com.aditya.ping.util.AutomationExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Executes the automation linked to a reminder when the user taps the
 * automation action button in the reminder's notification.
 */
class AutomationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val automationId = intent.getLongExtra(EXTRA_AUTOMATION_ID, -1L)
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (automationId < 0) return

        // Dismiss the reminder notification so the user sees feedback
        if (reminderId >= 0) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(reminderId.toInt())
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = AutomationRepository.from(context)
                val automation = repo.getById(automationId) ?: return@launch
                AutomationExecutor.execute(context, automation)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_AUTOMATION_ID = "automation_id"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
