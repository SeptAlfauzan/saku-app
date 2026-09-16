package com.septaalfauzan.saku.ui.addedit

import com.septaalfauzan.saku.domain.model.AddEditFormState
import com.septaalfauzan.saku.domain.model.Receipt
import com.septaalfauzan.saku.domain.model.ReceiptItem
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScanPrefillTest {

    private val receipt = Receipt(
        merchantName = "Toko Maju",
        transactionDate = "2026-09-06",
        transactionTime = "12:00",
        currency = "IDR",
        subtotal = 30000,
        tax = 0,
        discount = 0,
        total = 30000,
        paymentMethod = "QRIS",
        items = listOf(ReceiptItem("Nasi Goreng", 2, 10000, 20000)),
    )

    @Test
    fun fromReceiptMapsKnownFields() {
        val prefill = ScanPrefill.fromReceipt(receipt, fallbackMillis = 123L)
        assertEquals(30000L, prefill.amount)
        assertEquals("Toko Maju", prefill.merchant)
        assertEquals("2x Nasi Goreng (Rp. 20.000)", prefill.note)
        val expectedDate = LocalDate(2026, 9, 6)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        assertEquals(expectedDate, prefill.occurredAtMillis)
    }

    @Test
    fun fromReceiptFallsBackWhenDateUnparseable() {
        val bad = receipt.copy(transactionDate = "garbage")
        val prefill = ScanPrefill.fromReceipt(bad, fallbackMillis = 999L)
        assertEquals(999L, prefill.occurredAtMillis)
    }

    @Test
    fun jsonRoundTripSurvives() {
        val prefill = ScanPrefill.fromReceipt(receipt, fallbackMillis = 0L)
        val json = Json.encodeToString(prefill)
        assertEquals(prefill, ScanPrefill.decode(json))
    }

    @Test
    fun decodeRejectsInvalidJson() {
        assertNull(ScanPrefill.decode("{not json"))
    }

    @Test
    fun withPrefillSeedsFormFields() {
        val prefill = ScanPrefill(
            amount = 25000,
            merchant = "M",
            note = "N",
            occurredAtMillis = 777L,
            type = TransactionType.EXPENSE,
            categoryId = null,
            categories = emptyList(),
        )
        val seeded = AddEditFormState().withPrefill(prefill)
        assertEquals("25000", seeded.amountInput)
        assertEquals("M", seeded.merchant)
        assertEquals("N", seeded.note)
        assertEquals(777L, seeded.occurredAtMillis)
    }

    @Test
    fun withPrefillKeepsTypeAndClearsCategory() {
        val prefill = ScanPrefill(
            amount = 1,
            merchant = "",
            note = "",
            occurredAtMillis = 0L,
            type = TransactionType.EXPENSE,
            categoryId = null,
            categories = emptyList(),
        )
        val seeded = AddEditFormState(categoryId = "food").withPrefill(prefill)
        assertEquals(com.septaalfauzan.saku.domain.model.TransactionType.EXPENSE, seeded.type)
        assertEquals(null, seeded.categoryId)
    }
}
