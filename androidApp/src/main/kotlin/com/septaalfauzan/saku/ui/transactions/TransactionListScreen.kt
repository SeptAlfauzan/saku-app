package com.septaalfauzan.saku.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PermContactCalendar
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.septaalfauzan.saku.R
import com.septaalfauzan.saku.ui.components.EmptyState
import com.septaalfauzan.saku.ui.components.FilterChip
import com.septaalfauzan.saku.ui.components.PillButton
import com.septaalfauzan.saku.ui.components.PillButtonVariant
import com.septaalfauzan.saku.ui.components.TopBar
import com.septaalfauzan.saku.ui.components.TransactionRow
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import com.septaalfauzan.saku.util.formatShortDate
import java.time.Instant
import java.time.ZoneId
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionListRoute(
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: TransactionListViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerRangeState = rememberDateRangePickerState()
    val selectedStartDate = datePickerRangeState.selectedStartDateMillis?.let {
        convertMillisToDate(it)
    } ?: ""
    val selectedEndDate = datePickerRangeState.selectedEndDateMillis?.let {
        convertMillisToDate(it)
    } ?: ""


    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.transactions_title),
                onBack = onBack,
                action = {

                    Icon(
                        SakuIcons.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = palette.slate,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onOpenSettings() },
                    )
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = SakuDp.screenEdgePadding)
        ) {
            Spacer(Modifier.height(SakuDp.spaceMd))
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .clickable
                    { showDatePicker = !showDatePicker },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = "filder date button"
                )
                Text(
                    if (selectedStartDate == "" && selectedEndDate == "") "All" else "$selectedStartDate - $selectedEndDate",
                )
            }
            Spacer(Modifier.height(SakuDp.spaceMd))
            Row(horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXs)) {
                TransactionFilter.entries.forEach { filter ->
                    FilterChip(
                        label = stringResource(
                            when (filter) {
                                TransactionFilter.ALL -> R.string.transactions_filter_all
                                TransactionFilter.INCOME -> R.string.common_income
                                TransactionFilter.EXPENSE -> R.string.common_expense
                            },
                        ),
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                    )
                }
            }
            Spacer(Modifier.height(SakuDp.spaceSm))

            if (state.transactions.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.transactions_empty),
                    body = stringResource(R.string.transactions_empty_filtered),
                    ctaLabel = stringResource(R.string.dashboard_add_transaction),
                    onCta = onAdd,
                )
            } else {
                val zone = ZoneId.systemDefault()
                val grouped = remember(state.transactions) {
                    state.transactions.groupBy { tx ->
                        Instant.ofEpochMilli(tx.occurredAt.toEpochMilliseconds())
                            .atZone(zone)
                            .toLocalDate()
                    }.toSortedMap(compareByDescending { it })
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = SakuDp.bottomSafeClearance),
                ) {
                    grouped.forEach { (day, txs) ->
                        item(key = "header-$day") {
                            Text(
                                formatShortDate(day.atStartOfDay(zone).toInstant().toEpochMilli()),
                                style = type.labelCaps,
                                color = palette.slate,
                                modifier = Modifier.padding(top = SakuDp.spaceSm),
                            )
                        }
                        items(txs, key = { it.id }) { tx ->
                            TransactionRow(tx, onClick = { onOpen(tx.id) })
                        }
                    }
                }
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showDatePicker = false
                            if (datePickerRangeState.selectedEndDateMillis != null && datePickerRangeState.selectedStartDateMillis != null) {
                                viewModel.filterByDates(
                                    startDateMils = datePickerRangeState.selectedStartDateMillis!!,
                                    endDateMils = datePickerRangeState.selectedEndDateMillis!!
                                )
                            }
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DateRangePicker(
                        state = datePickerRangeState,
                        title = {
                            Text(
                                text = "Select date range"
                            )
                        },
                        showModeToggle = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}