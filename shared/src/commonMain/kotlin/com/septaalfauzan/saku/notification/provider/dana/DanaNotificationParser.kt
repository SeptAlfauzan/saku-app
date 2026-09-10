package com.septaalfauzan.saku.notification.provider.dana

import com.septaalfauzan.saku.notification.provider.ProviderParser

class DanaNotificationParser : ProviderParser(
    packageName = "id.dana",
    expenseWords = listOf("pembayaran", "pembelian", "transfer keluar", "pengeluaran"),
    incomeWords = listOf("dana masuk", "transfer masuk", "diterima", "refund"),
    merchantWords = listOf("ke ", "di ", "dari ", "merchant "),
)