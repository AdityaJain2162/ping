package com.aditya.ping.ui

import android.app.Activity
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.ping.data.PingDatabase
import com.aditya.ping.data.ReminderEntity
import com.aditya.ping.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var volumeLevel = 1
    private val maxVolume = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full-screen, show over lock screen, turn screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Alarm"
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()

        startSound()
        startVibration()
        startVolumeEscalation()

        setContent {
            AlarmScreen(
                title = title,
                note = note,
                onDismiss = {
                    stopAlarm()
                    finish()
                },
                onSnooze = {
                    snooze(reminderId, title, note)
                    stopAlarm()
                    finish()
                },
            )
        }
    }

    private fun startSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(this, uri)
            ringtone?.audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            volumeLevel = 1
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volumeLevel, 0)
            ringtone?.play()
        } catch (e: Exception) {
            // Fallback: no sound
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VibratorManager::class.java)
            vibrator = vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun startVolumeEscalation() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (volumeLevel < maxVolume) {
                    volumeLevel++
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volumeLevel, 0)
                    handler.postDelayed(this, 3000) // ramp every 3 seconds
                }
            }
        }, 3000)
    }

    private fun snooze(reminderId: Long, title: String, note: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = PingDatabase.get(this@AlarmActivity).reminderDao()
            val reminder = dao.getById(reminderId) ?: return@launch
            val snoozeMillis = reminder.snoozeMinutes * 60_000L
            val newDueAt = System.currentTimeMillis() + snoozeMillis
            dao.update(reminder.copy(dueAt = newDueAt))

            // Re-schedule the alarm
            val updated = reminder.copy(dueAt = newDueAt)
            AlarmScheduler.schedule(this@AlarmActivity, updated)
        }
    }

    private fun stopAlarm() {
        handler.removeCallbacksAndMessages(null)
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }

    override fun onBackPressed() {
        // Prevent accidental back-press dismissal; must use Snooze or Dismiss
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_NOTE = "note"
    }
}

@Composable
private fun AlarmScreen(
    title: String,
    note: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "ALARM",
            color = Color.White,
            fontSize = 20.sp,
            letterSpacing = 4.sp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 32.sp,
            style = MaterialTheme.typography.headlineMedium,
        )
        if (note.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = note,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp,
            )
        }
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Text("Dismiss", fontSize = 20.sp)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onSnooze,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("Snooze", fontSize = 18.sp, color = Color.White)
        }
    }
}
