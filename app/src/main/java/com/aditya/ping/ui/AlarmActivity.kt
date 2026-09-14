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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.ping.data.PingDatabase
import com.aditya.ping.util.AlarmScheduler
import com.aditya.ping.util.NagScheduler
import com.aditya.ping.util.QuickActionExecutor
import com.aditya.ping.util.RecurrenceCalculator
import com.aditya.ping.util.SmartSnooze
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var volumeLevel = 1
    private val maxVolume = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )

        // On Android 10+, use the newer API for lock screen visibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        // Request to dismiss keyguard if device is locked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(android.app.KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        }

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Alarm"
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()

        val reminder = if (reminderId > 0) {
            try { kotlinx.coroutines.runBlocking { PingDatabase.get(this@AlarmActivity).reminderDao().getById(reminderId) } }
            catch (e: Exception) { null }
        } else null

        val customRingtoneUri = reminder?.ringtoneUri?.takeIf { it.isNotBlank() }
        val quickActionType = reminder?.quickActionType ?: 0
        val quickActionData = reminder?.quickActionData ?: ""
        val quickActionLabel = QuickActionExecutor.actionLabel(quickActionType)
        val hasQuickAction = quickActionType != 0 && quickActionData.isNotBlank()

        startSound(customRingtoneUri)
        startVibration()
        startVolumeEscalation()

        setContent {
            AlarmScreen(
                title = title,
                note = note,
                quickActionLabel = if (hasQuickAction) quickActionLabel else null,
                quickActionIcon = quickActionIcon(quickActionType),
                onQuickAction = if (hasQuickAction) {
                    { reminder?.let { QuickActionExecutor.execute(this, it) } }
                } else null,
                onDismiss = {
                    dismissAlarm(reminderId)
                    stopAlarm()
                    finish()
                },
                onSnooze = {
                    snooze(reminderId, title, note)
                    stopAlarm()
                    finish()
                },
                onSnoozeTo = { snoozeTo ->
                    snoozeTo(reminderId, snoozeTo)
                    stopAlarm()
                    finish()
                },
            )
        }
    }

    private fun dismissAlarm(reminderId: Long) {
        NagScheduler.cancel(this, reminderId)
        CoroutineScope(Dispatchers.IO).launch {
            val dao = PingDatabase.get(this@AlarmActivity).reminderDao()
            val reminder = dao.getById(reminderId) ?: return@launch
            // For non-recurring reminders, mark as completed
            if (RecurrenceCalculator.nextOccurrence(reminder, System.currentTimeMillis()) == null) {
                dao.setCompleted(reminderId, true, System.currentTimeMillis())
            }
        }
    }

    private fun quickActionIcon(type: Int): ImageVector = when (type) {
        1 -> Icons.Filled.Call
        2, 7 -> Icons.Filled.Whatsapp
        6 -> Icons.Filled.Message
        4 -> Icons.Filled.NavigateNext
        5 -> Icons.Filled.OpenInNew
        else -> Icons.Filled.OpenInNew
    }

    private fun startSound(customUri: String? = null) {
        try {
            val uri = if (!customUri.isNullOrBlank()) {
                android.net.Uri.parse(customUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
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
                    handler.postDelayed(this, 3000)
                }
            }
        }, 3000)
    }

    private fun snooze(reminderId: Long, title: String, note: String) {
        NagScheduler.cancel(this@AlarmActivity, reminderId)
        CoroutineScope(Dispatchers.IO).launch {
            val dao = PingDatabase.get(this@AlarmActivity).reminderDao()
            val reminder = dao.getById(reminderId) ?: return@launch
            val snoozeMillis = reminder.snoozeMinutes * 60_000L
            val newDueAt = System.currentTimeMillis() + snoozeMillis
            dao.update(reminder.copy(dueAt = newDueAt))
            val updated = reminder.copy(dueAt = newDueAt)
            AlarmScheduler.schedule(this@AlarmActivity, updated)
        }
    }

    private fun snoozeTo(reminderId: Long, newDueAt: Long) {
        NagScheduler.cancel(this@AlarmActivity, reminderId)
        CoroutineScope(Dispatchers.IO).launch {
            val dao = PingDatabase.get(this@AlarmActivity).reminderDao()
            val reminder = dao.getById(reminderId) ?: return@launch
            dao.update(reminder.copy(dueAt = newDueAt))
            AlarmScheduler.schedule(this@AlarmActivity, reminder.copy(dueAt = newDueAt))
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
        // Prevent accidental back-press dismissal
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
    onSnoozeTo: (Long) -> Unit,
    quickActionLabel: String? = null,
    quickActionIcon: ImageVector? = null,
    onQuickAction: (() -> Unit)? = null,
) {
    var showSnoozeOptions by remember { mutableStateOf(false) }
    val snoozeOptions = remember { SmartSnooze.options() }
    val currentTime = remember { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) }

    // Pulsing alarm icon animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A0A0A),
                        Color(0xFF2D1010),
                        Color(0xFF1A0A0A),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Top: Pulsing alarm icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFE85D5D), Color(0xFFE85D5D).copy(alpha = 0.3f)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Alarm,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(52.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = currentTime,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                )
            }

            // Middle: Title and note
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (note.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = note,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }

            // Bottom: Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Quick action button (prominent, gradient)
                if (quickActionLabel != null && onQuickAction != null && quickActionIcon != null) {
                    Button(
                        onClick = onQuickAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366), // WhatsApp green
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(quickActionIcon, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(quickActionLabel, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Dismiss button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE85D5D),
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Dismiss", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                // Snooze button
                OutlinedButton(
                    onClick = { showSnoozeOptions = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text("Snooze", fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }

        // Snooze options dialog
        if (showSnoozeOptions) {
            AlertDialog(
                onDismissRequest = { showSnoozeOptions = false },
                title = { Text("Snooze for…") },
                text = {
                    Column {
                        snoozeOptions.forEach { option ->
                            TextButton(
                                onClick = {
                                    showSnoozeOptions = false
                                    onSnoozeTo(option.calculate())
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(option.label, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSnoozeOptions = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}
