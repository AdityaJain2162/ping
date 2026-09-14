package com.aditya.ping.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ReminderEntity::class], version = 5, exportSchema = false)
abstract class PingDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: PingDatabase? = null

        fun get(context: Context): PingDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PingDatabase::class.java,
                    "ping.db",
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
