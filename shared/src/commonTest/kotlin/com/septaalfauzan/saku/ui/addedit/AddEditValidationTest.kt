package com.septaalfauzan.saku.ui.addedit

import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddEditValidationTest {

    @Test
    fun parseAmountRejectsNonNumeric() {
        assertNull(InputValidation.parseAmountToLong(""))
        assertNull(InputValidation.parseAmountToLong("abc"))
        assertNull(InputValidation.parseAmountToLong("12.500"))
    }

    @Test
    fun parseAmountAcceptsPlainInteger() {
        assertEquals(12500L, InputValidation.parseAmountToLong("12500"))
        assertEquals(0L, InputValidation.parseAmountToLong("0"))
    }

    @Test
    fun expenseRequiresPositiveAmountAndCategory() {
        val errors = InputValidation.validate(
            type = TransactionType.EXPENSE,
            amount = 0L,
            categoryId = null,
        )
        assertTrue(errors.contains("Amount must be greater than zero"))
        assertTrue(errors.contains("Category is required"))
    }

    @Test
    fun incomeDoesNotRequireCategory() {
        val errors = InputValidation.validate(
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
            InputValidation.validate(TransactionType.EXPENSE, 35000L, "food"),
        )
    }
}
