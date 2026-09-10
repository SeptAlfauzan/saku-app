package com.septaalfauzan.saku.data.database

import com.septaalfauzan.saku.domain.model.KeywordType
import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.ParserKeyword

object NotificationSourceSeed {
    const val KEY_TRACKING_ENABLED = "notification_tracking_enabled"
    const val KEY_AUTO_CONFIRM = "auto_confirm"

    val sources: List<NotificationSource> = listOf(
        NotificationSource(packageName = "com.bca", providerId = "bca", enabled = true),
        NotificationSource(packageName = "com.gojek.app", providerId = "gopay", enabled = true),
        NotificationSource(packageName = "com.ovo.id", providerId = "ovo", enabled = true),
        NotificationSource(packageName = "id.dana", providerId = "dana", enabled = true),
    )

    val defaultKeywords: Map<String, List<ParserKeyword>> = mapOf(
        "com.bca" to listOf(
            ParserKeyword("com.bca", KeywordType.EXPENSE, "pembayaran"),
            ParserKeyword("com.bca", KeywordType.EXPENSE, "pembelian"),
            ParserKeyword("com.bca", KeywordType.EXPENSE, "debit"),
            ParserKeyword("com.bca", KeywordType.EXPENSE, "transaksi kartu"),
            ParserKeyword("com.bca", KeywordType.EXPENSE, "pengeluaran"),
            ParserKeyword("com.bca", KeywordType.INCOME, "transfer masuk"),
            ParserKeyword("com.bca", KeywordType.INCOME, "dana masuk"),
            ParserKeyword("com.bca", KeywordType.INCOME, "diterima"),
            ParserKeyword("com.bca", KeywordType.INCOME, "pemasukan"),
            ParserKeyword("com.bca", KeywordType.MERCHANT, "berhasil di "),
            ParserKeyword("com.bca", KeywordType.MERCHANT, "di "),
            ParserKeyword("com.bca", KeywordType.MERCHANT, "ke "),
            ParserKeyword("com.bca", KeywordType.MERCHANT, "dari "),
            ParserKeyword("com.bca", KeywordType.MERCHANT, "merchant "),
        ),
        "com.gojek.app" to listOf(
            ParserKeyword("com.gojek.app", KeywordType.EXPENSE, "pembayaran"),
            ParserKeyword("com.gojek.app", KeywordType.EXPENSE, "pembelian"),
            ParserKeyword("com.gojek.app", KeywordType.EXPENSE, "pengeluaran"),
            ParserKeyword("com.gojek.app", KeywordType.INCOME, "menerima"),
            ParserKeyword("com.gojek.app", KeywordType.INCOME, "memperoleh"),
            ParserKeyword("com.gojek.app", KeywordType.INCOME, "pengembalian dana"),
            ParserKeyword("com.gojek.app", KeywordType.INCOME, "refund"),
            ParserKeyword("com.gojek.app", KeywordType.MERCHANT, "di "),
            ParserKeyword("com.gojek.app", KeywordType.MERCHANT, "dari "),
            ParserKeyword("com.gojek.app", KeywordType.MERCHANT, "ke "),
            ParserKeyword("com.gojek.app", KeywordType.MERCHANT, "merchant "),
        ),
        "com.ovo.id" to listOf(
            ParserKeyword("com.ovo.id", KeywordType.EXPENSE, "pembayaran"),
            ParserKeyword("com.ovo.id", KeywordType.EXPENSE, "pembelian"),
            ParserKeyword("com.ovo.id", KeywordType.EXPENSE, "transfer keluar"),
            ParserKeyword("com.ovo.id", KeywordType.EXPENSE, "pengeluaran"),
            ParserKeyword("com.ovo.id", KeywordType.INCOME, "dompet terisi"),
            ParserKeyword("com.ovo.id", KeywordType.INCOME, "transfer masuk"),
            ParserKeyword("com.ovo.id", KeywordType.INCOME, "diterima"),
            ParserKeyword("com.ovo.id", KeywordType.INCOME, "cashback"),
            ParserKeyword("com.ovo.id", KeywordType.MERCHANT, "di "),
            ParserKeyword("com.ovo.id", KeywordType.MERCHANT, "dari "),
            ParserKeyword("com.ovo.id", KeywordType.MERCHANT, "ke "),
        ),
        "id.dana" to listOf(
            ParserKeyword("id.dana", KeywordType.EXPENSE, "pembayaran"),
            ParserKeyword("id.dana", KeywordType.EXPENSE, "pembelian"),
            ParserKeyword("id.dana", KeywordType.EXPENSE, "transfer keluar"),
            ParserKeyword("id.dana", KeywordType.EXPENSE, "pengeluaran"),
            ParserKeyword("id.dana", KeywordType.INCOME, "dana masuk"),
            ParserKeyword("id.dana", KeywordType.INCOME, "transfer masuk"),
            ParserKeyword("id.dana", KeywordType.INCOME, "diterima"),
            ParserKeyword("id.dana", KeywordType.INCOME, "refund"),
            ParserKeyword("id.dana", KeywordType.MERCHANT, "ke "),
            ParserKeyword("id.dana", KeywordType.MERCHANT, "di "),
            ParserKeyword("id.dana", KeywordType.MERCHANT, "dari "),
            ParserKeyword("id.dana", KeywordType.MERCHANT, "merchant "),
        ),
    )

    val genericDefaults = listOf(
        ParserKeyword("", KeywordType.EXPENSE, "pembayaran"),
        ParserKeyword("", KeywordType.EXPENSE, "pembelian"),
        ParserKeyword("", KeywordType.EXPENSE, "debit"),
        ParserKeyword("", KeywordType.EXPENSE, "transaksi kartu"),
        ParserKeyword("", KeywordType.EXPENSE, "pengeluaran"),
        ParserKeyword("", KeywordType.INCOME, "transfer masuk"),
        ParserKeyword("", KeywordType.INCOME, "dana masuk"),
        ParserKeyword("", KeywordType.INCOME, "diterima"),
        ParserKeyword("", KeywordType.INCOME, "pemasukan"),
        ParserKeyword("", KeywordType.MERCHANT, "berhasil di "),
        ParserKeyword("", KeywordType.MERCHANT, "di "),
        ParserKeyword("", KeywordType.MERCHANT, "ke "),
        ParserKeyword("", KeywordType.MERCHANT, "dari "),
        ParserKeyword("", KeywordType.MERCHANT, "merchant "),
    )
}