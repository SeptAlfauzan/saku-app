package com.septaalfauzan.saku.data.importexport.csv

import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TransactionCsvMapperTest {

    private val now = Instant.fromEpochMilliseconds(1_770_000_000_000)

    private fun tx(
        id: String = "id-1",
        type: TransactionType = TransactionType.EXPENSE,
        amount: Long = 90000,
        categoryId: String? = "transport",
        source: TransactionSource = TransactionSource.NOTIFICATION,
        confidence: Double = 0.87,
    ) = Transaction(
        id = id, type = type, amount = amount, currency = "IDR",
        merchant = "Warung Bu Sari, Jakarta", categoryId = categoryId,
        description = "Grab to office", source = source,
        sourcePackage = "com.gojek.app", status = TransactionStatus.CONFIRMED,
        confidence = confidence,
        occurredAt = now, createdAt = now, updatedAt = now,
    )

    @Test
    fun headerOrderMatchesSpec() {
        assertEquals(
            listOf(
                "Transaction ID", "Type", "Amount", "Currency", "Merchant", "Category", "Notes",
                "Entry Method", "Detected From", "Status", "Date & Time", "Created At", "Last Updated", "Confidence",
            ),
            TransactionCsvMapper.headers,
        )
    }

    @Test
    fun mapsTransactionToReadableRow() {
        val row = TransactionCsvMapper.toRow(
            tx(),
            categoryName = "Transport",
            sourceDisplay = "gopay",
        )
        assertEquals("id-1", row[0])
        assertEquals("Expense", row[1])
        assertEquals("90000", row[2])
        assertEquals("IDR", row[3])
        assertEquals("Warung Bu Sari, Jakarta", row[4])
        assertEquals("Transport", row[5])
        assertEquals("Grab to office", row[6])
        assertEquals("Auto-detected", row[7])
        assertEquals("gopay", row[8])
        assertEquals("Confirmed", row[9])
        assertEquals(formatCsvDateTime(now.toEpochMilliseconds()), row[10])
        assertEquals("87%", row[13])
    }

    @Test
    fun manualTransactionsShowManualAndBlankConfidence() {
        val row = TransactionCsvMapper.toRow(
            tx(source = TransactionSource.MANUAL, categoryId = null, confidence = 0.0),
            categoryName = null,
            sourceDisplay = null,
        )
        assertEquals("Manual", row[7])
        assertNull(row[8])
        assertNull(row[5])
        assertNull(row[13])
    }

    @Test
    fun parsesRowIntoDraftByHeaderNameIgnoringOrder() {
        val values = listOf("90000", "2026-01-05 14:00:30", "Income", "IDR")
        val index = TransactionCsvMapper.headerIndexMap(
            listOf("Amount", "Date & Time", "Type", "Currency"),
        )
        val draft = TransactionCsvMapper.fromRow(lineNumber = 2, values = values, index = index)
        assertEquals(2, draft.lineNumber)
        assertEquals("90000", draft.amount)
        assertEquals("Income", draft.type)
        assertEquals("IDR", draft.currency)
        assertEquals("2026-01-05 14:00:30", draft.occurredAt)
        assertNull(draft.merchant)
    }

    @Test
    fun displayLegendsRoundTrip() {
        assertEquals(TransactionType.INCOME, TransactionCsvMapper.typeFromDisplay("income"))
        assertEquals(TransactionType.EXPENSE, TransactionCsvMapper.typeFromDisplay("expense"))
        assertNull(TransactionCsvMapper.typeFromDisplay("transfer"))
        assertEquals(TransactionStatus.PENDING_REVIEW, TransactionCsvMapper.statusFromDisplay("pending"))
        assertEquals(TransactionSource.MANUAL, TransactionCsvMapper.sourceFromDisplay("manual"))
    }
}
