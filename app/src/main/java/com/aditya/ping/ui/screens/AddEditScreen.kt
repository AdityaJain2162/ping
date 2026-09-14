package com.aditya.ping.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aditya.ping.R
import com.aditya.ping.data.ReminderRepository
import com.aditya.ping.util.AlarmScheduler
import com.aditya.ping.util.LocationUtil
import com.aditya.ping.util.PermissionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    reminderId: Long,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
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

    val dateFmt = remember { SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault()) }

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
                        dateFmt.format(Date(state.dueAt!!)),
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

            // --- Location trigger section ---
            Text(stringResource(R.string.add_location_label), style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = {
                if (!PermissionUtil.hasFineLocation(context)) {
                    locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                } else {
                    val util = LocationUtil(context)
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = vm::save,
                    enabled = !state.saving && state.title.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.add_save)) }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.add_cancel))
                }
            }
        }
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
