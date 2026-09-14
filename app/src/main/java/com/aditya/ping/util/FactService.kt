package com.aditya.ping.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fetches a daily fact from a free, no-auth API.
 * Falls back to a built-in fact list if the network is unavailable.
 * Caches the fact per-day so it stays consistent throughout the day.
 */
object FactService {

    private const val TAG = "FactService"
    private const val PREFS = "ping_facts"
    private const val KEY_DATE = "fact_date"
    private const val KEY_TEXT = "fact_text"
    private const val API_URL = "https://uselessfacts.jsph.pl/api/v2/facts/today"

    private val fallbackFacts = listOf(
        "Honey never spoils. Archaeologists have found 3000-year-old honey in Egyptian tombs that's still edible.",
        "Octopuses have three hearts and blue blood.",
        "A day on Venus is longer than a year on Venus.",
        "Bananas are berries, but strawberries aren't.",
        "The first oranges weren't orange — they were green.",
        "A group of flamingos is called a flamboyance.",
        "Wombat poop is cube-shaped.",
        "The shortest war in history lasted 38 minutes.",
        "Sea otters hold hands while sleeping to avoid drifting apart.",
        "A single cloud can weigh over a million pounds.",
        "There are more possible chess games than atoms in the universe.",
        "Your stomach gets a new lining every 3-4 days.",
        "The Eiffel Tower can grow up to 6 inches taller in summer.",
        "Dolphins have names for each other — they use unique whistles.",
        "A bolt of lightning is 5 times hotter than the sun's surface.",
        "Cows have best friends and get stressed when separated from them.",
        "The human nose can detect over 1 trillion scents.",
        "Sharks existed before trees did.",
        "Your brain uses about 20% of your body's total energy.",
        "There's a species of jellyfish that's biologically immortal.",
    )

    suspend fun getTodayFact(context: Context): String = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Return cached fact if it's from today
        val cachedDate = prefs.getString(KEY_DATE, null)
        val cachedText = prefs.getString(KEY_TEXT, null)
        if (cachedDate == today && cachedText != null) {
            return@withContext cachedText
        }

        // Try fetching from API
        try {
            val url = URL(API_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Ping/1.0 (android reminder app)")
            }
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = org.json.JSONObject(response)
            val fact = json.optString("text").trim().removeSuffix(".")
            if (fact.isNotBlank()) {
                prefs.edit()
                    .putString(KEY_DATE, today)
                    .putString(KEY_TEXT, fact)
                    .apply()
                return@withContext fact
            }
        } catch (e: Exception) {
            Log.d(TAG, "API fetch failed, using fallback: ${e.message}")
        }

        // Fallback: deterministic fact based on day of year
        val dayOfYear = SimpleDateFormat("D", Locale.getDefault()).format(Date()).toInt()
        val fact = fallbackFacts[dayOfYear % fallbackFacts.size]
        prefs.edit()
            .putString(KEY_DATE, today)
            .putString(KEY_TEXT, fact)
            .apply()
        return@withContext fact
    }
}
