package com.septaalfauzan.saku.ui.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.notification.NotificationAccessManager
import com.septaalfauzan.saku.ui.components.EmptyState
import org.koin.androidx.compose.koinViewModel

@Composable
fun TrackingRoute(onBack: () -> Unit) {
    val viewModel: TrackingViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val accessGranted = remember { NotificationAccessManager.isListening(context) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Automatic Tracking", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(8.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Reads notifications from selected financial apps to automatically record transactions. " +
                        "Notifications are processed locally whenever possible.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (accessGranted) {
                    Text("Notification access granted", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Notification access not granted", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { NotificationAccessManager.openSettings(context) }) {
                        Text("Enable Notification Access")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Automatic Tracking", style = MaterialTheme.typography.titleMedium)
            Switch(checked = state.trackingEnabled, onCheckedChange = viewModel::setTrackingEnabled)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Auto-confirm high confidence", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = state.autoConfirm, onCheckedChange = viewModel::setAutoConfirm)
        }

        Spacer(Modifier.height(12.dp))
        Text("Monitored Apps", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        if (state.sources.isEmpty()) {
            EmptyState(title = "No apps", body = "Enable a financial app to track.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.sources, key = { it.packageName }) { source ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(source.providerId, style = MaterialTheme.typography.bodyLarge)
                            Text(source.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = source.enabled, onCheckedChange = { enabled -> viewModel.setSourceEnabled(source.packageName, enabled) })
                    }
                }
            }
        }
    }
}
