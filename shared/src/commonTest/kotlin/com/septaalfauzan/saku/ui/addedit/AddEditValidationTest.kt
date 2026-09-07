package com.septaalfauzan.saku.ui.addedit

import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddEditValidationTest {

    @Test
    fun parseAmountRejectsNonNumeric() {
        assertNull(AddEditValidation.parseAmount(""))
        assertNull(AddEditValidation.parseAmount("abc"))
        assertNull(AddEditValidation.parseAmount("12.500"))
    }

    @Test
    fun parseAmountAcceptsPlainInteger() {
        assertEquals(12500L, AddEditValidation.parseAmount("12500"))
        assertEquals(0L, AddEditValidation.parseAmount("0"))
    }

    @Test
    fun expenseRequiresPositiveAmountAndCategory() {
        val errors = AddEditValidation.validate(
            type = TransactionType.EXPENSE,
            amount = 0L,
            categoryId = null,
        )
        assertTrue(errors.contains("Amount must be greater than zero"))
        assertTrue(errors.contains("Category is required"))
    }

    @Test
    fun incomeDoesNotRequireCategory() {
        val errors = AddEditValidation.validate(
            type = TransactionType.INCOME,
            amount = null,
            categoryId = null,
        )
        assertTrue(errors.contains("Amount must be greater than zero"))
        assertTrue(errors.none { it == "Category is required" })
    }

    @Test
    fun validExpenseHasNoErrors() {
        assertEquals(
            emptyList(),
            AddEditValidation.validate(TransactionType.EXPENSE, 35000L, "food"),
        )
    }
}
