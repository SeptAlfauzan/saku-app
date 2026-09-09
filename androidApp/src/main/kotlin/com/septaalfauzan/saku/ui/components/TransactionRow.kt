package com.septaalfauzan.saku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import com.septaalfauzan.saku.ui.designsystem.categoryGlyph
import com.septaalfauzan.saku.ui.designsystem.categoryLabel
import com.septaalfauzan.saku.util.formatRupiah
import com.septaalfauzan.saku.util.formatShortDate

@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: (() -> Unit)? = null,
    showStatusBadge: Boolean = true,
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    val isIncome = transaction.isIncome
    val title = transaction.merchant ?: transaction.description ?: "Transaction"
    val account = transaction.sourcePackage?.takeLast(12)?.uppercase() ?: transaction.source.name.uppercase()
    val date = formatShortDate(transaction.occurredAt.toEpochMilliseconds())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = SakuDp.spaceSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceSm), modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(40.dp).background(palette.chalk, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    categoryGlyph(transaction.categoryId),
                    contentDescription = categoryLabel(transaction.categoryId),
                    tint = palette.ink,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = type.headlineSm, color = palette.ink, maxLines = 1)
                    if (showStatusBadge && transaction.status == TransactionStatus.PENDING_REVIEW) {
                        Box(Modifier.padding(start = 6.dp).size(6.dp).background(palette.crimson, CircleShape))
                    }
                }
                Text(
                    "${categoryLabel(transaction.categoryId)} • $account • $date",
                    style = type.bodySm,
                    color = palette.slate,
                    maxLines = 1,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (isIncome) "+" else "-") + formatRupiah(transaction.amount),
                style = type.numericTable,
                color = palette.ink,
            )
            if (transaction.source.name != "MANUAL") {
                Text("Auto", style = type.bodySm, color = palette.slate)
            }
        }
    }
}
