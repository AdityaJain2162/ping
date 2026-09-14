package com.aditya.ping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya.ping.R
import com.aditya.ping.data.ReminderRepository
import com.aditya.ping.ui.components.ReminderCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val context = LocalContext.current
    val repo = remember { ReminderRepository.from(context) }
    val scope = rememberCoroutineScope()

    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var displayedMonth by remember { mutableStateOf(Calendar.getInstance()) }

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

    val monthFmt = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    // Locale-aware day headers
    val dayHeaders = remember {
        val cal = Calendar.getInstance()
        val firstDay = cal.firstDayOfWeek
        val fmt = SimpleDateFormat("EEEEE", Locale.getDefault())
        (0 until 7).map { offset ->
            val day = (firstDay - Calendar.SUNDAY + offset + 7) % 7
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY + day)
            fmt.format(cal.time).uppercase()
        }
    }

    // Days with reminder indicator dots
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
    // from SwipeToDismissBox inside the LazyColumn. Vertical scrolls pass
    // through to the LazyColumn normally.
    val pagerScrollGuard = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                // Consume horizontal pre-scroll so the pager doesn't intercept
                return if (available.x != 0f && available.y == 0f) {
                    available
                } else {
                    androidx.compose.ui.geometry.Offset.Zero
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pagerScrollGuard),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Month navigation
        item(key = "monthNav") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = {
                    val newMonth = displayedMonth.clone() as Calendar
                    newMonth.add(Calendar.MONTH, -1)
                    displayedMonth = newMonth
                }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.calendar_prev_month))
                }
                Text(
                    monthFmt.format(displayedMonth.time),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = {
                    val newMonth = displayedMonth.clone() as Calendar
                    newMonth.add(Calendar.MONTH, 1)
                    displayedMonth = newMonth
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

        // Calendar grid — each row as a separate item
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

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable(enabled = isCurrentMonth) {
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
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else if (isToday) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surface,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (hasReminders) MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selected day label
        item(key = "selectedDayLabel") {
            Spacer(Modifier.height(16.dp))
            Text(
                dayFmt.format(java.util.Date(selectedDate)),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
        }

        // Reminders for selected day
        if (dayReminders.isEmpty()) {
            item(key = "empty") {
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
        } else {
            items(dayReminders, key = { it.id }) { reminder ->
                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                    ReminderCard(
                        reminder = reminder,
                        onToggleEnabled = { enabled ->
                            scope.launch { repo.setEnabled(reminder.id, enabled) }
                        },
                        onToggleCompleted = { completed ->
                            scope.launch { repo.setCompleted(reminder.id, completed, if (completed) System.currentTimeMillis() else null) }
                        },
                        onDelete = {
                            scope.launch { repo.deleteById(reminder.id) }
                        },
                        onClick = { onEdit(reminder.id) },
                    )
                }
            }
        }
    }
}
