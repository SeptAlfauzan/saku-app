package com.septaalfauzan.saku.notification.normalizer

import com.septaalfauzan.saku.domain.model.TransactionType

object TypeDetector {
    private val incomeWords = listOf("transfer masuk", "dana masuk", "diterima", "credit", "pemasukan")
    private val expenseWords = listOf("pembayaran", "pembelian", "debit", "transaksi kartu", "charged", "card was")

    fun detect(text: String): TransactionType? {
        val lower = text.lowercase()
        return when {
            incomeWords.any { lower.contains(it) } -> TransactionType.INCOME
            expenseWords.any { lower.contains(it) } -> TransactionType.EXPENSE
            else -> null
        }
    }
}
