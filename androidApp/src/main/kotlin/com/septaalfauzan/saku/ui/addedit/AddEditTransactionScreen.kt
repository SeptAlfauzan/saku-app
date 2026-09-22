package com.septaalfauzan.saku.ui.addedit

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import com.septaalfauzan.saku.ui.components.FilterChip
import com.septaalfauzan.saku.ui.components.PillBackplate
import com.septaalfauzan.saku.ui.components.PillButton
import com.septaalfauzan.saku.ui.components.PillButtonVariant
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import androidx.compose.ui.res.stringResource
import com.septaalfauzan.saku.R
import com.septaalfauzan.saku.domain.model.AddEditUiState
import com.septaalfauzan.saku.ui.components.NumberVisualTransformation
import com.septaalfauzan.saku.ui.components.TopBar
import com.septaalfauzan.saku.util.formatShortDate
import java.time.Instant
import java.time.ZoneId
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRoute(
    transactionId: String? = null,
    prefillJson: String? = null,
    editingScan: Boolean = false,
    onDone: (AddEditUiState) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: AddEditTransactionViewModel = koinViewModel(
        parameters = { parametersOf(transactionId, prefillJson, editingScan) },
    )
    val state by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()

    LaunchedEffect(event) {
        when (event) {
            AddEditEvent.Saved -> onDone(state)
            AddEditEvent.NavigateBack -> onBack()
            null -> Unit
        }
        if (event != null) viewModel.consumeEvent()
    }

    Scaffold(
        topBar = {
            TopBar(
                title =
                    if (transactionId != null) stringResource(R.string.addedit_edit_title) else stringResource(
                        R.string.addedit_add_title
                    ),
                onBack = { onBack() },
                action = {}
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = SakuDp.screenEdgePadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = SakuDp.bottomSafeClearance),
        ) {
            item {
                TypeSelector(type = state.type, onSelect = viewModel::updateType)
            }
            item {
                PillBackplate {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Rp",
                            style = SakuTheme.type.numericTable,
                            color = SakuTheme.palette.slate
                        )
                        Spacer(Modifier.width(SakuDp.spaceSm))
                        BasicTextField(
                            value = state.amountInput,
                            onValueChange = viewModel::updateAmount,
                            textStyle = SakuTheme.type.displayCurrencyMobile.copy(
                                color = SakuTheme.palette.ink,
                                fontFeatureSettings = "tnum",
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            visualTransformation = NumberVisualTransformation()
                        )
                    }
                }
                state.amountError?.let {
                    Text(
                        it,
                        style = SakuTheme.type.bodySm,
                        color = SakuTheme.palette.crimson,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.addedit_category),
                    style = SakuTheme.type.labelCaps,
                    color = SakuTheme.palette.slate
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXs)) {
                    items(state.categories, key = { it.id }) { category ->
                        FilterChip(
                            label = category.name,
                            selected = category.id == state.categoryId,
                            onClick = { viewModel.updateCategory(category.id) },
                        )
                    }
                }
                state.categoryError?.let {
                    Text(
                        it,
                        style = SakuTheme.type.bodySm,
                        color = SakuTheme.palette.crimson,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            item {
                PillBackplate {
                    BasicTextField(
                        value = state.merchant,
                        onValueChange = viewModel::updateMerchant,
                        textStyle = SakuTheme.type.bodyLg.copy(color = SakuTheme.palette.ink),
                        decorationBox = { inner ->
                            if (state.merchant.isEmpty()) {
                                Text(
                                    stringResource(R.string.addedit_merchant_hint),
                                    style = SakuTheme.type.bodyLg,
                                    color = SakuTheme.palette.slate
                                )
                            }
                            inner()
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                PillBackplate {
                    BasicTextField(
                        value = state.note,
                        onValueChange = viewModel::updateNote,
                        textStyle = SakuTheme.type.bodyLg.copy(color = SakuTheme.palette.ink),
                        decorationBox = { inner ->
                            if (state.note.isEmpty()) {
                                Text(
                                    stringResource(R.string.addedit_note_hint),
                                    style = SakuTheme.type.bodyLg,
                                    color = SakuTheme.palette.slate
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                DateField(state.occurredAtMillis, viewModel::updateDate)
            }
            item {
                PillButton(
                    text = if (transactionId != null) stringResource(R.string.addedit_update) else stringResource(
                        R.string.addedit_save
                    ),
                    onClick = viewModel::save,
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth(),
                    variant = if (state.type == TransactionType.EXPENSE) PillButtonVariant.ACCENT else PillButtonVariant.PRIMARY,
                )
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
        val options = listOf(
            TransactionType.EXPENSE to stringResource(R.string.common_expense),
            TransactionType.INCOME to stringResource(R.string.common_income)
        )
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
                    Text(stringResource(R.string.common_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPicker = false
                }) { Text(stringResource(R.string.common_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.common_date),
            style = SakuTheme.type.labelCaps,
            color = SakuTheme.palette.slate
        )
        Spacer(Modifier.width(12.dp))
        TextButton(onClick = { showPicker = true }) {
            Text(
                formatShortDate(occurredAtMillis),
                style = SakuTheme.type.labelMd,
                color = SakuTheme.palette.crimson
            )
        }
    }
}