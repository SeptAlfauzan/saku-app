package com.septaalfauzan.saku.notification

import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.notification.provider.ParserRegistry
import com.septaalfauzan.saku.notification.provider.bca.BcaNotificationParser
import com.septaalfauzan.saku.notification.provider.dana.DanaNotificationParser
import com.septaalfauzan.saku.notification.provider.gopay.GoPayNotificationParser
import com.septaalfauzan.saku.notification.provider.ovo.OvoNotificationParser
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

    private val registry = ParserRegistry(
        listOf(BcaNotificationParser(), GoPayNotificationParser(), OvoNotificationParser(), DanaNotificationParser()),
    )

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