package com.septaalfauzan.saku.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.ui.components.EmptyState
import com.septaalfauzan.saku.ui.components.FilterChip
import com.septaalfauzan.saku.ui.components.TransactionRow
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import com.septaalfauzan.saku.util.formatShortDate
import java.time.Instant
import java.time.ZoneId
import org.koin.androidx.compose.koinViewModel

@Composable
fun TransactionListRoute(onOpen: (String) -> Unit, onAdd: () -> Unit) {
    val viewModel: TransactionListViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val palette = SakuTheme.palette
    val type = SakuTheme.type

    Column(Modifier.fillMaxSize().padding(horizontal = SakuDp.screenEdgePadding)) {
        Spacer(Modifier.height(SakuDp.spaceLg))
        Text("Transactions", style = type.headlineLg, color = palette.ink)
        Spacer(Modifier.height(SakuDp.spaceMd))
        Row(horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXs)) {
            TransactionFilter.entries.forEach { filter ->
                FilterChip(
                    label = filter.name,
                    selected = state.filter == filter,
                    onClick = { viewModel.setFilter(filter) },
                )
            }
        }
        Spacer(Modifier.height(SakuDp.spaceSm))

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
    }
}

