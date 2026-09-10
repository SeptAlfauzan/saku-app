package com.septaalfauzan.saku.notification

import com.septaalfauzan.saku.domain.model.KeywordType
import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.ParserKeyword
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.notification.provider.ParserRegistry
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProviderCorpusTest {

    private fun notification(packageName: String, body: String?) = NotificationData(
        packageName = packageName,
        title = null,
        body = body,
        postedAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
        notificationId = 1,
    )

    private val bcaSource = NotificationSource("com.bca", "bca", enabled = true)
    private val bcaKeywords = listOf(
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
    )

    private val gopaySource = NotificationSource("com.gojek.app", "gopay", enabled = true)
    private val gopayKeywords = listOf(
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
    )

    private val ovoSource = NotificationSource("com.ovo.id", "ovo", enabled = true)
    private val ovoKeywords = listOf(
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
        ParserKeyword("com.ovo.id", KeywordType.MERCHANT, "merchant "),
    )

    private val danaSource = NotificationSource("id.dana", "dana", enabled = true)
    private val danaKeywords = listOf(
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
    )

    private val registry = ParserRegistry().also { reg ->
        reg.rebuild(
            listOf(bcaSource, gopaySource, ovoSource, danaSource),
            mapOf(
                "com.bca" to bcaKeywords,
                "com.gojek.app" to gopayKeywords,
                "com.ovo.id" to ovoKeywords,
                "id.dana" to danaKeywords,
            ),
        )
    }

    @Test
    fun gopayExpense() {
        val parsed = registry.parse(notification("com.gojek.app", "Pembayaran Rp35.000 di MCDONALDS berhasil"))
        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(35_000L, parsed.amount)
    }

    @Test
    fun gopayIncome() {
        val parsed = registry.parse(notification("com.gojek.app", "Kamu telah menerima Rp100.000 dari ANDI"))
        assertNotNull(parsed)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(100_000L, parsed.amount)
    }

    @Test
    fun ovoExpense() {
        val parsed = registry.parse(notification("com.ovo.id", "Pembayaran Rp50.000 di WARUNG BERKAH berhasil"))
        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(50_000L, parsed.amount)
    }

    @Test
    fun ovoIncome() {
        val parsed = registry.parse(notification("com.ovo.id", "Dompet terisi sebesar Rp200.000 dari TOPUP ATM"))
        assertNotNull(parsed)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(200_000L, parsed.amount)
    }

    @Test
    fun danaExpense() {
        val parsed = registry.parse(notification("id.dana", "Pembayaran berhasil Rp50.000 ke GRAB"))
        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(50_000L, parsed.amount)
        assertEquals("GRAB", parsed.rawMerchant)
    }

    @Test
    fun danaIncome() {
        val parsed = registry.parse(notification("id.dana", "Dana masuk Rp300.000 dari +6281234567890"))
        assertNotNull(parsed)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(300_000L, parsed.amount)
    }
}
