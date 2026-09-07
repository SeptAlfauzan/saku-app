package com.septaalfauzan.saku.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.septaalfauzan.saku.util.formatRupiah

@Composable
fun AmountText(
    amount: Long,
    sign: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    Text(
        text = "$sign${formatRupiah(amount)}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}
