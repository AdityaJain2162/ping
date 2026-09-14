package com.aditya.ping.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aditya.ping.ui.screens.AddEditScreen
import com.aditya.ping.ui.screens.CalendarScreen
import com.aditya.ping.ui.screens.HomeScreen
import com.aditya.ping.ui.screens.ListsScreen
import com.aditya.ping.ui.screens.SavedPlacesScreen
import com.aditya.ping.ui.screens.SettingsScreen

@Composable
fun PingNavHost() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onAdd = { nav.navigate(Routes.ADD) },
                onEdit = { id -> nav.navigate(Routes.edit(id)) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onSavedPlaces = { nav.navigate(Routes.SAVED_PLACES) },
                onLists = { nav.navigate(Routes.LISTS) },
                onCalendar = { nav.navigate(Routes.CALENDAR) },
            )
        }
        composable(Routes.ADD) {
            AddEditScreen(
                reminderId = 0L,
                onSaved = { nav.popBackStack() },
                onCancel = { nav.popBackStack() },
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            AddEditScreen(
                reminderId = id,
                onSaved = { nav.popBackStack() },
                onCancel = { nav.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SAVED_PLACES) {
            SavedPlacesScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.LISTS) {
            ListsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.CALENDAR) {
            CalendarScreen(
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate(Routes.edit(id)) },
            )
        }
    }
}
