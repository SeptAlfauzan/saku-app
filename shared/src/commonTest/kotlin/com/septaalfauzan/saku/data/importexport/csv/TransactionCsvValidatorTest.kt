package com.septaalfauzan.saku.data.importexport.csv

import com.septaalfauzan.saku.data.database.CategorySeed
import com.septaalfauzan.saku.domain.importexport.CsvRowDraft
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransactionCsvValidatorTest {

    private val categories: List<Category> = CategorySeed.categories
    private val sources: List<NotificationSource> = listOf(
        NotificationSource("com.gojek.app", "gopay", true),
        NotificationSource("com.bca", "bca", true),
    )
    private val now = Instant.fromEpochMilliseconds(1_770_000_000_000)

    private fun validDraft() = CsvRowDraft(
        lineNumber = 3,
        type = "Expense",
        amount = "90000",
        currency = "IDR",
        merchant = "Warung",
        category = "Transport",
        notes = "Grab",
        entryMethod = "Auto-detected",
        detectedFrom = "gopay",
        status = "Confirmed",
        occurredAt = "2026-01-05 14:00:30",
        confidence = "87%",
    )

    private fun result(draft: CsvRowDraft) =
        TransactionCsvValidator.buildDraft(draft, categories, sources, now)

    @Test
    fun buildsDraftFromValidRow() {
        val r = result(validDraft())
        assertTrue(r.errors.isEmpty(), r.errors.toString())
        val d = r.draft!!
        assertEquals(TransactionType.EXPENSE, d.type)
        assertEquals(90000L, d.amount)
        assertEquals("IDR", d.currency)
        assertEquals("transport", d.categoryId)
        assertEquals("Grab", d.notes)
        assertEquals(TransactionSource.NOTIFICATION, d.source)
        assertEquals("com.gojek.app", d.sourcePackage)
        assertEquals(TransactionStatus.CONFIRMED, d.status)
        assertEquals(0.87, d.confidence)
    }

    @Test
    fun defaultsMissingOptionalFields() {
        val r = result(CsvRowDraft(
            lineNumber = 4,
            type = "income",
            amount = "1000",
            occurredAt = "2026-01-05",
        ))
        assertTrue(r.errors.isEmpty(), r.errors.toString())
        val d = r.draft!!
        assertEquals(TransactionType.INCOME, d.type)
        assertEquals("IDR", d.currency)
        assertEquals(TransactionSource.MANUAL, d.source)
        assertEquals(TransactionStatus.CONFIRMED, d.status)
        assertEquals(0.0, d.confidence)
        assertNull(d.merchant)
        assertNull(d.categoryId)
    }

    @Test
    fun rejectsTransferType() {
        val r = result(validDraft().copy(type = "Transfer"))
        assertEquals("Must be Income or Expense", r.errors.first { it.field == "Type" }.reason)
        assertNull(r.draft)
    }

    @Test
    fun rejectsBadAmountAndCurrency() {
        val r = result(validDraft().copy(amount = "10,000", currency = "USD"))
        assertEquals("Must be a non-negative whole number", r.errors.first { it.field == "Amount" }.reason)
        assertEquals("Currency must be IDR", r.errors.first { it.field == "Currency" }.reason)
        assertNull(r.draft)
    }

    @Test
    fun rejectsUnknownCategoryAndMissingDate() {
        val r = result(validDraft().copy(category = "Grceries", occurredAt = null))
        assertTrue(r.errors.any { it.field == "Category" && it.reason == "No category named \"Grceries\"" })
        assertTrue(r.errors.any { it.field == "Date & Time" })
        assertNull(r.draft)
    }

    @Test
    fun resolvesSourcePackageByProviderIdOrPackageNameAndStoresRawOtherwise() {
        assertEquals("com.gojek.app", result(validDraft().copy(detectedFrom = "gopay")).draft?.sourcePackage)
        assertEquals("com.gojek.app", result(validDraft().copy(detectedFrom = "com.gojek.app")).draft?.sourcePackage)
        assertEquals("MyWallet", result(validDraft().copy(detectedFrom = "MyWallet")).draft?.sourcePackage)
    }

    @Test
    fun returnsAllErrorsForTheRow() {
        val r = result(validDraft().copy(type = "Transfer", amount = "abc", currency = "EUR",
            status = "Maybe", confidence = "120%", createdAt = "junk"))
        val fields = r.errors.map { it.field }.toSet()
        assertEquals(setOf("Type", "Amount", "Currency", "Status", "Confidence", "Created At"), fields)
        assertTrue(r.errors.all { it.row == 3 })
    }
}
