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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya.ping.R
import com.aditya.ping.data.ReminderRepository
import com.aditya.ping.ui.components.ReminderCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val context = LocalContext.current
    val repo = remember { ReminderRepository.from(context) }

    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var displayedMonth by remember { mutableStateOf(Calendar.getInstance()) }

    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startOfDay = cal.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, 1)
    val endOfDay = cal.timeInMillis

    val dayReminders by repo.observeByDateRange(startOfDay, endOfDay)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val monthFmt = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendar_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
        ) {
            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = {
                    displayedMonth.add(Calendar.MONTH, -1)
                    displayedMonth = displayedMonth.clone() as Calendar
                }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = null)
                }
                Text(
                    monthFmt.format(displayedMonth.time),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = {
                    displayedMonth.add(Calendar.MONTH, 1)
                    displayedMonth = displayedMonth.clone() as Calendar
                }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                }
            }

            // Day headers
            val dayHeaders = remember { listOf("S", "M", "T", "W", "T", "F", "S") }
            Row(modifier = Modifier.fillMaxWidth()) {
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
            Spacer(Modifier.size(4.dp))

            // Calendar grid
            val firstDayOfMonth = (displayedMonth.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val daysInMonth = displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday

            var dayCounter = 1
            val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7
            val rows = totalCells / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val isCurrentMonth = cellIndex >= firstDayOfWeek && dayCounter <= daysInMonth
                        val day = if (isCurrentMonth) dayCounter else 0
                        if (isCurrentMonth) dayCounter++

                        val cellCal = (displayedMonth.clone() as Calendar).apply {
                            if (isCurrentMonth) set(Calendar.DAY_OF_MONTH, day)
                        }
                        val isSelected = isCurrentMonth && run {
                            val selCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                            selCal.get(Calendar.YEAR) == cellCal.get(Calendar.YEAR) &&
                                selCal.get(Calendar.MONTH) == cellCal.get(Calendar.MONTH) &&
                                selCal.get(Calendar.DAY_OF_MONTH) == cellCal.get(Calendar.DAY_OF_MONTH)
                        }
                        val isToday = isCurrentMonth && run {
                            val today = Calendar.getInstance()
                            today.get(Calendar.YEAR) == cellCal.get(Calendar.YEAR) &&
                                today.get(Calendar.MONTH) == cellCal.get(Calendar.MONTH) &&
                                today.get(Calendar.DAY_OF_MONTH) == cellCal.get(Calendar.DAY_OF_MONTH)
                        }

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
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
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
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.size(16.dp))

            // Selected day's reminders
            Text(
                dayFmt.format(Date(selectedDate)),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.size(8.dp))

            if (dayReminders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.calendar_no_reminders),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dayReminders, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onToggleEnabled = {},
                            onToggleCompleted = {},
                            onDelete = {},
                            onClick = { onEdit(reminder.id) },
                        )
                    }
                }
            }
        }
    }
}
