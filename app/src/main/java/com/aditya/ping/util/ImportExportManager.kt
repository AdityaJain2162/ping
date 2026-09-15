package com.aditya.ping.util

import android.content.Context
import com.aditya.ping.data.PingDatabase
import com.aditya.ping.data.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * JSON import/export for Ping data (reminders, lists, saved places).
 * Uses org.json (built into Android) — no extra dependencies.
 */
class ImportExportManager(private val context: Context) {

    suspend fun export(): String = withContext(Dispatchers.IO) {
        val db = PingDatabase.get(context)
        val reminders = db.reminderDao().observeAll().first()
        val places = db.savedPlaceDao().observeAll().first()

        val backup = JSONObject()
        backup.put("version", 2)

        val remindersArray = JSONArray()
        reminders.forEach { r ->
            remindersArray.put(JSONObject().apply {
                put("title", r.title)
                put("note", r.note)
                put("lat", r.lat)
                put("lng", r.lng)
                put("addressLabel", r.addressLabel)
                put("radiusMeters", r.radiusMeters)
                put("triggerType", r.triggerType)
                put("triggerMode", r.triggerMode)
                put("createdAt", r.createdAt)
                put("enabled", r.enabled)
                put("completed", r.completed)
                put("dueAt", r.dueAt ?: JSONObject.NULL)
                put("isAlarm", r.isAlarm)
                put("snoozeMinutes", r.snoozeMinutes)
                put("recurrenceType", r.recurrenceType)
                put("recurrenceInterval", r.recurrenceInterval)
                put("recurrenceEndDate", r.recurrenceEndDate ?: JSONObject.NULL)
                put("nagMode", r.nagMode)
                put("nagIntervalMinutes", r.nagIntervalMinutes)
                put("listId", r.listId ?: JSONObject.NULL)
                put("automationId", r.automationId ?: JSONObject.NULL)
                put("ringtoneUri", r.ringtoneUri)
                put("lastFiredAt", r.lastFiredAt)
            })
        }
        backup.put("reminders", remindersArray)

        val placesArray = JSONArray()
        places.forEach { p ->
            placesArray.put(JSONObject().apply {
                put("name", p.name)
                put("lat", p.lat)
                put("lng", p.lng)
                put("addressLabel", p.addressLabel)
                put("radiusMeters", p.radiusMeters)
                put("createdAt", p.createdAt)
            })
        }
        backup.put("savedPlaces", placesArray)

        backup.toString(2)
    }

    suspend fun import(jsonString: String): Int = withContext(Dispatchers.IO) {
        val backup = try {
            JSONObject(jsonString)
        } catch (e: JSONException) {
            return@withContext -1
        }
        val db = PingDatabase.get(context)

        // Import saved places
        val placesArray = backup.optJSONArray("savedPlaces") ?: JSONArray()
        for (i in 0 until placesArray.length()) {
            val obj = placesArray.getJSONObject(i)
            db.savedPlaceDao().insert(
                com.aditya.ping.data.SavedPlaceEntity(
                    name = obj.getString("name"),
                    lat = obj.getDouble("lat"),
                    lng = obj.getDouble("lng"),
                    addressLabel = obj.optString("addressLabel", ""),
                    radiusMeters = obj.optInt("radiusMeters", 150),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                ),
            )
        }

        // Import reminders
        val remindersArray = backup.optJSONArray("reminders") ?: JSONArray()
        var imported = 0
        for (i in 0 until remindersArray.length()) {
            val obj = remindersArray.getJSONObject(i)
            val oldListId = if (obj.isNull("listId")) null else obj.optLong("listId", 0L).takeIf { it > 0 }

            val reminder = ReminderEntity(
                title = obj.getString("title"),
                note = obj.optString("note", ""),
                lat = obj.optDouble("lat", 0.0),
                lng = obj.optDouble("lng", 0.0),
                addressLabel = obj.optString("addressLabel", ""),
                radiusMeters = obj.optInt("radiusMeters", 150),
                triggerType = obj.optInt("triggerType", 0),
                triggerMode = obj.optInt("triggerMode", 0),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                enabled = obj.optBoolean("enabled", true),
                completed = obj.optBoolean("completed", false),
                dueAt = if (obj.isNull("dueAt")) null else obj.optLong("dueAt", 0L).takeIf { it > 0 },
                isAlarm = obj.optBoolean("isAlarm", false),
                snoozeMinutes = obj.optInt("snoozeMinutes", 10),
                recurrenceType = obj.optInt("recurrenceType", 0),
                recurrenceInterval = obj.optInt("recurrenceInterval", 1),
                recurrenceEndDate = if (obj.isNull("recurrenceEndDate")) null else obj.optLong("recurrenceEndDate", 0L).takeIf { it > 0 },
                nagMode = obj.optBoolean("nagMode", false),
                nagIntervalMinutes = obj.optInt("nagIntervalMinutes", 15),
                listId = null, // Lists feature removed
                automationId = if (obj.isNull("automationId")) null else obj.optLong("automationId", 0L).takeIf { it > 0 },
                ringtoneUri = obj.optString("ringtoneUri", ""),
                lastFiredAt = obj.optLong("lastFiredAt", 0L),
            )
            val newReminderId = db.reminderDao().insert(reminder)

            // Re-schedule alarm if the reminder has a future dueAt and is active
            if (reminder.enabled && !reminder.completed && reminder.dueAt != null &&
                reminder.dueAt > System.currentTimeMillis()
            ) {
                AlarmScheduler.schedule(context, reminder.copy(id = newReminderId))
            }
            imported++
        }
        imported
    }
}
