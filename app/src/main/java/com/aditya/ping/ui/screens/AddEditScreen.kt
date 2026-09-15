package com.aditya.ping.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aditya.ping.R
import com.aditya.ping.data.ReminderRepository
import com.aditya.ping.ui.components.BannerAd
import com.aditya.ping.ui.components.LocationPickerField
import com.aditya.ping.ui.theme.LocalHaptics
import com.aditya.ping.util.GeoCoderUtil
import com.aditya.ping.util.LocationUtil
import com.aditya.ping.util.PermissionUtil
import com.aditya.ping.util.UiFormats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    reminderId: Long,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    sharedText: String? = null,
) {
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    val appContext = context.applicationContext
    val repo = remember { ReminderRepository.from(context) }
    val savedPlaceRepo = remember { com.aditya.ping.data.SavedPlaceRepository.from(context) }
    val savedPlaces by savedPlaceRepo.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val vm: AddEditViewModel = viewModel(factory = AddEditViewModel.Factory(repo, appContext))
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(reminderId) { vm.load(reminderId) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    // Pre-fill title from shared text (voice assistant / share intent)
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank() && state.title.isBlank()) {
            vm.onTitleChange(sharedText)
        }
    }

    // Voice input launcher — uses Google speech recognition activity
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) {
                vm.onTitleChange(text)
            }
        }
    }

    fun startVoiceInput() {
        haptics.tap()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.add_voice_input))
        }
        try {
            voiceLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, R.string.add_voice_not_supported, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isEdit) R.string.edit_title else R.string.add_title))
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Title + voice input ──
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::onTitleChange,
                label = { Text(stringResource(R.string.add_title_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput() }) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = stringResource(R.string.add_voice_input),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = vm::onNoteChange,
                label = { Text(stringResource(R.string.add_note_label)) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
            )

            // ── Time section ──
            SectionCard(
                title = stringResource(R.string.add_section_time),
                icon = Icons.Filled.Schedule,
            ) {
                // Quick date options: Today, Tomorrow, Pick custom date
                Text(stringResource(R.string.add_time_label), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    val todayCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val tomorrowCal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    FilterChip(
                        selected = state.dueAt != null && isSameDay(state.dueAt!!, todayCal.timeInMillis),
                        onClick = {
                            haptics.tap()
                            // Preserve existing time if set, otherwise default to 9 AM
                            val cal = todayCal.clone() as Calendar
                            if (state.dueAt != null) {
                                val existing = Calendar.getInstance().apply { timeInMillis = state.dueAt!! }
                                cal.set(Calendar.HOUR_OF_DAY, existing.get(Calendar.HOUR_OF_DAY))
                                cal.set(Calendar.MINUTE, existing.get(Calendar.MINUTE))
                            }
                            vm.onDueAtChange(cal.timeInMillis)
                        },
                        label = { Text(stringResource(R.string.add_time_today)) },
                    )
                    FilterChip(
                        selected = state.dueAt != null && isSameDay(state.dueAt!!, tomorrowCal.timeInMillis),
                        onClick = {
                            haptics.tap()
                            val cal = tomorrowCal.clone() as Calendar
                            if (state.dueAt != null) {
                                val existing = Calendar.getInstance().apply { timeInMillis = state.dueAt!! }
                                cal.set(Calendar.HOUR_OF_DAY, existing.get(Calendar.HOUR_OF_DAY))
                                cal.set(Calendar.MINUTE, existing.get(Calendar.MINUTE))
                            }
                            vm.onDueAtChange(cal.timeInMillis)
                        },
                        label = { Text(stringResource(R.string.add_time_tomorrow)) },
                    )
                    FilterChip(
                        selected = state.dueAt != null &&
                            !isSameDay(state.dueAt!!, todayCal.timeInMillis) &&
                            !isSameDay(state.dueAt!!, tomorrowCal.timeInMillis),
                        onClick = {
                            haptics.tap()
                            showDatePicker(context, state.dueAt) { picked ->
                                val cal = Calendar.getInstance()
                                if (state.dueAt != null) {
                                    val existing = Calendar.getInstance().apply { timeInMillis = state.dueAt!! }
                                    cal.set(Calendar.HOUR_OF_DAY, existing.get(Calendar.HOUR_OF_DAY))
                                    cal.set(Calendar.MINUTE, existing.get(Calendar.MINUTE))
                                } else {
                                    cal.set(Calendar.HOUR_OF_DAY, 9)
                                    cal.set(Calendar.MINUTE, 0)
                                }
                                val pickedCal = Calendar.getInstance().apply { timeInMillis = picked }
                                cal.set(pickedCal.get(Calendar.YEAR), pickedCal.get(Calendar.MONTH), pickedCal.get(Calendar.DAY_OF_MONTH))
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                vm.onDueAtChange(cal.timeInMillis)
                            }
                        },
                        label = { Text(stringResource(R.string.add_time_pick_date)) },
                    )
                }

                // Time picker — independent, always enabled (defaults to today if no date)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        haptics.tap()
                        showTimePicker(context, state.dueAt) { picked ->
                            val cal = if (state.dueAt != null) {
                                Calendar.getInstance().apply { timeInMillis = state.dueAt!! }
                            } else {
                                Calendar.getInstance().apply {
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                            }
                            val pickedCal = Calendar.getInstance().apply { timeInMillis = picked }
                            cal.set(Calendar.HOUR_OF_DAY, pickedCal.get(Calendar.HOUR_OF_DAY))
                            cal.set(Calendar.MINUTE, pickedCal.get(Calendar.MINUTE))
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            vm.onDueAtChange(cal.timeInMillis)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        if (state.dueAt != null) UiFormats.formatTimeOnly(state.dueAt!!)
                        else stringResource(R.string.add_time_pick_time),
                    )
                }
                if (state.dueAt != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            UiFormats.formatReminderDate(state.dueAt!!),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = {
                            haptics.tap()
                            vm.onDueAtChange(null)
                        }) {
                            Text(stringResource(R.string.add_time_clear))
                        }
                    }
                }

                // Alarm toggle (only when time is set)
                if (state.dueAt != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.add_alarm_toggle), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = state.isAlarm,
                            onCheckedChange = {
                                haptics.confirm()
                                vm.onAlarmToggle(it)
                            },
                        )
                    }
                    if (state.isAlarm) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.snoozeMinutes.toString(),
                            onValueChange = { v -> v.toIntOrNull()?.let { vm.onSnoozeChange(it) } },
                            label = { Text(stringResource(R.string.add_snooze_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        // Ringtone picker
                        val ringtoneLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.StartActivityForResult(),
                        ) { result ->
                            @Suppress("DEPRECATION")
                            val uri = result.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                            if (uri != null) {
                                vm.onRingtoneUriChange(uri.toString())
                            }
                        }
                        OutlinedButton(onClick = {
                            haptics.tap()
                            val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM)
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Select alarm tone")
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                if (state.ringtoneUri.isNotBlank()) {
                                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.net.Uri.parse(state.ringtoneUri))
                                }
                            }
                            ringtoneLauncher.launch(intent)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Alarm, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(if (state.ringtoneUri.isNotBlank()) stringResource(R.string.add_ringtone_custom) else stringResource(R.string.add_ringtone_pick))
                        }
                        // Anti-sleep dismiss selector
                        Text(
                            stringResource(R.string.add_alarm_anti_sleep),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            listOf(
                                0 to stringResource(R.string.add_alarm_anti_sleep_none),
                                1 to stringResource(R.string.add_alarm_anti_sleep_math),
                                2 to stringResource(R.string.add_alarm_anti_sleep_long_press),
                            ).forEach { (type, label) ->
                                FilterChip(
                                    selected = state.antiSleepDismiss == type,
                                    onClick = {
                                        haptics.tap()
                                        vm.onAntiSleepChange(type)
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                )
                            }
                        }
                    }
                }

                // Recurrence (only when time is set)
                if (state.dueAt != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.add_recurrence_label), style = MaterialTheme.typography.labelMedium)
                    val recurrenceOptions = remember {
                        listOf(
                            0 to "Once", 1 to "Daily", 2 to "Weekly",
                            3 to "Weekdays", 4 to "Weekends", 5 to "Monthly",
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        recurrenceOptions.take(3).forEach { (type, label) ->
                            FilterChip(
                                selected = state.recurrenceType == type,
                                onClick = {
                                    haptics.tap()
                                    vm.onRecurrenceTypeChange(type)
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        recurrenceOptions.drop(3).forEach { (type, label) ->
                            FilterChip(
                                selected = state.recurrenceType == type,
                                onClick = {
                                    haptics.tap()
                                    vm.onRecurrenceTypeChange(type)
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                    if (state.recurrenceType == 7) {
                        OutlinedTextField(
                            value = state.recurrenceInterval.toString(),
                            onValueChange = { v -> v.toIntOrNull()?.let { vm.onRecurrenceIntervalChange(it) } },
                            label = { Text(stringResource(R.string.add_recurrence_interval)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // ── Location section ──
            SectionCard(
                title = stringResource(R.string.add_section_location),
                icon = Icons.Filled.LocationOn,
            ) {
                LocationPickerField(
                    lat = state.lat,
                    lng = state.lng,
                    label = state.addressLabel,
                    onPicked = { lat, lng, label -> vm.onLocation(lat, lng, label) },
                    savedPlaces = savedPlaces,
                )
                if (state.lat != 0.0 || state.lng != 0.0) {
                    Text(stringResource(R.string.add_trigger_label), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.triggerType == 0,
                            onClick = {
                                haptics.tap()
                                vm.onTriggerChange(0)
                            },
                            label = { Text(stringResource(R.string.add_trigger_arrive)) },
                        )
                        FilterChip(
                            selected = state.triggerType == 1,
                            onClick = {
                                haptics.tap()
                                vm.onTriggerChange(1)
                            },
                            label = { Text(stringResource(R.string.add_trigger_leave)) },
                        )
                    }
                    OutlinedTextField(
                        value = state.radiusMeters.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { vm.onRadiusChange(it) } },
                        label = { Text(stringResource(R.string.add_radius_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── Automation section ──
            SectionCard(
                title = stringResource(R.string.add_section_actions),
                icon = Icons.Filled.Bolt,
            ) {
                val automationRepo = remember { com.aditya.ping.data.AutomationRepository.from(context) }
                val automations by automationRepo.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
                var automationExpanded by remember { mutableStateOf(false) }

                if (automations.isEmpty()) {
                    Text(
                        stringResource(R.string.add_automation_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val selected = automations.find { it.id == state.automationId }
                    Box {
                        OutlinedButton(
                            onClick = {
                                haptics.tap()
                                automationExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(selected?.name ?: stringResource(R.string.add_automation_none))
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = automationExpanded,
                            onDismissRequest = { automationExpanded = false },
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_automation_none)) },
                                onClick = {
                                    haptics.tap()
                                    vm.onAutomationChange(null)
                                    automationExpanded = false
                                },
                            )
                            automations.forEach { automation ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(automation.name) },
                                    onClick = {
                                        haptics.tap()
                                        vm.onAutomationChange(automation.id)
                                        automationExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    if (selected != null) {
                        Text(
                            "Runs: ${com.aditya.ping.util.AutomationExecutor.actionLabel(selected.actionType)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            // ── Advanced section: nag mode + combined trigger ──
            SectionCard(
                title = stringResource(R.string.add_section_advanced),
                icon = Icons.Filled.Alarm,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.add_nag_toggle), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = state.nagMode,
                        onCheckedChange = {
                            haptics.confirm()
                            vm.onNagModeToggle(it)
                        },
                    )
                }
                if (state.nagMode) {
                    OutlinedTextField(
                        value = state.nagIntervalMinutes.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { vm.onNagIntervalChange(it) } },
                        label = { Text(stringResource(R.string.add_nag_interval)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                val hasLocation = state.lat != 0.0 || state.lng != 0.0
                val hasTime = state.dueAt != null
                if (hasLocation && hasTime) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.add_trigger_mode), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.triggerMode == 0,
                            onClick = {
                                haptics.tap()
                                vm.onTriggerModeChange(0)
                            },
                            label = { Text(stringResource(R.string.add_trigger_mode_or)) },
                        )
                        FilterChip(
                            selected = state.triggerMode == 1,
                            onClick = {
                                haptics.tap()
                                vm.onTriggerModeChange(1)
                            },
                            label = { Text(stringResource(R.string.add_trigger_mode_and)) },
                        )
                    }
                    Text(
                        stringResource(
                            if (state.triggerMode == 0) R.string.add_trigger_mode_or_desc
                            else R.string.add_trigger_mode_and_desc,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Save / Cancel ──
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        haptics.heavy()
                        vm.save()
                    },
                    enabled = !state.saving && state.title.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.add_save)) }
                OutlinedButton(onClick = {
                    haptics.tap()
                    onCancel()
                }, modifier = Modifier.weight(1f).testTag("cancelButton")) {
                    Text(stringResource(R.string.add_cancel))
                }
            }

            Spacer(Modifier.height(24.dp))
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private fun showDatePicker(
    context: android.content.Context,
    current: Long?,
    onPicked: (Long) -> Unit,
) {
    val cal = if (current != null) Calendar.getInstance().apply { timeInMillis = current } else Calendar.getInstance()
    DatePickerDialog(context, { _, year, month, day ->
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, day)
        onPicked(cal.timeInMillis)
    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
}

private fun showTimePicker(
    context: android.content.Context,
    current: Long?,
    onPicked: (Long) -> Unit,
) {
    val cal = if (current != null) Calendar.getInstance().apply { timeInMillis = current } else Calendar.getInstance()
    TimePickerDialog(context, { _, hour, minute ->
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        onPicked(cal.timeInMillis)
    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val calA = Calendar.getInstance().apply { timeInMillis = a }
    val calB = Calendar.getInstance().apply { timeInMillis = b }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
        calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
}
