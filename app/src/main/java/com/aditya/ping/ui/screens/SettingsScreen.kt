package com.aditya.ping.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aditya.ping.R
import com.aditya.ping.data.ThemeRepository
import com.aditya.ping.domain.ThemeMode
import com.aditya.ping.util.QuietHoursManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val themeRepo = remember { ThemeRepository(context) }
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(themeRepo))
    val current by vm.themeMode.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val quietHours = remember { QuietHoursManager(context) }
    val quietEnabled by quietHours.enabled.collectAsStateWithLifecycle(initialValue = false)
    val quietStart by quietHours.startMinutes.collectAsStateWithLifecycle(initialValue = 1320)
    val quietEnd by quietHours.endMinutes.collectAsStateWithLifecycle(initialValue = 420)

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
            Column(modifier = Modifier.selectableGroup()) {
                ThemeOption(
                    label = stringResource(R.string.settings_theme_system),
                    selected = current == ThemeMode.SYSTEM,
                    onSelect = { vm.setTheme(ThemeMode.SYSTEM) },
                )
                ThemeOption(
                    label = stringResource(R.string.settings_theme_light),
                    selected = current == ThemeMode.LIGHT,
                    onSelect = { vm.setTheme(ThemeMode.LIGHT) },
                )
                ThemeOption(
                    label = stringResource(R.string.settings_theme_dark),
                    selected = current == ThemeMode.DARK,
                    onSelect = { vm.setTheme(ThemeMode.DARK) },
                )
                ThemeOption(
                    label = stringResource(R.string.settings_theme_amoled),
                    selected = current == ThemeMode.AMOLED,
                    onSelect = { vm.setTheme(ThemeMode.AMOLED) },
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.quiet_hours_title), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.quiet_hours_enable), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = quietEnabled,
                    onCheckedChange = { scope.launch { quietHours.setEnabled(it) } },
                )
            }
            if (quietEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.quiet_hours_start), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = {
                        TimePickerDialog(context, { _, h, m ->
                            scope.launch { quietHours.setStartMinutes(h * 60 + m) }
                        }, quietStart / 60, quietStart % 60, true).show()
                    }) {
                        Text(formatTime(quietStart))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.quiet_hours_end), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = {
                        TimePickerDialog(context, { _, h, m ->
                            scope.launch { quietHours.setEndMinutes(h * 60 + m) }
                        }, quietEnd / 60, quietEnd % 60, true).show()
                    }) {
                        Text(formatTime(quietEnd))
                    }
                }
                Text(
                    stringResource(R.string.quiet_hours_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_about_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "%02d:%02d".format(h, m)
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.size(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
