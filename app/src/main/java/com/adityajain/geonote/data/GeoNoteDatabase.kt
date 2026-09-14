package com.adityajain.geonote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
abstract class GeoNoteDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: GeoNoteDatabase? = null

        fun get(context: Context): GeoNoteDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GeoNoteDatabase::class.java,
                    "geonote.db",
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
