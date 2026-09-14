package com.aditya.ping.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.aditya.ping.ui.components.ReminderCard
import com.aditya.ping.ui.theme.PrimaryGradientEnd
import com.aditya.ping.ui.theme.PrimaryGradientStart

@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSettings: () -> Unit,
    onSavedPlaces: () -> Unit,
    onLists: () -> Unit,
    onCalendar: () -> Unit,
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

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Gradient hero header with stats
        AnimatedVisibility(
            visible = !isSearching && reminders.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            HeroStatsHeader(stats = stats)
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = vm::onSearchQueryChange,
            placeholder = { Text(stringResource(R.string.home_search)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
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
                        onToggle = { vm.toggleEnabled(r.id, it) },
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
                        SectionHeader(section = section)
                    }
                    items(
                        items = section.reminders,
                        key = { "${section.title}_${it.id}" },
                    ) { r ->
                        ReminderCard(
                            reminder = r,
                            onToggle = { vm.toggleEnabled(r.id, it) },
                            onDelete = { vm.delete(r.id) },
                            onClick = { onEdit(r.id) },
                        )
                    }
                }
                item { BannerAd() }
            }
        }
    }
}

@Composable
private fun HeroStatsHeader(stats: HomeStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(PrimaryGradientStart, PrimaryGradientEnd),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
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
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(Color.White.copy(alpha = 0.2f)),
    )
}

@Composable
private fun SectionHeader(section: ReminderSection) {
    Row(
        modifier = Modifier
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
                .clip(RoundedCornerShape(10.dp))
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
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryGradientStart.copy(alpha = 0.15f), PrimaryGradientEnd.copy(alpha = 0.1f)),
                        ),
                    ),
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
                shape = RoundedCornerShape(16.dp),
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
