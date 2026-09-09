package com.septaalfauzan.saku.notification

import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.notification.generic.GenericNotificationParser
import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.notification.provider.ParserRegistry
import com.septaalfauzan.saku.notification.provider.bca.BcaNotificationParser
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

    private val bcaParser = BcaNotificationParser()
    private val generic = GenericNotificationParser()

    @Test
    fun bcaExpenseExtractsTypeAmountCurrencyAndMerchant() {
        val parsed = bcaParser.parse(notification("com.bca", "Pembayaran Rp150.000 berhasil di TOKOPEDIA"))
        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(150_000L, parsed.amount)
        assertEquals("IDR", parsed.currency)
        assertEquals("TOKOPEDIA", parsed.rawMerchant)
    }

    @Test
    fun bcaIncomeExtractsTypeAmountAndCounterpart() {
        val parsed = bcaParser.parse(notification("com.bca", "Transfer masuk Rp2.000.000 dari SEPTA ALFAUZAN"))
        assertNotNull(parsed)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(2_000_000L, parsed.amount)
        assertEquals("SEPTA ALFAUZAN", parsed.rawMerchant)
    }

    @Test
    fun bcaCardTransactionExtractsMerchant() {
        val parsed = bcaParser.parse(notification("com.bca", "Transaksi kartu berhasil sebesar IDR 125.500 merchant MCDONALDS JAKARTA"))
        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(125_500L, parsed.amount)
        assertEquals("MCDONALDS JAKARTA", parsed.rawMerchant)
    }

    @Test
    fun bcaDoesNotParseOtherPackages() {
        assertNull(bcaParser.parse(notification("com.ovo.id", "Pembayaran Rp50.000 di WARUNG")))
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
        val registry = ParserRegistry(listOf(bcaParser), generic)
        val parsed = registry.parse(notification("com.bca", "Pembayaran Rp150.000 berhasil di TOKOPEDIA"))
        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(150_000L, parsed.amount)
    }

    @Test
    fun registryFallsBackToGenericForUnknownProvider() {
        val registry = ParserRegistry(listOf(bcaParser), generic)
        val parsed = registry.parse(notification("com.somebank", "Pembayaran Rp75.000 berhasil"))
        assertNotNull(parsed)
        assertEquals(75_000L, parsed.amount)
    }
}
