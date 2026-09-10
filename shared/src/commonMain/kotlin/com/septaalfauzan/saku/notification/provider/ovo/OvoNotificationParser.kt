package com.septaalfauzan.saku.notification.provider.ovo

import com.septaalfauzan.saku.notification.provider.ProviderParser

class OvoNotificationParser : ProviderParser(
    packageName = "com.ovo.id",
    expenseWords = listOf("pembayaran", "pembelian", "transfer keluar", "pengeluaran"),
    incomeWords = listOf("dompet terisi", "transfer masuk", "diterima", "cashback"),
    merchantWords = listOf("di ", "dari ", "ke ", "merchant "),
)