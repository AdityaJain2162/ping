package com.aditya.ping.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aditya.ping.R
import com.aditya.ping.data.ReminderRepository
import com.aditya.ping.ui.components.BannerAd
import com.aditya.ping.ui.components.CelebrationOverlay
import com.aditya.ping.ui.components.ReminderCard

@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSettings: () -> Unit,
    onSavedPlaces: () -> Unit,
    onLists: () -> Unit,
    onCalendar: () -> Unit,
    onHistory: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val repo = remember { ReminderRepository.from(context) }
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repo, appContext))
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val sections by vm.sections.collectAsStateWithLifecycle()
    val isSearching = searchQuery.isNotBlank()

    // Celebration overlay state
    var showCelebration by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Gradient hero header with stats + progress ring
            AnimatedVisibility(
                visible = !isSearching && reminders.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    HeroStatsHeader(stats = stats)
                    if (stats.completed > 0) {
                        Text(
                            text = stringResource(R.string.home_view_history, stats.completed),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onHistory() },
                        )
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = vm::onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.home_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (reminders.isEmpty() && !isSearching) {
                EmptyState(onAdd = onAdd)
            } else if (isSearching) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(reminders, key = { it.id }) { r ->
                        ReminderCard(
                            reminder = r,
                            onToggleEnabled = { enabled -> vm.toggleEnabled(r.id, enabled) },
                            onToggleCompleted = { completed ->
                                vm.toggleCompleted(r.id, completed)
                                if (completed) showCelebration = true
                            },
                            onDelete = { vm.delete(r.id) },
                            onClick = { onEdit(r.id) },
                        )
                    }
                    item { BannerAd() }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    sections.forEach { section ->
                        item(key = "header_${section.title}") {
                            SectionHeader(
                                section = section,
                                modifier = Modifier.animateItem(),
                            )
                        }
                        items(
                            items = section.reminders,
                            key = { "${section.title}_${it.id}" },
                        ) { r ->
                            ReminderCard(
                                reminder = r,
                                onToggleEnabled = { enabled -> vm.toggleEnabled(r.id, enabled) },
                                onToggleCompleted = { completed ->
                                    vm.toggleCompleted(r.id, completed)
                                    if (completed) showCelebration = true
                                },
                                onDelete = { vm.delete(r.id) },
                                onClick = { onEdit(r.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    item { BannerAd() }
                }
            }
        }

        // Celebration overlay
        CelebrationOverlay(
            visible = showCelebration,
            onDismiss = { showCelebration = false },
        )
    }
}

@Composable
private fun HeroStatsHeader(stats: HomeStats) {
    val total = stats.active + stats.completed
    val completionRate = if (total > 0) stats.completed.toFloat() / total else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = completionRate,
        label = "progressRing",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Progress ring
            val ringColor = MaterialTheme.colorScheme.onPrimaryContainer
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    val strokeWidth = 6.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val arcOffset = Offset(strokeWidth / 2, strokeWidth / 2)
                    drawArc(
                        color = ringColor.copy(alpha = 0.25f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
                Text(
                    text = "${(completionRate * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.width(16.dp))

            // Stats
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatItem(
                    icon = Icons.Filled.Bolt,
                    count = stats.active,
                    label = stringResource(R.string.home_stat_active),
                )
                VerticalDivider()
                StatItem(
                    icon = Icons.Filled.WarningAmber,
                    count = stats.overdue,
                    label = stringResource(R.string.home_stat_overdue),
                )
                VerticalDivider()
                StatItem(
                    icon = Icons.Filled.CheckCircle,
                    count = stats.completed,
                    label = stringResource(R.string.home_stat_completed),
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    count: Int,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)),
    )
}

@Composable
private fun SectionHeader(section: ReminderSection, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.labelLarge,
            color = if (section.isOverdue) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (section.isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = section.reminders.size.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (section.isOverdue) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val iconScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "emptyIconScale",
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .scale(iconScale)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                )
            }
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.home_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onAdd,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_empty_cta))
            }
        }
    }
}
