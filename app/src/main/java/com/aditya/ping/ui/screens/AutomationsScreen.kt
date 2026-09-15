package com.aditya.ping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aditya.ping.R
import com.aditya.ping.data.AutomationEntity
import com.aditya.ping.data.AutomationRepository
import com.aditya.ping.ui.components.BannerAd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationsScreen() {
    val context = LocalContext.current
    val repo = remember { AutomationRepository.from(context) }
    val vm: AutomationsViewModel = viewModel(factory = AutomationsViewModel.Factory(repo))
    val automations by vm.automations.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (automations.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp).padding(16.dp),
                )
                Text(
                    text = stringResource(R.string.automations_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.automations_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(automations, key = { it.id }) { automation ->
                    AutomationCard(
                        automation = automation,
                        onToggle = { vm.toggleEnabled(automation.id, it) },
                        onDelete = { vm.delete(automation.id) },
                    )
                }
                // Banner ad at bottom of automations list
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    BannerAd()
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.automation_add))
        }
    }

    if (showAddDialog) {
        AddAutomationDialog(
            onDismiss = { showAddDialog = false },
            onSave = { automation ->
                vm.add(automation)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun AutomationCard(
    automation: AutomationEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val (triggerIcon, triggerLabel) = triggerInfo(automation.triggerType)
    val actionLabel = actionLabel(automation.actionType)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(triggerIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(automation.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text("$triggerLabel → $actionLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = automation.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddAutomationDialog(
    onDismiss: () -> Unit,
    onSave: (AutomationEntity) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var triggerType by remember { mutableIntStateOf(2) } // default: wifi connect
    var triggerData by remember { mutableStateOf("") }
    var actionType by remember { mutableIntStateOf(0) } // default: notification
    var actionData by remember { mutableStateOf("") }
    var actionMessage by remember { mutableStateOf("") }

    val triggers = listOf(
        0 to stringResource(R.string.trigger_location_arrive),
        1 to stringResource(R.string.trigger_location_leave),
        2 to stringResource(R.string.trigger_wifi_connect),
        3 to stringResource(R.string.trigger_wifi_disconnect),
        4 to stringResource(R.string.trigger_bluetooth_connect),
        5 to stringResource(R.string.trigger_nfc_tag),
        6 to stringResource(R.string.trigger_webhook),
    )
    val actions = listOf(
        0 to stringResource(R.string.action_notification),
        1 to stringResource(R.string.action_call),
        2 to stringResource(R.string.action_whatsapp),
        3 to stringResource(R.string.action_sms),
        4 to stringResource(R.string.action_open_app),
        5 to stringResource(R.string.action_navigate),
        6 to stringResource(R.string.action_url),
        7 to stringResource(R.string.action_toggle_wifi),
        8 to stringResource(R.string.action_toggle_bluetooth),
        9 to stringResource(R.string.action_silent_mode),
        10 to stringResource(R.string.action_volume),
        11 to stringResource(R.string.action_webhook),
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.automation_add),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Scrollable form body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.automation_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))

                    // Trigger section
                    Text(
                        stringResource(R.string.automation_trigger),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        triggers.forEach { (type, label) ->
                            androidx.compose.material3.FilterChip(
                                selected = triggerType == type,
                                onClick = { triggerType = type },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = triggerData,
                        onValueChange = { triggerData = it },
                        label = { Text(triggerHint(triggerType)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))

                    // Action section
                    Text(
                        stringResource(R.string.automation_action),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        actions.forEach { (type, label) ->
                            androidx.compose.material3.FilterChip(
                                selected = actionType == type,
                                onClick = { actionType = type },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                    if (actionType !in listOf(0, 7, 8, 9)) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = actionData,
                            onValueChange = { actionData = it },
                            label = { Text(actionHint(actionType)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (actionType == 2 || actionType == 3) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = actionMessage,
                            onValueChange = { actionMessage = it },
                            label = { Text(stringResource(R.string.quick_action_message_hint)) },
                            modifier = Modifier.fillMaxWidth().height(72.dp),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // Action buttons (always visible, not scrolled)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.add_cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    AutomationEntity(
                                        name = name.trim(),
                                        triggerType = triggerType,
                                        triggerData = triggerData.trim(),
                                        actionType = actionType,
                                        actionData = actionData.trim(),
                                        actionMessage = actionMessage.trim(),
                                    ),
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                    ) { Text(stringResource(R.string.add_save)) }
                }
            }
        }
    }
}

@Composable
private fun triggerInfo(type: Int): Pair<ImageVector, String> = when (type) {
    0, 1 -> Icons.Filled.LocationOn to stringResource(R.string.trigger_location_arrive)
    2, 3 -> Icons.Filled.Wifi to stringResource(R.string.trigger_wifi_connect)
    4 -> Icons.Filled.Bluetooth to stringResource(R.string.trigger_bluetooth_connect)
    5 -> Icons.Filled.Nfc to stringResource(R.string.trigger_nfc_tag)
    6 -> Icons.Filled.Webhook to stringResource(R.string.trigger_webhook)
    else -> Icons.Filled.AutoAwesome to "Unknown"
}

@Composable
private fun triggerHint(type: Int): String = when (type) {
    0, 1 -> stringResource(R.string.trigger_hint_location)
    2, 3 -> stringResource(R.string.trigger_hint_wifi)
    4 -> stringResource(R.string.trigger_hint_bluetooth)
    5 -> stringResource(R.string.trigger_hint_nfc)
    6 -> stringResource(R.string.trigger_hint_webhook)
    else -> ""
}

@Composable
private fun actionLabel(type: Int): String = when (type) {
    0 -> stringResource(R.string.action_notification)
    1 -> stringResource(R.string.action_call)
    2 -> stringResource(R.string.action_whatsapp)
    3 -> stringResource(R.string.action_sms)
    4 -> stringResource(R.string.action_open_app)
    5 -> stringResource(R.string.action_navigate)
    6 -> stringResource(R.string.action_url)
    7 -> stringResource(R.string.action_toggle_wifi)
    8 -> stringResource(R.string.action_toggle_bluetooth)
    9 -> stringResource(R.string.action_silent_mode)
    10 -> stringResource(R.string.action_volume)
    11 -> stringResource(R.string.action_webhook)
    else -> ""
}

@Composable
private fun actionHint(type: Int): String = when (type) {
    1 -> stringResource(R.string.quick_action_call_hint)
    2 -> stringResource(R.string.quick_action_whatsapp_hint)
    3 -> stringResource(R.string.quick_action_sms_hint)
    4 -> stringResource(R.string.quick_action_app_hint)
    5 -> stringResource(R.string.trigger_hint_location)
    6 -> stringResource(R.string.quick_action_url_hint)
    10 -> stringResource(R.string.action_hint_volume)
    11 -> stringResource(R.string.action_hint_webhook)
    else -> ""
}
