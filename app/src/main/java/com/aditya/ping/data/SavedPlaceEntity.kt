package com.aditya.ping.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lat: Double,
    val lng: Double,
    val addressLabel: String = "",
    val radiusMeters: Int = 150,
    val createdAt: Long = System.currentTimeMillis(),
)
