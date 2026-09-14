package com.aditya.ping.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_lists")
data class ReminderListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int = 0, // ARGB color for the list tag
    val createdAt: Long = System.currentTimeMillis(),
)
