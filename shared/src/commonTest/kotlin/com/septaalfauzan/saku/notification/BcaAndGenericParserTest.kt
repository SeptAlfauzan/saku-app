package com.septaalfauzan.saku.notification

import com.septaalfauzan.saku.domain.model.KeywordType
import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.ParserKeyword
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.notification.generic.GenericNotificationParser
import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.notification.provider.ParserRegistry
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BcaAndGenericParserTest {

    private fun notification(packageName: String, body: String?) = NotificationData(
        packageName = packageName,
        title = null,
        body = body,
        postedAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
        notificationId = 1,
    )

    private val generic = GenericNotificationParser()
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
    private val bcaRegistry = ParserRegistry().also { it.rebuild(listOf(bcaSource), mapOf("com.bca" to bcaKeywords)) }

    @Test
    fun bcaExpenseExtractsTypeAmountCurrencyAndMerchant() {
        val parsed = bcaRegistry.parse(notification("com.bca", "Pembayaran Rp150.000 berhasil di TOKOPEDIA"))
        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(150_000L, parsed.amount)
        assertEquals("TOKOPEDIA", parsed.rawMerchant)
    }

    @Test
    fun bcaIncomeExtractsTypeAmountAndCounterpart() {
        val parsed = bcaRegistry.parse(notification("com.bca", "Transfer masuk Rp2.000.000 dari SEPTA ALFAUZAN"))
        assertNotNull(parsed)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(2_000_000L, parsed.amount)
        assertEquals("SEPTA ALFAUZAN", parsed.rawMerchant)
    }

    @Test
    fun bcaCardTransactionExtractsMerchant() {
        val parsed = bcaRegistry.parse(notification("com.bca", "Transaksi kartu berhasil sebesar IDR 125.500 merchant MCDONALDS JAKARTA"))
        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(125_500L, parsed.amount)
        assertEquals("MCDONALDS JAKARTA", parsed.rawMerchant)
    }

    @Test
    fun bcaDoesNotParseOtherPackages() {
        assertNull(bcaRegistry.parse(notification("com.ovo.id", "Pembayaran Rp50.000 di WARUNG")))
    }

    @Test
    fun genericParserExtractsUnknownProviderMoneyNotification() {
        val parsed = generic.parse(notification("com.somebank", "Your card was charged IDR 125,500 at MCDONALDS JAKARTA"))
        assertNotNull(parsed)
        assertEquals(125_500L, parsed.amount)
    }

    @Test
    fun genericParserReturnsNullWithoutAmount() {
        assertNull(generic.parse(notification("com.somebank", "Thanks for using our app")))
    }

    @Test
    fun registryPrefersProviderParserOverGeneric() {
        val bcaSource = NotificationSource("com.bca", "bca", enabled = true)
        val bcaKeywords = listOf(
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
        val registry = ParserRegistry()
        registry.rebuild(listOf(bcaSource), mapOf("com.bca" to bcaKeywords))
        val parsed = registry.parse(notification("com.bca", "Pembayaran Rp150.000 berhasil di TOKOPEDIA"))
        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(150_000L, parsed.amount)
    }

    @Test
    fun registryFallsBackToGenericForUnknownProvider() {
        val registry = ParserRegistry()
        registry.rebuild(emptyList(), emptyMap())
        val parsed = registry.parse(notification("com.somebank", "Pembayaran Rp75.000 berhasil"))
        assertNotNull(parsed)
        assertEquals(75_000L, parsed.amount)
    }
}
