package com.aditya.ping.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val lat: Double,
    val lng: Double,
    val addressLabel: String = "",
    /** meters */
    val radiusMeters: Int = 150,
    /** 0 = on arrival, 1 = on departure */
    val triggerType: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val enabled: Boolean = true,
    /** last fired epoch millis (cooldown) */
    val lastFiredAt: Long = 0L,
)
