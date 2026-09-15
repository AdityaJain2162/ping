package com.aditya.ping.ui.components

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.aditya.ping.R
import com.aditya.ping.ui.theme.LocalHaptics
import com.aditya.ping.util.GeoCoderUtil
import com.aditya.ping.util.GeoResult
import com.aditya.ping.util.LocationUtil
import com.aditya.ping.util.PermissionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Reusable location picker with address search and current-location button.
 *
 * @param lat       Current latitude (0.0 if unset).
 * @param lng       Current longitude (0.0 if unset).
 * @param label     Current human-readable label for the selected location.
 * @param onPicked  Called with (lat, lng, label) when the user picks a location.
 */
@Composable
fun LocationPickerField(
    lat: Double,
    lng: Double,
    label: String,
    onPicked: (lat: Double, lng: Double, label: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeoResult>>(emptyList()) }
    var showLocationDisabledDialog by remember { mutableStateOf(false) }
    val geoCoder = remember { GeoCoderUtil(context) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 3) {
            searchResults = geoCoder.search(searchQuery)
        } else {
            searchResults = emptyList()
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            val util = LocationUtil(context)
            scope.launch {
                val loc = withContext(Dispatchers.IO) { util.currentLocation() }
                if (loc != null) onPicked(loc.latitude, loc.longitude, "Current location")
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(stringResource(R.string.add_location_search)) },
            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (searchResults.isNotEmpty()) {
            searchResults.forEach { result ->
                TextButton(
                    onClick = {
                        haptics.tap()
                        onPicked(result.lat, result.lng, result.label)
                        searchQuery = ""
                        searchResults = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        result.label,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
            }
        }
        OutlinedButton(onClick = {
            haptics.tap()
            val util = LocationUtil(context)
            if (!util.isLocationEnabled()) {
                showLocationDisabledDialog = true
            } else if (!PermissionUtil.hasFineLocation(context)) {
                locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            } else {
                scope.launch {
                    val loc = withContext(Dispatchers.IO) { util.currentLocation() }
                    if (loc != null) onPicked(loc.latitude, loc.longitude, "Current location")
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.MyLocation, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.add_pick_current))
        }
        if (lat != 0.0 || lng != 0.0) {
            Spacer(Modifier.height(4.dp))
            Text(
                label.ifBlank { "%.4f, %.4f".format(lat, lng) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
