package com.septaalfauzan.saku.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.util.formatRupiah
import com.septaalfauzan.saku.util.formatShortDate
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DetailRoute(transactionId: String, onEdit: () -> Unit, onDeleted: () -> Unit) {
    val viewModel: TransactionDetailViewModel = koinViewModel(parameters = { parametersOf(transactionId) })
    val tx by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()

    LaunchedEffect(event) {
        when (event) {
            DetailEvent.Deleted -> onDeleted()
            DetailEvent.EditRequested -> onEdit()
            null -> Unit
        }
        viewModel.consumeEvent()
    }

    val transaction = tx
    if (transaction == null) {
        Text("Transaction not found", modifier = Modifier.padding(24.dp))
        return
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            transaction.merchant ?: transaction.description ?: "Transaction",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (transaction.isIncome) "+${formatRupiah(transaction.amount)}"
            else "-${formatRupiah(transaction.amount)}",
            style = MaterialTheme.typography.headlineMedium,
            color = if (transaction.isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("Date", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(formatShortDate(transaction.occurredAt.toEpochMilliseconds()), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("Source", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("Manual", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = viewModel::requestEdit, modifier = Modifier.fillMaxWidth()) {
            Text("Edit")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Delete")
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete transaction?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.requestDelete()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}
