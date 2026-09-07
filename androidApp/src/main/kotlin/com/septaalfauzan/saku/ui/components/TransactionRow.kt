package com.septaalfauzan.saku.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.util.formatRupiah
import com.septaalfauzan.saku.util.formatShortDate

@Composable
fun TransactionRow(transaction: Transaction, onClick: (() -> Unit)? = null) {
    val isIncome = transaction.isIncome
    val title = transaction.merchant
        ?: transaction.description
        ?: "Transaction"
    val date = formatShortDate(transaction.occurredAt.toEpochMilliseconds())
    val amountColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = if (isIncome) "+${formatRupiah(transaction.amount)}"
                else "-${formatRupiah(transaction.amount)}",
                style = MaterialTheme.typography.titleSmall,
                color = amountColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
