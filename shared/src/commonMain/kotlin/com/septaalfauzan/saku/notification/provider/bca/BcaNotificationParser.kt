package com.septaalfauzan.saku.notification.provider.bca

import com.septaalfauzan.saku.notification.provider.ProviderParser

class BcaNotificationParser : ProviderParser(
    packageName = "com.bca",
    expenseWords = listOf("pembayaran", "pembelian", "debit", "transaksi kartu"),
    incomeWords = listOf("transfer masuk", "dana masuk", "diterima", "pemasukan"),
    merchantWords = listOf("berhasil di ", "di ", "ke ", "dari ", "merchant "),
)
