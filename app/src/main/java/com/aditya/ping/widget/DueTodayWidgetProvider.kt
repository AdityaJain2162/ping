package com.aditya.ping.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.aditya.ping.MainActivity
import com.aditya.ping.R
import com.aditya.ping.data.PingDatabase
import com.aditya.ping.data.ReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DueTodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            updateWidget(context, appWidgetManager, id)
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = PingDatabase.get(context).reminderDao()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = cal.timeInMillis

            val today = dao.observeByDateRange(startOfDay, endOfDay)
            // Single-shot query for widget — use a snapshot
            val reminders = kotlinx.coroutines.runBlocking {
                dao.getEnabledWithTimeTrigger().filter {
                    it.dueAt != null && it.dueAt in startOfDay until endOfDay
                }
            }

            val views = RemoteViews(context.packageName, R.layout.widget_due_today)
            val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())

            if (reminders.isEmpty()) {
                views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_no_reminders))
                views.setTextViewText(R.id.widget_subtitle, context.getString(R.string.widget_no_reminders_desc))
                views.setTextViewText(R.id.widget_item_1, "")
                views.setTextViewText(R.id.widget_item_2, "")
                views.setTextViewText(R.id.widget_item_3, "")
            } else {
                views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_due_today))
                views.setTextViewText(
                    R.id.widget_subtitle,
                    "${reminders.size} ${context.getString(R.string.widget_reminders)}",
                )
                val sorted = reminders.sortedBy { it.dueAt }
                val items = sorted.take(3)
                val ids = listOf(R.id.widget_item_1, R.id.widget_item_2, R.id.widget_item_3)
                ids.forEachIndexed { i, viewId ->
                    if (i < items.size) {
                        val r = items[i]
                        val time = r.dueAt?.let { timeFmt.format(Date(it)) } ?: ""
                        val prefix = if (r.isAlarm) "🔔 " else "⏰ "
                        views.setTextViewText(viewId, "$prefix$time  ${r.title}")
                    } else {
                        views.setTextViewText(viewId, "")
                    }
                }
            }

            // Tap to open app
            val openIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val openPi = PendingIntent.getActivity(
                context, widgetId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPi)

            // Quick-add button
            val addIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_QUICK_ADD, true)
            }
            val addPi = PendingIntent.getActivity(
                context, widgetId + 1000, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_add_button, addPi)

            manager.updateAppWidget(widgetId, views)
        }
    }

    companion object {
        const val EXTRA_QUICK_ADD = "quick_add"
    }
}
