package com.aditya.ping.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ReminderEntity::class, SavedPlaceEntity::class, ReminderListEntity::class, AutomationEntity::class],
    version = 15,
    exportSchema = false,
)
abstract class PingDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun reminderListDao(): ReminderListDao
    abstract fun automationDao(): AutomationDao

    companion object {
        @Volatile private var INSTANCE: PingDatabase? = null

        fun get(context: Context): PingDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PingDatabase::class.java,
                    "ping.db",
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Insert preloaded reminders directly via SQL — INSTANCE is null
                            // at this point because .also { INSTANCE = it } hasn't run yet
                            prepopulateViaSql(db)
                        }
                    })
                    .build().also { INSTANCE = it }
            }

        private fun prepopulateViaSql(db: SupportSQLiteDatabase) {
            val morningAlarm = nextTimeTodayOrTomorrow(7, 0)
            val workReminder = nextTimeTodayOrTomorrow(9, 0)
            val eveningReminder = nextTimeTodayOrTomorrow(22, 0)
            val now = System.currentTimeMillis()

            // Morning Alarm — 7:00 AM, daily, full-screen alarm, off by default
            db.execSQL(
                """INSERT INTO reminders (title, note, lat, lng, addressLabel, radiusMeters,
                triggerType, dueAt, isAlarm, snoozeMinutes, recurrenceType, recurrenceInterval,
                recurrenceEndDate, nagMode, nagIntervalMinutes, listId, triggerMode,
                automationId, ringtoneUri,
                antiSleepDismiss, createdAt, enabled, completed, completedAt, lastFiredAt)
                VALUES (?, ?, 0, 0, '', 150, 0, ?, 1, 10, 1, 1, NULL, 0, 15, NULL, 0, NULL, '', 0, ?, 0, 0, NULL, 0)""",
                arrayOf("Morning Alarm", "Time to wake up and start the day", morningAlarm, now),
            )

            // Leave for Work — 9:00 AM, weekdays, notification, off by default
            db.execSQL(
                """INSERT INTO reminders (title, note, lat, lng, addressLabel, radiusMeters,
                triggerType, dueAt, isAlarm, snoozeMinutes, recurrenceType, recurrenceInterval,
                recurrenceEndDate, nagMode, nagIntervalMinutes, listId, triggerMode,
                automationId, ringtoneUri,
                antiSleepDismiss, createdAt, enabled, completed, completedAt, lastFiredAt)
                VALUES (?, ?, 0, 0, '', 150, 0, ?, 0, 5, 3, 1, NULL, 0, 15, NULL, 0, NULL, '', 0, ?, 0, 0, NULL, 0)""",
                arrayOf("Leave for Work", "Don't forget your keys and badge", workReminder, now),
            )

            // Wind Down — 10:00 PM, daily, notification, off by default
            db.execSQL(
                """INSERT INTO reminders (title, note, lat, lng, addressLabel, radiusMeters,
                triggerType, dueAt, isAlarm, snoozeMinutes, recurrenceType, recurrenceInterval,
                recurrenceEndDate, nagMode, nagIntervalMinutes, listId, triggerMode,
                automationId, ringtoneUri,
                antiSleepDismiss, createdAt, enabled, completed, completedAt, lastFiredAt)
                VALUES (?, ?, 0, 0, '', 150, 0, ?, 0, 5, 1, 1, NULL, 0, 15, NULL, 0, NULL, '', 0, ?, 0, 0, NULL, 0)""",
                arrayOf("Wind Down", "Put the phone away and get ready for bed", eveningReminder, now),
            )

            // Buy groceries — location reminder, off by default
            db.execSQL(
                """INSERT INTO reminders (title, note, lat, lng, addressLabel, radiusMeters,
                triggerType, dueAt, isAlarm, snoozeMinutes, recurrenceType, recurrenceInterval,
                recurrenceEndDate, nagMode, nagIntervalMinutes, listId, triggerMode,
                automationId, ringtoneUri,
                antiSleepDismiss, createdAt, enabled, completed, completedAt, lastFiredAt)
                VALUES (?, ?, 0, 0, ?, 200, 0, NULL, 0, 5, 0, 1, NULL, 0, 15, NULL, 0, NULL, '', 0, ?, 0, 0, NULL, 0)""",
                arrayOf("Buy groceries", "Milk, eggs, bread — tap to set your store location", "Tap to set location", now),
            )
        }

        /** Returns epoch millis for the given hour:minute today, or tomorrow if already passed. */
        private fun nextTimeTodayOrTomorrow(hour: Int, minute: Int): Long {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }
            return cal.timeInMillis
        }
    }
}
