package com.septaalfauzan.saku.ui.addedit

import com.septaalfauzan.saku.domain.model.TransactionType

object InputValidation {

    fun parseAmountToLong(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        if (!trimmed.all { it.isDigit() }) return null
        return trimmed.toLongOrNull()
    }

    fun validate(type: TransactionType, amount: Long?, categoryId: String?): List<String> =
        buildList {
            if (amount == null || amount <= 0L) add("Amount must be greater than zero")
            if (type == TransactionType.EXPENSE && categoryId == null) add("Category is required")
        }
}
