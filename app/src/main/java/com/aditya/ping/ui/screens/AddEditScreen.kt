package com.aditya.ping.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aditya.ping.R
import com.aditya.ping.data.ReminderRepository
import com.aditya.ping.util.AlarmScheduler
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
    val appContext = context.applicationContext
    val repo = remember { ReminderRepository.from(context) }
    val vm: AddEditViewModel = viewModel(factory = AddEditViewModel.Factory(repo, appContext))
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(reminderId) { vm.load(reminderId) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            val util = LocationUtil(context)
            scope.launch {
                val loc = withContext(Dispatchers.IO) { util.currentLocation() }
                if (loc != null) vm.onLocation(loc.latitude, loc.longitude, "Current location")
            }
        }
    }

    var showLocationDisabledDialog by remember { mutableStateOf(false) }
    var locationSearchQuery by remember { mutableStateOf("") }
    var locationSearchResults by remember { mutableStateOf<List<com.aditya.ping.util.GeoResult>>(emptyList()) }
    val geoCoder = remember { GeoCoderUtil(context) }

    LaunchedEffect(locationSearchQuery) {
        if (locationSearchQuery.length >= 3) {
            locationSearchResults = geoCoder.search(locationSearchQuery)
        } else {
            locationSearchResults = emptyList()
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Natural language quick-add (only when creating new)
            if (!state.isEdit) {
                var nlInput by remember { mutableStateOf(sharedText ?: "") }
                var nlParsed by remember { mutableStateOf(false) }
                LaunchedEffect(sharedText) {
                    if (!sharedText.isNullOrBlank() && !nlParsed) {
                        val parsed = com.aditya.ping.util.NaturalLanguageParser.parse(sharedText)
                        if (parsed.title.isNotBlank()) vm.onTitleChange(parsed.title)
                        parsed.dueAt?.let { vm.onDueAtChange(it) }
                        if (parsed.recurrenceType != 0) vm.onRecurrenceTypeChange(parsed.recurrenceType)
                        if (parsed.isAlarm) vm.onAlarmToggle(true)
                        parsed.triggerType?.let { vm.onTriggerChange(it) }
                        if (parsed.addressLabel.isNotBlank()) vm.onLocation(0.0, 0.0, parsed.addressLabel)
                        nlParsed = true
                    }
                }
                OutlinedTextField(
                    value = nlInput,
                    onValueChange = { nlInput = it; nlParsed = false },
                    label = { Text(stringResource(R.string.add_quick_add)) },
                    placeholder = { Text(stringResource(R.string.add_quick_add_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                val parsed = com.aditya.ping.util.NaturalLanguageParser.parse(nlInput)
                                if (parsed.title.isNotBlank()) vm.onTitleChange(parsed.title)
                                parsed.dueAt?.let { vm.onDueAtChange(it) }
                                if (parsed.recurrenceType != 0) vm.onRecurrenceTypeChange(parsed.recurrenceType)
                                if (parsed.isAlarm) vm.onAlarmToggle(true)
                                parsed.triggerType?.let { vm.onTriggerChange(it) }
                                if (parsed.addressLabel.isNotBlank()) vm.onLocation(0.0, 0.0, parsed.addressLabel)
                                nlParsed = true
                            },
                        ) {
                            Text(stringResource(R.string.add_quick_add_parse))
                        }
                    },
                )
                if (nlParsed) {
                    Text(
                        stringResource(R.string.add_quick_add_parsed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::onTitleChange,
                label = { Text(stringResource(R.string.add_title_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = vm::onNoteChange,
                label = { Text(stringResource(R.string.add_note_label)) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )

            // --- Time trigger section ---
            Text(stringResource(R.string.add_time_label), style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                if (state.dueAt != null) {
                    Text(
                        UiFormats.formatReminderDate(state.dueAt!!),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { vm.onDueAtChange(null) }) {
                        Text(stringResource(R.string.add_time_clear))
                    }
                } else {
                    OutlinedButton(onClick = { showDateTimePicker(context, vm::onDueAtChange) }) {
                        Text(stringResource(R.string.add_time_pick))
                    }
                }
            }

            // --- Alarm toggle (only shown when time is set) ---
            if (state.dueAt != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.add_alarm_toggle), style = MaterialTheme.typography.bodyMedium)
                    androidx.compose.material3.Switch(
                        checked = state.isAlarm,
                        onCheckedChange = vm::onAlarmToggle,
                    )
                }
                if (state.isAlarm) {
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
                        val uri = result.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                        if (uri != null) {
                            vm.onRingtoneUriChange(uri.toString())
                        }
                    }
                    OutlinedButton(onClick = {
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
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp),
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
                            androidx.compose.material3.FilterChip(
                                selected = state.antiSleepDismiss == type,
                                onClick = { vm.onAntiSleepChange(type) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }
            }

            // --- Recurrence section (only shown when time is set) ---
            if (state.dueAt != null) {
                Text(stringResource(R.string.add_recurrence_label), style = MaterialTheme.typography.labelLarge)
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
                            onClick = { vm.onRecurrenceTypeChange(type) },
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
                            onClick = { vm.onRecurrenceTypeChange(type) },
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

            // --- Nag mode section ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.add_nag_toggle), style = MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.Switch(
                    checked = state.nagMode,
                    onCheckedChange = vm::onNagModeToggle,
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

            // --- Location trigger section ---
            Text(stringResource(R.string.add_location_label), style = MaterialTheme.typography.labelLarge)

            // Search any address
            OutlinedTextField(
                value = locationSearchQuery,
                onValueChange = { locationSearchQuery = it },
                label = { Text(stringResource(R.string.add_location_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (locationSearchResults.isNotEmpty()) {
                locationSearchResults.forEach { result ->
                    TextButton(
                        onClick = {
                            vm.onLocation(result.lat, result.lng, result.label)
                            locationSearchQuery = ""
                            locationSearchResults = emptyList()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            result.label,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                        )
                    }
                }
            }

            // Use current location button
            OutlinedButton(onClick = {
                val util = LocationUtil(context)
                if (!util.isLocationEnabled()) {
                    showLocationDisabledDialog = true
                } else if (!PermissionUtil.hasFineLocation(context)) {
                    locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                } else {
                    scope.launch {
                        val loc = withContext(Dispatchers.IO) { util.currentLocation() }
                        if (loc != null) vm.onLocation(loc.latitude, loc.longitude, "Current location")
                    }
                }
            }) {
                Icon(Icons.Filled.MyLocation, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.add_pick_current))
            }
            if (state.lat != 0.0 || state.lng != 0.0) {
                Text(
                    state.addressLabel.ifBlank { "%.4f, %.4f".format(state.lat, state.lng) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(stringResource(R.string.add_trigger_label), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.triggerType == 0,
                    onClick = { vm.onTriggerChange(0) },
                    label = { Text(stringResource(R.string.add_trigger_arrive)) },
                )
                FilterChip(
                    selected = state.triggerType == 1,
                    onClick = { vm.onTriggerChange(1) },
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

            // --- Quick action section ---
            Text(stringResource(R.string.add_quick_action), style = MaterialTheme.typography.labelLarge)
            val quickActions = listOf(
                0 to stringResource(R.string.quick_action_none),
                1 to stringResource(R.string.quick_action_call),
                2 to stringResource(R.string.quick_action_whatsapp),
                6 to stringResource(R.string.quick_action_sms),
                7 to stringResource(R.string.quick_action_whatsapp_group),
                3 to stringResource(R.string.quick_action_open_app),
                4 to stringResource(R.string.quick_action_navigate),
                5 to stringResource(R.string.quick_action_url),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                quickActions.take(4).forEach { (type, label) ->
                    FilterChip(
                        selected = state.quickActionType == type,
                        onClick = { vm.onQuickActionTypeChange(type) },
                        label = { Text(label) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                quickActions.drop(4).forEach { (type, label) ->
                    FilterChip(
                        selected = state.quickActionType == type,
                        onClick = { vm.onQuickActionTypeChange(type) },
                        label = { Text(label) },
                    )
                }
            }
            if (state.quickActionType != 0 && state.quickActionType != 4) {
                OutlinedTextField(
                    value = state.quickActionData,
                    onValueChange = vm::onQuickActionDataChange,
                    label = {
                        Text(
                            when (state.quickActionType) {
                                1 -> stringResource(R.string.quick_action_call_hint)
                                2 -> stringResource(R.string.quick_action_whatsapp_hint)
                                6 -> stringResource(R.string.quick_action_sms_hint)
                                7 -> stringResource(R.string.quick_action_whatsapp_group_hint)
                                3 -> stringResource(R.string.quick_action_app_hint)
                                5 -> stringResource(R.string.quick_action_url_hint)
                                else -> ""
                            },
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Message field for WhatsApp and SMS
                if (state.quickActionType == 2 || state.quickActionType == 6) {
                    OutlinedTextField(
                        value = state.quickActionMessage,
                        onValueChange = vm::onQuickActionMessageChange,
                        label = { Text(stringResource(R.string.quick_action_message_hint)) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                    )
                }
            }

            // --- Combined trigger mode (only when both time and location are set) ---
            val hasLocation = state.lat != 0.0 || state.lng != 0.0
            val hasTime = state.dueAt != null
            if (hasLocation && hasTime) {
                Text(stringResource(R.string.add_trigger_mode), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.triggerMode == 0,
                        onClick = { vm.onTriggerModeChange(0) },
                        label = { Text(stringResource(R.string.add_trigger_mode_or)) },
                    )
                    FilterChip(
                        selected = state.triggerMode == 1,
                        onClick = { vm.onTriggerModeChange(1) },
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = vm::save,
                    enabled = !state.saving && state.title.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.add_save)) }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).testTag("cancelButton")) {
                    Text(stringResource(R.string.add_cancel))
                }
            }

            Spacer(Modifier.height(24.dp))
            com.aditya.ping.ui.components.BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }

    if (showLocationDisabledDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDisabledDialog = false },
            title = { Text(stringResource(R.string.location_off_title)) },
            text = { Text(stringResource(R.string.location_off_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLocationDisabledDialog = false
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) {
                    Text(stringResource(R.string.location_off_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDisabledDialog = false }) {
                    Text(stringResource(R.string.add_cancel))
                }
            },
        )
    }
}

private fun showDateTimePicker(
    context: android.content.Context,
    onPicked: (Long) -> Unit,
) {
    val cal = Calendar.getInstance()
    DatePickerDialog(context, { _, year, month, day ->
        cal.set(year, month, day)
        TimePickerDialog(context, { _, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            onPicked(cal.timeInMillis)
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
}
