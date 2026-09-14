package com.aditya.ping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.aditya.ping.data.ThemeRepository
import com.aditya.ping.ui.navigation.PingNavHost
import com.aditya.ping.ui.theme.PingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeRepo = remember { ThemeRepository(this) }
            val themeMode by themeRepo.themeMode.collectAsState(initial = com.aditya.ping.domain.ThemeMode.SYSTEM)

            PingTheme(themeMode = themeMode) {
                PingNavHost()
            }
        }
    }
}
