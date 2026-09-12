package com.septaalfauzan.saku.notification

import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.notification.provider.ConfigurableParser
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConfigurableParserTest {

    private fun notification(packageName: String, body: String?) = NotificationData(
        packageName = packageName,
        title = null,
        body = body,
        postedAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
        notificationId = 1,
    )

    private val bcaParser = ConfigurableParser(
        packageName = "com.bca",
        expenseWords = listOf("pembayaran", "pembelian", "debit", "transaksi kartu", "pengeluaran"),
        incomeWords = listOf("transfer masuk", "dana masuk", "diterima", "pemasukan"),
        merchantWords = listOf("berhasil di ", "di ", "ke ", "dari ", "merchant "),
    )

    @Test
    fun parsesExpenseWithCorrectTypeAndAmount() {
        val parsed = bcaParser.parse(notification("com.bca", "Pembayaran Rp150.000 berhasil di TOKOPEDIA"))
        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(150_000L, parsed.amount)
        assertEquals("TOKOPEDIA", parsed.rawMerchant)
    }

    @Test
    fun parsesIncomeWithCorrectType() {
        val parsed = bcaParser.parse(notification("com.bca", "Transfer masuk Rp2.000.000 dari SEPTA"))
        assertNotNull(parsed)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(2_000_000L, parsed.amount)
    }

    @Test
    fun returnsNullForWrongPackage() {
        assertNull(bcaParser.parse(notification("com.ovo.id", "Pembayaran Rp50.000 di WARUNG")))
    }

    @Test
    fun returnsNullForUnrecognizedKeyword() {
        assertNull(bcaParser.parse(notification("com.bca", "Thanks for using BCA")))
    }

    @Test
    fun canParseMatchesPackageName() {
        assertEquals(true, bcaParser.canParse(notification("com.bca", "test")))
        assertEquals(false, bcaParser.canParse(notification("com.ovo.id", "test")))
    }
}
