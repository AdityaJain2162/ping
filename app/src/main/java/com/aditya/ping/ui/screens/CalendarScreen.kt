package com.aditya.ping.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import com.aditya.ping.ui.theme.LocalHaptics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya.ping.R
import com.aditya.ping.data.ReminderRepository
import com.aditya.ping.ui.components.BannerAd
import com.aditya.ping.ui.components.ReminderCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
@Suppress("UNUSED_PARAMETER")
fun CalendarScreen(onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    val repo = remember { ReminderRepository.from(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var displayedMonth by remember { mutableStateOf(Calendar.getInstance()) }
    // Track navigation direction: -1 = prev month, +1 = next month, 0 = initial
    var navDirection by remember { mutableIntStateOf(0) }

    // Query all reminders for the displayed month to show indicators
    val monthStartCal = (displayedMonth.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val monthStart = monthStartCal.timeInMillis
    monthStartCal.add(Calendar.MONTH, 1)
    val monthEnd = monthStartCal.timeInMillis

    val monthReminders by repo.observeByDateRange(monthStart, monthEnd)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Selected day's reminders
    val selCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
    selCal.set(Calendar.HOUR_OF_DAY, 0)
    selCal.set(Calendar.MINUTE, 0)
    selCal.set(Calendar.SECOND, 0)
    selCal.set(Calendar.MILLISECOND, 0)
    val startOfDay = selCal.timeInMillis
    selCal.add(Calendar.DAY_OF_YEAR, 1)
    val endOfDay = selCal.timeInMillis

    val dayReminders by repo.observeByDateRange(startOfDay, endOfDay)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Location-only reminders (no dueAt) — shown in a separate section
    val locationReminders by repo.observeLocationOnly()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val monthFmt = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    // Locale-aware day headers — use fresh calendar per iteration
    val dayHeaders = remember {
        val firstDay = Calendar.getInstance().firstDayOfWeek
        val fmt = SimpleDateFormat("EEEEE", Locale.getDefault())
        (0 until 7).map { offset ->
            val day = (firstDay - Calendar.SUNDAY + offset + 7) % 7
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY + day)
            fmt.format(cal.time).uppercase()
        }
    }

    // Days with reminder indicator dots — keyed on monthReminders so it
    // recomputes when the month changes
    val daysWithReminders = remember(monthReminders) {
        monthReminders.mapNotNull { r ->
            r.dueAt?.let {
                val c = Calendar.getInstance().apply { timeInMillis = it }
                c.get(Calendar.DAY_OF_MONTH)
            }
        }.toSet()
    }

    // Calendar grid calculations — recompute when month changes
    val gridData = remember(displayedMonth) {
        val fdom = (displayedMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val dim = displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val fdow = (fdom.get(Calendar.DAY_OF_WEEK) - Calendar.getInstance().firstDayOfWeek + 7) % 7
        val total = ((fdow + dim + 6) / 7) * 7
        Triple(fdow, dim, total / 7)
    }
    val firstDayOfWeek = gridData.first
    val daysInMonth = gridData.second
    val rows = gridData.third
    val monthKey = displayedMonth.timeInMillis

    // NestedScroll: consume horizontal drags so the pager doesn't steal them
    val pagerScrollGuard = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                return if (available.x != 0f && available.y == 0f) available
                else androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    fun changeMonth(direction: Int) {
        navDirection = direction
        val newMonth = displayedMonth.clone() as Calendar
        newMonth.add(Calendar.MONTH, direction)
        displayedMonth = newMonth
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pagerScrollGuard),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Month navigation with animated title
        item(key = "monthNav") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = {
                    haptics.tap()
                    changeMonth(-1)
                }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.calendar_prev_month))
                }
                AnimatedContent(
                    targetState = monthFmt.format(displayedMonth.time),
                    transitionSpec = {
                        if (navDirection >= 0) {
                            (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)))
                        } else {
                            (slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)))
                        }
                    },
                    label = "monthTitle",
                ) { title ->
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                IconButton(onClick = {
                    haptics.tap()
                    changeMonth(1)
                }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.calendar_next_month))
                }
            }
        }

        // Day headers
        item(key = "dayHeaders") {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                dayHeaders.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Calendar grid — each row as a separate item, keyed by month
        items(rows, key = { row -> "gridRow_${monthKey}_$row" }) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val isCurrentMonth = cellIndex >= firstDayOfWeek && cellIndex < firstDayOfWeek + daysInMonth
                    val day = if (isCurrentMonth) cellIndex - firstDayOfWeek + 1 else 0

                    val cellCal = (displayedMonth.clone() as Calendar).apply {
                        if (isCurrentMonth) set(Calendar.DAY_OF_MONTH, day)
                    }
                    val isSelected = isCurrentMonth && run {
                        val sCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                        sCal.get(Calendar.YEAR) == cellCal.get(Calendar.YEAR) &&
                            sCal.get(Calendar.MONTH) == cellCal.get(Calendar.MONTH) &&
                            sCal.get(Calendar.DAY_OF_MONTH) == cellCal.get(Calendar.DAY_OF_MONTH)
                    }
                    val isToday = isCurrentMonth && run {
                        val today = Calendar.getInstance()
                        today.get(Calendar.YEAR) == cellCal.get(Calendar.YEAR) &&
                            today.get(Calendar.MONTH) == cellCal.get(Calendar.MONTH) &&
                            today.get(Calendar.DAY_OF_MONTH) == cellCal.get(Calendar.DAY_OF_MONTH)
                    }
                    val hasReminders = isCurrentMonth && day in daysWithReminders

                    // Animated selection background
                    val cellBg by animateColorAsState(
                        targetValue = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "cellBg",
                    )
                    val cellFg by animateColorAsState(
                        targetValue = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "cellFg",
                    )
                    // Animate dot scale
                    val dotScale by animateFloatAsState(
                        targetValue = if (hasReminders) 1f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "dotScale",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable(enabled = isCurrentMonth) {
                                haptics.tap()
                                selectedDate = cellCal.timeInMillis
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isCurrentMonth) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(cellBg),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = cellFg,
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .scale(dotScale)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selected day label with crossfade
        item(key = "selectedDayLabel") {
            Spacer(Modifier.height(16.dp))
            AnimatedContent(
                targetState = dayFmt.format(java.util.Date(selectedDate)),
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "dayLabel",
            ) { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // Reminders for selected day
        if (dayReminders.isEmpty()) {
            item(key = "empty") {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(300)),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.calendar_no_reminders),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(dayReminders, key = { it.id }) { reminder ->
                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                    ReminderCard(
                        reminder = reminder,
                        onToggleEnabled = { enabled ->
                            haptics.tap()
                            scope.launch { repo.setEnabled(reminder.id, enabled) }
                        },
                        onToggleCompleted = { completed ->
                            haptics.heavy()
                            scope.launch { repo.setCompleted(reminder.id, completed, if (completed) System.currentTimeMillis() else null) }
                        },
                        onDelete = {
                            haptics.heavy()
                            scope.launch { repo.deleteById(reminder.id) }
                        },
                        onClick = { onEdit(reminder.id) },
                    )
                }
            }
        }

        // Location-only reminders (no due date) — always visible regardless of selected day
        if (locationReminders.isNotEmpty()) {
            item(key = "locationHeader") {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.calendar_location_reminders),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            items(locationReminders.size, key = { index -> "loc_${locationReminders[index].id}" }) { index ->
                val reminder = locationReminders[index]
                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                    ReminderCard(
                        reminder = reminder,
                        onToggleEnabled = { enabled ->
                            haptics.tap()
                            scope.launch { repo.setEnabled(reminder.id, enabled) }
                        },
                        onToggleCompleted = { completed ->
                            haptics.heavy()
                            scope.launch { repo.setCompleted(reminder.id, completed, if (completed) System.currentTimeMillis() else null) }
                        },
                        onDelete = {
                            haptics.heavy()
                            scope.launch { repo.deleteById(reminder.id) }
                        },
                        onClick = { onEdit(reminder.id) },
                    )
                }
            }
        }
    }
}
