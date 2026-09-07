package com.septaalfauzan.saku.data.database

import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.TransactionType

object CategorySeed {
    val categories: List<Category> = listOf(
        Category("food", "Food", "food", TransactionType.EXPENSE),
        Category("transport", "Transport", "transport", TransactionType.EXPENSE),
        Category("shopping", "Shopping", "shopping", TransactionType.EXPENSE),
        Category("bills", "Bills", "bills", TransactionType.EXPENSE),
        Category("entertainment", "Entertainment", "entertainment", TransactionType.EXPENSE),
        Category("health", "Health", "health", TransactionType.EXPENSE),
        Category("education", "Education", "education", TransactionType.EXPENSE),
        Category("insurance", "Insurance", "insurance", TransactionType.EXPENSE),
        Category("other_expense", "Other", "other", TransactionType.EXPENSE),
        Category("salary", "Salary", "salary", TransactionType.INCOME),
        Category("freelance", "Freelance", "freelance", TransactionType.INCOME),
        Category("cashback", "Cashback", "cashback", TransactionType.INCOME),
        Category("interest", "Interest", "interest", TransactionType.INCOME),
        Category("other_income", "Other", "other", TransactionType.INCOME),
    )
}
