package com.aditya.ping

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.aditya.ping.data.ThemeRepository
import com.aditya.ping.ui.navigation.PingNavHost
import com.aditya.ping.ui.navigation.Routes
import com.aditya.ping.ui.theme.PingTheme
import com.aditya.ping.ui.theme.rememberHapticController
import com.aditya.ping.widget.DueTodayWidgetProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+ (required for reminders/alarms to fire)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                    .launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val quickAdd = intent?.getBooleanExtra(DueTodayWidgetProvider.EXTRA_QUICK_ADD, false) ?: false
        val sharedText = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null

        setContent {
            val themeRepo = remember { ThemeRepository(this) }
            val prefs by themeRepo.themePrefs.collectAsState(initial = com.aditya.ping.data.ThemePrefs())
            val hapticController = rememberHapticController(
                enabled = prefs.hapticFeedback,
                intensityName = prefs.hapticIntensity,
            )

            PingTheme(
                themeMode = prefs.mode,
                accentName = prefs.accentName,
                dynamicColor = prefs.dynamicColor,
                animationsEnabled = prefs.animationsEnabled,
                hapticController = hapticController,
            ) {
                PingNavHost(
                    startRoute = if (quickAdd || sharedText != null) Routes.ADD else Routes.HOME,
                    sharedText = sharedText,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
