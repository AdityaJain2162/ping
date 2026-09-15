package com.aditya.ping.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aditya.ping.R
import com.aditya.ping.data.SavedPlaceRepository
import com.aditya.ping.ui.components.BannerAd
import com.aditya.ping.util.GeoCoderUtil
import com.aditya.ping.util.LocationUtil
import com.aditya.ping.util.PermissionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPlacesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val repo = remember { SavedPlaceRepository.from(context) }
    val vm: SavedPlacesViewModel = viewModel(factory = SavedPlacesViewModel.Factory(repo, appContext))
    val places by vm.places.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var newName by remember { mutableStateOf("") }
    var newRadius by remember { mutableStateOf("150") }
    var pendingLocation by remember { mutableStateOf<Triple<Double, Double, String>?>(null) }
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

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            val util = LocationUtil(context)
            scope.launch {
                val loc = withContext(Dispatchers.IO) { util.currentLocation() }
                if (loc != null) pendingLocation = Triple(loc.latitude, loc.longitude, "Current location")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saved_places_title)) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // --- Add new place ---
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(stringResource(R.string.saved_places_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            // Location search
            OutlinedTextField(
                value = locationSearchQuery,
                onValueChange = { locationSearchQuery = it },
                label = { Text(stringResource(R.string.add_location_search)) },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            locationSearchResults.forEach { result ->
                OutlinedButton(
                    onClick = {
                        pendingLocation = Triple(result.lat, result.lng, result.label)
                        locationSearchQuery = ""
                        locationSearchResults = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(result.label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    if (!PermissionUtil.hasFineLocation(context)) {
                        locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                    } else {
                        val util = LocationUtil(context)
                        scope.launch {
                            val loc = withContext(Dispatchers.IO) { util.currentLocation() }
                            if (loc != null) pendingLocation = Triple(loc.latitude, loc.longitude, "Current location")
                        }
                    }
                }) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.saved_places_pick))
                }
                pendingLocation?.let { (lat, lng, label) ->
                    Spacer(Modifier.size(12.dp))
                    Text(
                        "%.4f, %.4f".format(lat, lng),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedTextField(
                value = newRadius,
                onValueChange = { newRadius = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.saved_places_radius)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = {
                    val loc = pendingLocation ?: return@OutlinedButton
                    val radius = newRadius.toIntOrNull() ?: 150
                    vm.add(newName, loc.first, loc.second, loc.third, radius)
                    newName = ""
                    newRadius = "150"
                    pendingLocation = null
                },
                enabled = newName.isNotBlank() && pendingLocation != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.saved_places_add))
            }

            // --- List ---
            if (places.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.saved_places_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BannerAd(modifier = Modifier.fillMaxWidth().padding(16.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(places, key = { it.id }) { place ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp),
                                )
                                Spacer(Modifier.size(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(place.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        place.addressLabel.ifBlank { "%.4f, %.4f".format(place.lat, place.lng) },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        "${place.radiusMeters} m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { vm.delete(place.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.add_cancel))
                                }
                            }
                        }
                    }
                    // Banner ad at bottom of saved places
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        BannerAd()
                    }
                }
            }
        }
    }
}
