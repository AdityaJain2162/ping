package com.aditya.geonote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.aditya.geonote.data.ThemeRepository
import com.aditya.geonote.ui.navigation.GeoNoteNavHost
import com.aditya.geonote.ui.theme.GeoNoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeRepo = remember { ThemeRepository(this) }
            val themeMode by themeRepo.themeMode.collectAsState(initial = com.aditya.geonote.domain.ThemeMode.SYSTEM)

            GeoNoteTheme(themeMode = themeMode) {
                GeoNoteNavHost()
            }
        }
    }
}
