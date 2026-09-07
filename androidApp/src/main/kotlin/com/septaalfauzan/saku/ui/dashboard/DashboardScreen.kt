package com.septaalfauzan.saku.ui.dashboard

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.ui.components.AmountText
import com.septaalfauzan.saku.ui.components.EmptyState
import com.septaalfauzan.saku.ui.components.TransactionRow
import com.septaalfauzan.saku.util.formatRupiah
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardRoute(onAdd: () -> Unit) {
    val viewModel: DashboardViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(state.monthLabel, style = MaterialTheme.typography.headlineSmall)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Balance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    formatRupiah(state.balance),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Income", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AmountText(state.income, "+")
            }
            Column {
                Text("Expense", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AmountText(state.expense, "-")
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)

        if (state.recentTransactions.isEmpty()) {
            EmptyState(
                title = "No transactions yet",
                body = "Record your first income or expense.",
                ctaLabel = "Add Transaction",
                onCta = onAdd,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.recentTransactions, key = { it.id }) { tx ->
                    TransactionRow(tx)
                }
            }
        }
    }
}
