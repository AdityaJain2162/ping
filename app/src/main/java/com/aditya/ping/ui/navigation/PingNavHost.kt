package com.aditya.ping.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingNavHost(startRoute: String = Routes.HOME, sharedText: String? = null) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isOnTab = currentRoute == Routes.HOME
    val scope = rememberCoroutineScope()
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    val tabRoutes = listOf(Routes.HOME, Routes.SAVED_PLACES, Routes.CALENDAR, Routes.AUTOMATIONS)
    val pagerState = rememberPagerState(pageCount = { tabRoutes.size })

    Scaffold(
        topBar = {
            if (isOnTab) {
                TopAppBar(
                    title = {
                        androidx.compose.animation.AnimatedContent(
                            targetState = pagerState.currentPage,
                            transitionSpec = { fadeIn(spring()) togetherWith fadeOut(spring()) },
                            label = "titleCrossfade",
                        ) { page ->
                            Text(tabTitle(tabRoutes[page]), modifier = Modifier.testTag("topBarTitle"))
                        }
                    },
                    actions = {
                        if (pagerState.currentPage == 0) {
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
            if (isOnTab) {
                NavigationBar {
                    tabRoutes.forEachIndexed { index, route ->
                        val (icon, label) = tabInfo(route)
                        val selected = pagerState.currentPage == index
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.15f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "navIconScale",
                        )
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            modifier = Modifier.testTag("tab_${tabRoutes[index]}"),
                            icon = {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    modifier = Modifier.scale(iconScale),
                                )
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isOnTab && pagerState.currentPage == 0,
                enter = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = scaleOut(spring(stiffness = Spring.StiffnessMediumLow)),
            ) {
                FloatingActionButton(
                    onClick = { nav.navigate(Routes.ADD) },
                    modifier = Modifier.testTag("fab"),
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
            startDestination = startRoute,
            modifier = Modifier.fillMaxSize().padding(inner),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            composable(Routes.HOME) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (tabRoutes[page]) {
                        Routes.HOME -> HomeScreen(
                            onAdd = { nav.navigate(Routes.ADD) },
                            onEdit = { id -> nav.navigate(Routes.edit(id)) },
                            onSettings = { nav.navigate(Routes.SETTINGS) },
                            onSavedPlaces = { scope.launch { pagerState.animateScrollToPage(1) } },
                            onLists = { nav.navigate(Routes.LISTS) },
                            onCalendar = { scope.launch { pagerState.animateScrollToPage(2) } },
                            onHistory = { nav.navigate(Routes.HISTORY) },
                        )
                        Routes.SAVED_PLACES -> SavedPlacesScreen(onBack = { scope.launch { pagerState.animateScrollToPage(0) } })
                        Routes.CALENDAR -> CalendarScreen(
                            onBack = { scope.launch { pagerState.animateScrollToPage(0) } },
                            onEdit = { id -> nav.navigate(Routes.edit(id)) },
                        )
                        Routes.AUTOMATIONS -> AutomationsScreen()
                    }
                }
            }
            composable(Routes.ADD) {
                AddEditScreen(
                    reminderId = 0L,
                    sharedText = sharedText,
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
            composable(Routes.LISTS) {
                ListsScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun tabTitle(route: String): String = when (route) {
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
