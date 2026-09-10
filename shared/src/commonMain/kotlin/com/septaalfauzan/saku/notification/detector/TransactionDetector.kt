package com.septaalfauzan.saku.notification.detector

import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.notification.normalizer.AmountNormalizer
import com.septaalfauzan.saku.util.Logger

object TransactionDetector {
    private val transactionKeywords = listOf(
        "pembayaran", "pembelian", "debit", "transfer masuk", "dana masuk",
        "diterima", "credit", "transaksi kartu", "dompet terisi", "pengeluaran"
    )
    private val rejectedKeywords = listOf(
        "gagal", "ditolak", "promo", "cashback", "diskon", "laporan", "statement",
    )

    fun isTransaction(data: NotificationData): Boolean {
        val text = buildString {
            data.title?.let { append(it).append("\n") }
            data.body?.let { append(it) }
        }
        if (AmountNormalizer.firstToken(text) == null) return false
        val lower = text.lowercase()
        val hasTransaction = transactionKeywords.any { lower.contains(it) }
        val rejected = rejectedKeywords.any { lower.contains(it) }

        return hasTransaction && !rejected
    }
}
