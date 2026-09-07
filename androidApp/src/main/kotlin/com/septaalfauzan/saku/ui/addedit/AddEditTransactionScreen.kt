package com.septaalfauzan.saku.ui.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.util.formatShortDate
import java.time.Instant
import java.time.ZoneId
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRoute(transactionId: String? = null, onDone: () -> Unit) {
    val viewModel: AddEditTransactionViewModel = koinViewModel(parameters = { parametersOf(transactionId) })
    val state by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()

    LaunchedEffect(event) {
        when (event) {
            AddEditEvent.Saved -> onDone()
            AddEditEvent.NavigateBack -> onDone()
            null -> Unit
        }
        if (event != null) viewModel.consumeEvent()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TypeSelector(
                type = state.type,
                onSelect = viewModel::updateType,
            )
        }

        item {
            val amountError = state.amountError
            OutlinedTextField(
                value = state.amountInput,
                onValueChange = viewModel::updateAmount,
                label = { Text("Amount") },
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Text("Category", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = category.id == state.categoryId,
                        onClick = { viewModel.updateCategory(category.id) },
                        label = { Text(category.name) },
                    )
                }
            }
            val categoryError = state.categoryError
            if (categoryError != null) {
                Text(
                    text = categoryError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.merchant,
                onValueChange = viewModel::updateMerchant,
                label = { Text("Merchant") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::updateNote,
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            DateField(
                occurredAtMillis = state.occurredAtMillis,
                onPick = viewModel::updateDate,
            )
        }

        item {
            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.editingId == null) "Save" else "Update")
            }
        }
    }
}

@Composable
private fun TypeSelector(
    type: TransactionType,
    onSelect: (TransactionType) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        val options = listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income")
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = type == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    occurredAtMillis: Long,
    onPick: (Long) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        val zone = ZoneId.systemDefault()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = occurredAtMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = pickerState.selectedDateMillis
                        if (selected != null) {
                            val localDate = Instant.ofEpochMilli(selected)
                                .atZone(zone)
                                .toLocalDate()
                            val dateTime = localDate.atStartOfDay(zone)
                            onPick(dateTime.toInstant().toEpochMilli())
                        }
                        showPicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Date", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(12.dp))
        TextButton(onClick = { showPicker = true }) {
            Text(formatShortDate(occurredAtMillis))
        }
    }
}
