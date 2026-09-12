package com.septaalfauzan.saku.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val type: TransactionType,
)

fun List<Category>.defaultExpenseId(): String? =
    firstOrNull { it.id == "other_expense" }?.id
        ?: firstOrNull { it.type == TransactionType.EXPENSE }?.id
