package com.septaalfauzan.saku.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReceiptHelpersTest {

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
        items = listOf(
            ReceiptItem("Nasi Goreng", 2, 10000, 20000),
            ReceiptItem("Ayam Bakar", 1, 10000, 10000),
        ),
    )

    @Test
    fun itemsBecomePricedNoteWithQuantityPrefix() {
        assertEquals("2x Nasi Goreng (Rp. 20.000), 1x Ayam Bakar (Rp. 10.000)", receipt.toItemsNote())
    }

    @Test
    fun emptyItemsProduceEmptyNote() {
        val empty = receipt.copy(items = emptyList())
        assertEquals("", empty.toItemsNote())
    }

    @Test
    fun picksOtherExpenseWhenPresent() {
        val categories = listOf(
            Category("food", "Food", "🍜", TransactionType.EXPENSE),
            Category("other_expense", "Other", "📦", TransactionType.EXPENSE),
        )
        assertEquals("other_expense", categories.defaultExpenseId())
    }

    @Test
    fun fallsBackToFirstExpenseCategory() {
        val categories = listOf(
            Category("food", "Food", "🍜", TransactionType.EXPENSE),
            Category("salary", "Salary", "💰", TransactionType.INCOME),
        )
        assertEquals("food", categories.defaultExpenseId())
    }

    @Test
    fun returnsNullWithoutExpenseCategory() {
        val categories = listOf(
            Category("salary", "Salary", "💰", TransactionType.INCOME),
        )
        assertNull(categories.defaultExpenseId())
    }

    @Test
    fun returnsNullForEmptyCategories() {
        assertNull(emptyList<Category>().defaultExpenseId())
    }
}
