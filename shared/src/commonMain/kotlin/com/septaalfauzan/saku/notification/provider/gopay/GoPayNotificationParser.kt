package com.septaalfauzan.saku.notification.provider.gopay

import com.septaalfauzan.saku.notification.provider.ProviderParser

class GoPayNotificationParser : ProviderParser(
    packageName = "com.gojek.app",
    expenseWords = listOf("pembayaran", "pembelian"),
    incomeWords = listOf("menerima", "memperoleh", "pengembalian dana", "refund"),
    merchantWords = listOf("di ", "dari ", "ke ", "merchant "),
)