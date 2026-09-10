package com.septaalfauzan.saku.notification.provider.testapp

import com.septaalfauzan.saku.notification.provider.ProviderParser

class TestAppParser : ProviderParser(
    packageName = "com.ipkgroup.hris.dev",
    expenseWords = listOf("pembayaran", "pembelian", "debit", "transaksi kartu", "pengeluaran"),
    incomeWords = listOf("transfer masuk", "dana masuk", "diterima", "pemasukan"),
    merchantWords = listOf("berhasil di ", "di ", "ke ", "dari ", "merchant "),
)