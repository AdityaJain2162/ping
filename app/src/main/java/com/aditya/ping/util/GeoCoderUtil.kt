package com.aditya.ping.util

import android.content.Context
import android.location.Geocoder
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class GeoResult(
    val label: String,
    val lat: Double,
    val lng: Double,
)

class GeoCoderUtil(private val context: Context) {

    suspend fun search(query: String): List<GeoResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val geocoder = Geocoder(context, Locale.getDefault())
        val results = mutableListOf<GeoResult>()
        try {
            val addresses = geocoder.getFromLocationName(query, 10)
            addresses?.forEach { addr ->
                results.add(
                    GeoResult(
                        label = buildString {
                            addr.getAddressLine(0)?.let { append(it) }
                        }.ifBlank { addr.featureName ?: query },
                        lat = addr.latitude,
                        lng = addr.longitude,
                    ),
                )
            }
        } catch (e: Exception) {
            // Geocoder may fail on some devices/emulators without network
        }
        results
    }
}
