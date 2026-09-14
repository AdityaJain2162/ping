package com.aditya.ping.util

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.quietHoursStore by preferencesDataStore("quiet_hours")

/**
 * Manages quiet hours settings (stored in DataStore).
 * Quiet hours are defined by start/end time in minutes from midnight.
 * If start > end, quiet hours wrap past midnight (e.g., 22:00 to 07:00).
 */
class QuietHoursManager(private val context: Context) {

    private val store = context.quietHoursStore

    val enabled: Flow<Boolean> = store.data.map { it[KEY_ENABLED] ?: false }
    val startMinutes: Flow<Int> = store.data.map { it[KEY_START] ?: 1320 } // 22:00 default
    val endMinutes: Flow<Int> = store.data.map { it[KEY_END] ?: 420 }    // 07:00 default

    suspend fun setEnabled(enabled: Boolean) {
        store.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setStartMinutes(minutes: Int) {
        store.edit { it[KEY_START] = minutes.coerceIn(0, 1439) }
    }

    suspend fun setEndMinutes(minutes: Int) {
        store.edit { it[KEY_END] = minutes.coerceIn(0, 1439) }
    }

    /**
     * Returns true if the current time is within quiet hours.
     */
    suspend fun isCurrentlyQuiet(): Boolean {
        val data = store.data.first()
        val enabled = data[KEY_ENABLED] ?: false
        if (!enabled) return false

        val start = data[KEY_START] ?: 1320
        val end = data[KEY_END] ?: 420

        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        return if (start <= end) {
            nowMinutes in start..end
        } else {
            // Wraps past midnight (e.g., 22:00 to 07:00)
            nowMinutes >= start || nowMinutes <= end
        }
    }

    /**
     * Returns the next "quiet hours end" timestamp, or null if not in quiet hours.
     */
    suspend fun nextQuietEndTimestamp(): Long? {
        if (!isCurrentlyQuiet()) return null
        val data = store.data.first()
        val end = data[KEY_END] ?: 420

        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        cal.set(Calendar.HOUR_OF_DAY, end / 60)
        cal.set(Calendar.MINUTE, end % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (end < nowMinutes) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        private val KEY_START = intPreferencesKey("quiet_hours_start")
        private val KEY_END = intPreferencesKey("quiet_hours_end")
    }
}
