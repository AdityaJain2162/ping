package com.aditya.ping.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ReminderEntity::class, SavedPlaceEntity::class, ReminderListEntity::class, AutomationEntity::class],
    version = 12,
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
                            // Pre-load sample reminders on first database creation
                            INSTANCE?.let { instance ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    prepopulate(instance.reminderDao())
                                }
                            }
                        }
                    })
                    .build().also { INSTANCE = it }
            }

        private suspend fun prepopulate(dao: ReminderDao) {
            val now = System.currentTimeMillis()

            // Morning alarm — 7:00 AM, daily, turned off by default
            val morningAlarm = nextTimeTodayOrTomorrow(7, 0)
            dao.insert(
                ReminderEntity(
                    title = "Morning Alarm",
                    note = "Time to wake up and start the day",
                    lat = 0.0,
                    lng = 0.0,
                    dueAt = morningAlarm,
                    isAlarm = true,
                    snoozeMinutes = 10,
                    recurrenceType = 1, // daily
                    enabled = false,
                ),
            )

            // Work reminder — 9:00 AM, weekdays, turned off by default
            val workReminder = nextTimeTodayOrTomorrow(9, 0)
            dao.insert(
                ReminderEntity(
                    title = "Leave for Work",
                    note = "Don't forget your keys and badge",
                    lat = 0.0,
                    lng = 0.0,
                    dueAt = workReminder,
                    isAlarm = false,
                    snoozeMinutes = 5,
                    recurrenceType = 3, // weekdays
                    enabled = false,
                ),
            )

            // Evening reminder — 10:00 PM, daily, turned off by default
            val eveningReminder = nextTimeTodayOrTomorrow(22, 0)
            dao.insert(
                ReminderEntity(
                    title = "Wind Down",
                    note = "Put the phone away and get ready for bed",
                    lat = 0.0,
                    lng = 0.0,
                    dueAt = eveningReminder,
                    isAlarm = false,
                    snoozeMinutes = 5,
                    recurrenceType = 1, // daily
                    nagMode = false,
                    enabled = false,
                ),
            )

            // Location reminder — example, turned off by default
            dao.insert(
                ReminderEntity(
                    title = "Buy groceries",
                    note = "Milk, eggs, bread — tap to set your store location",
                    lat = 0.0,
                    lng = 0.0,
                    addressLabel = "Tap to set location",
                    radiusMeters = 200,
                    triggerType = 0, // on arrival
                    dueAt = null,
                    enabled = false,
                ),
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
