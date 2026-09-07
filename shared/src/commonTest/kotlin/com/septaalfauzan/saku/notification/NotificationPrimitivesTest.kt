package com.septaalfauzan.saku.notification

import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.notification.detector.TransactionDetector
import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.notification.normalizer.AmountNormalizer
import com.septaalfauzan.saku.notification.normalizer.CurrencyNormalizer
import com.septaalfauzan.saku.notification.normalizer.TypeDetector
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationPrimitivesTest {

    private fun notification(body: String) = NotificationData(
        packageName = "com.bca",
        title = "BCA Mobile",
        body = body,
        postedAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
        notificationId = 1,
    )

    @Test
    fun amountNormalizationFormats() {
        assertEquals(150_000L, AmountNormalizer.normalize("Rp150.000"))
        assertEquals(150_000L, AmountNormalizer.normalize("Rp 150.000"))
        assertEquals(150_000L, AmountNormalizer.normalize("150.000"))
        assertEquals(150_000L, AmountNormalizer.normalize("IDR 150,000"))
        assertEquals(150_000L, AmountNormalizer.normalize("IDR 150.000"))
        assertEquals(150_000L, AmountNormalizer.normalize("150000"))
        assertNull(AmountNormalizer.normalize("Rp????"))
        assertNull(AmountNormalizer.normalize(""))
    }

    @Test
    fun amountTokenFindsFirstAmount() {
        assertEquals("Rp150.000", AmountNormalizer.firstToken("Pembayaran Rp150.000 berhasil di TOKOPEDIA"))
        assertNull(AmountNormalizer.firstToken("Tidak ada nominal"))
    }

    @Test
    fun currencyDetection() {
        assertEquals("IDR", CurrencyNormalizer.normalize("Pembayaran sebesar Rp150.000"))
        assertEquals("IDR", CurrencyNormalizer.normalize("Charged IDR 125,500"))
        assertNull(CurrencyNormalizer.normalize("No currency mentioned"))
    }

    @Test
    fun typeDetection() {
        assertEquals(TransactionType.EXPENSE, TypeDetector.detect("Pembayaran berhasil"))
        assertEquals(TransactionType.EXPENSE, TypeDetector.detect("Pembelian berhasil"))
        assertEquals(TransactionType.EXPENSE, TypeDetector.detect("Debit kartu"))
        assertEquals(TransactionType.INCOME, TypeDetector.detect("Transfer masuk Rp2.000.000"))
        assertEquals(TransactionType.INCOME, TypeDetector.detect("Dana masuk"))
        assertNull(TypeDetector.detect("Info promo terbaru"))
    }

    @Test
    fun transformerNotificationsAreDetected() {
        assertTrue(TransactionDetector.isTransaction(notification("Pembayaran Rp150.000 berhasil di TOKOPEDIA")))
        assertTrue(TransactionDetector.isTransaction(notification("Transfer masuk Rp2.000.000 dari SEPTA ALFAUZAN")))
    }

    @Test
    fun nonTransactionNotificationsAreRejected() {
        assertFalse(TransactionDetector.isTransaction(notification("Nikmati promo cashback 50%")))
        assertFalse(TransactionDetector.isTransaction(notification("Laporan bulanan kartu kredit tersedia")))
        assertFalse(TransactionDetector.isTransaction(notification("Pembayaran Rp150.000 gagal")))
        assertFalse(TransactionDetector.isTransaction(notification("Fitur baru sudah tersedia di aplikasi")))
    }
}
