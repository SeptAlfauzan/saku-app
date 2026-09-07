package com.septaalfauzan.saku.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.ui.components.EmptyState
import com.septaalfauzan.saku.ui.components.TransactionRow
import com.septaalfauzan.saku.util.formatShortDate
import java.time.ZoneId
import org.koin.androidx.compose.koinViewModel

@Composable
fun TransactionListRoute(onOpen: (String) -> Unit, onAdd: () -> Unit) {
    val viewModel: TransactionListViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Transactions", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransactionFilter.entries.forEach { filter ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (state.filter == filter) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    onClick = { viewModel.setFilter(filter) },
                ) {
                    Text(
                        filter.name,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (state.transactions.isEmpty()) {
            EmptyState(
                title = "No transactions",
                body = "Nothing matches this filter yet.",
                ctaLabel = "Add Transaction",
                onCta = onAdd,
            )
        } else {
            val zone = ZoneId.systemDefault()
            val grouped = remember(state.transactions) {
                state.transactions.groupBy { tx ->
                    java.time.Instant.ofEpochMilli(tx.occurredAt.toEpochMilliseconds())
                        .atZone(zone)
                        .toLocalDate()
                }.toSortedMap(compareByDescending { it })
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                grouped.forEach { (day, txs) ->
                    item(key = "header-$day") {
                        Text(
                            formatShortDate(day.atStartOfDay(zone).toInstant().toEpochMilli()),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(txs, key = { it.id }) { tx ->
                        TransactionRow(tx, onClick = { onOpen(tx.id) })
                    }
                }
            }
        }
    }
}