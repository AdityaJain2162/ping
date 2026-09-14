package com.aditya.ping.util

import android.content.Context
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume

data class GeoResult(
    val label: String,
    val lat: Double,
    val lng: Double,
)

class GeoCoderUtil(private val context: Context) {

    private companion object {
        const val TAG = "GeoCoderUtil"
    }

    suspend fun search(query: String): List<GeoResult> {
        if (query.isBlank()) return emptyList()

        // Try Android Geocoder first
        val results = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                searchAsync(query)
            } else {
                searchSync(query)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoder failed: ${e.message}")
            emptyList()
        }

        // Fallback to Nominatim if Geocoder returns nothing
        if (results.isEmpty()) {
            Log.d(TAG, "Geocoder returned empty, falling back to Nominatim")
            return searchNominatim(query)
        }

        return results
    }

    @Suppress("DEPRECATION")
    private suspend fun searchSync(query: String): List<GeoResult> = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        val results = mutableListOf<GeoResult>()
        val addresses = geocoder.getFromLocationName(query, 10)
        addresses?.forEach { addr ->
            results.add(
                GeoResult(
                    label = addr.getAddressLine(0) ?: addr.featureName ?: query,
                    lat = addr.latitude,
                    lng = addr.longitude,
                ),
            )
        }
        results
    }

    private suspend fun searchAsync(query: String): List<GeoResult> =
        suspendCancellableCoroutine { cont ->
            val geocoder = Geocoder(context, Locale.getDefault())
            geocoder.getFromLocationName(query, 10, object : GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    val results = addresses.map { addr ->
                        GeoResult(
                            label = addr.getAddressLine(0) ?: addr.featureName ?: query,
                            lat = addr.latitude,
                            lng = addr.longitude,
                        )
                    }
                    if (cont.isActive) cont.resume(results)
                }

                override fun onError(errorMessage: String?) {
                    Log.e(TAG, "Async geocoder error: $errorMessage")
                    if (cont.isActive) cont.resume(emptyList())
                }
            })
        }

    /**
     * Fallback using OpenStreetMap Nominatim API (free, no key required).
     * Used when the Android Geocoder backend is unavailable.
     */
    private suspend fun searchNominatim(query: String): List<GeoResult> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<GeoResult>()
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = URL(
                    "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=10&addressdetails=1",
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("User-Agent", "Ping/1.0 (android reminder app)")
                }
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = org.json.JSONArray(response)
                for (i in 0 until json.length()) {
                    val item = json.getJSONObject(i)
                    val lat = item.optString("lat").toDoubleOrNull() ?: continue
                    val lng = item.optString("lon").toDoubleOrNull() ?: continue
                    val label = item.optString("display_name").ifBlank { query }
                    results.add(GeoResult(label = label, lat = lat, lng = lng))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Nominatim fallback failed: ${e.message}")
            }
            results
        }
}
