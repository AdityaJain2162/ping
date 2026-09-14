package com.aditya.ping.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aditya.ping.R
import com.aditya.ping.ui.screens.AddEditScreen
import com.aditya.ping.ui.screens.AutomationsScreen
import com.aditya.ping.ui.screens.CalendarScreen
import com.aditya.ping.ui.screens.HistoryScreen
import com.aditya.ping.ui.screens.HomeScreen
import com.aditya.ping.ui.screens.ListsScreen
import com.aditya.ping.ui.screens.SavedPlacesScreen
import com.aditya.ping.ui.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingNavHost() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val tabRoutes = listOf(Routes.HOME, Routes.SAVED_PLACES, Routes.CALENDAR, Routes.AUTOMATIONS)
    val isTabRoute = currentRoute in tabRoutes
    val currentIndex = tabRoutes.indexOf(currentRoute)

    fun navigateToTab(index: Int) {
        if (index < 0 || index >= tabRoutes.size) return
        val target = tabRoutes[index]
        nav.navigate(target) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val swipeModifier = if (isTabRoute) {
        Modifier.pointerInput(currentRoute) {
            val accumulated = mutableFloatStateOf(0f)
            val threshold = 120f
            detectHorizontalDragGestures(
                onDragStart = { accumulated.floatValue = 0f },
                onHorizontalDrag = { _, dragAmount ->
                    accumulated.floatValue += dragAmount
                },
                onDragEnd = {
                    val total = accumulated.floatValue
                    when {
                        total > threshold -> navigateToTab(currentIndex - 1)
                        total < -threshold -> navigateToTab(currentIndex + 1)
                    }
                    accumulated.floatValue = 0f
                },
                onDragCancel = { accumulated.floatValue = 0f },
            )
        }
    } else {
        Modifier
    }

    Scaffold(
        topBar = {
            if (isTabRoute) {
                TopAppBar(
                    title = { Text(tabTitle(currentRoute)) },
                    actions = {
                        if (currentRoute == Routes.HOME) {
                            IconButton(onClick = { nav.navigate(Routes.LISTS) }) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.lists_title))
                            }
                        }
                        IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (isTabRoute) {
                NavigationBar {
                    tabRoutes.forEach { route ->
                        val (icon, label) = tabInfo(route)
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = { navigateToTab(tabRoutes.indexOf(route)) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Routes.HOME) {
                FloatingActionButton(
                    onClick = { nav.navigate(Routes.ADD) },
                    shape = MaterialTheme.shapes.extraLarge,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.home_add))
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .then(swipeModifier),
        ) {
            val tabDuration = 300
            fun tabEnter(direction: AnimatedContentTransitionScope.SlideDirection) =
                slideInHorizontally(tween(tabDuration)) { full ->
                    if (direction == AnimatedContentTransitionScope.SlideDirection.Left) full else -full
                } + fadeIn(tween(tabDuration))
            fun tabExit(direction: AnimatedContentTransitionScope.SlideDirection) =
                slideOutHorizontally(tween(tabDuration)) { full ->
                    if (direction == AnimatedContentTransitionScope.SlideDirection.Left) -full else full
                } + fadeOut(tween(tabDuration))

            composable(
                Routes.HOME,
                enterTransition = { tabEnter(AnimatedContentTransitionScope.SlideDirection.Left) },
                exitTransition = { tabExit(AnimatedContentTransitionScope.SlideDirection.Left) },
            ) {
                HomeScreen(
                    onAdd = { nav.navigate(Routes.ADD) },
                    onEdit = { id -> nav.navigate(Routes.edit(id)) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                    onSavedPlaces = { nav.navigate(Routes.SAVED_PLACES) },
                    onLists = { nav.navigate(Routes.LISTS) },
                    onCalendar = { nav.navigate(Routes.CALENDAR) },
                    onHistory = { nav.navigate(Routes.HISTORY) },
                )
            }
            composable(
                Routes.SAVED_PLACES,
                enterTransition = { tabEnter(AnimatedContentTransitionScope.SlideDirection.Left) },
                exitTransition = { tabExit(AnimatedContentTransitionScope.SlideDirection.Left) },
            ) {
                SavedPlacesScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.LISTS) {
                ListsScreen(onBack = { nav.popBackStack() })
            }
            composable(
                Routes.CALENDAR,
                enterTransition = { tabEnter(AnimatedContentTransitionScope.SlideDirection.Left) },
                exitTransition = { tabExit(AnimatedContentTransitionScope.SlideDirection.Left) },
            ) {
                CalendarScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { id -> nav.navigate(Routes.edit(id)) },
                )
            }
            composable(
                Routes.AUTOMATIONS,
                enterTransition = { tabEnter(AnimatedContentTransitionScope.SlideDirection.Left) },
                exitTransition = { tabExit(AnimatedContentTransitionScope.SlideDirection.Left) },
            ) {
                AutomationsScreen()
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
            composable(Routes.HISTORY) {
                HistoryScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun tabTitle(route: String?): String = when (route) {
    Routes.HOME -> stringResource(R.string.home_title)
    Routes.SAVED_PLACES -> stringResource(R.string.saved_places_title)
    Routes.CALENDAR -> stringResource(R.string.calendar_title)
    Routes.AUTOMATIONS -> stringResource(R.string.automations_title)
    else -> stringResource(R.string.home_title)
}

@Composable
private fun tabInfo(route: String): Pair<androidx.compose.ui.graphics.vector.ImageVector, String> = when (route) {
    Routes.HOME -> Icons.Filled.Home to stringResource(R.string.tab_home)
    Routes.SAVED_PLACES -> Icons.Filled.LocationOn to stringResource(R.string.tab_saved)
    Routes.CALENDAR -> Icons.Filled.CalendarMonth to stringResource(R.string.tab_calendar)
    Routes.AUTOMATIONS -> Icons.Filled.AutoAwesome to stringResource(R.string.tab_automations)
    else -> Icons.Filled.Home to stringResource(R.string.tab_home)
}
