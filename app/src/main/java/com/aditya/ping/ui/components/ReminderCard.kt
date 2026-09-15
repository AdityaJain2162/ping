package com.aditya.ping.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aditya.ping.R
import com.aditya.ping.data.ReminderEntity
import com.aditya.ping.ui.theme.LocalHaptics
import com.aditya.ping.util.UiFormats
import com.aditya.ping.ui.theme.TimestampStyle

@Composable
fun ReminderCard(
    reminder: ReminderEntity,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onClone: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    val isOverdue = reminder.enabled && !reminder.completed && reminder.dueAt != null && reminder.dueAt < System.currentTimeMillis()
    val isDone = reminder.completed

    val accentColor = when {
        isOverdue -> MaterialTheme.colorScheme.error
        isDone -> MaterialTheme.colorScheme.secondary
        reminder.isAlarm -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val cardAlpha by animateFloatAsState(
        targetValue = if (isDone) 0.5f else 1f,
        label = "cardAlpha",
    )

    // Swipe state: right = toggle done, left = delete
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe right = toggle done
                    haptics.confirm()
                    onToggleCompleted(!reminder.completed)
                    false // Don't dismiss — just toggle
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe left = delete
                    haptics.heavy()
                    onDelete()
                    true // Dismiss
                }
                else -> false
            }
        },
        positionalThreshold = { distance -> distance * 0.4f },
    )

    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            val direction = swipeState.dismissDirection
            val bgColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondary
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(bgColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd)
                    Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(
                        imageVector = if (isDone) Icons.Filled.CheckCircle
                        else Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        },
    ) {
        Card(
            onClick = {
                haptics.tap()
                onClick()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 0.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Accent bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(56.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(accentColor),
                )

                Row(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 4.dp)
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Icon badge
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when {
                                isDone -> Icons.Filled.CheckCircle
                                reminder.isAlarm -> Icons.Filled.Alarm
                                reminder.triggerType == 0 -> Icons.Filled.LocationOn
                                else -> Icons.Filled.LocationOff
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = reminder.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            if (isOverdue) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.card_overdue),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))

                        if (reminder.lat != 0.0 || reminder.lng != 0.0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = reminder.addressLabel.ifBlank {
                                        "%.4f, %.4f".format(reminder.lat, reminder.lng)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                        }

                        reminder.dueAt?.let { due ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isOverdue) Icons.Filled.WarningAmber
                                    else Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = if (isOverdue) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = UiFormats.formatReminderDate(due),
                                    style = TimestampStyle,
                                    color = if (isOverdue) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                        }

                        if (reminder.note.isNotBlank()) {
                            Text(
                                text = reminder.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Checkmark button for done
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (isDone) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            else Color.Transparent,
                        )
                        .clickable {
                            haptics.confirm()
                            onToggleCompleted(!reminder.completed)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isDone) Icons.Filled.CheckCircle
                        else Icons.Filled.Check,
                        contentDescription = stringResource(R.string.card_mark_done),
                        tint = if (isDone) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Clone button
                IconButton(
                    onClick = {
                        haptics.tap()
                        onClone()
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.reminder_clone),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Switch for enabled/disabled (alarm active or not)
                Switch(checked = reminder.enabled, onCheckedChange = {
                    haptics.tap()
                    onToggleEnabled(it)
                })
            }
        }
    }
}
