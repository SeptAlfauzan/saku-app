package com.septaalfauzan.saku.domain.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Long,
    val currency: String,
    val merchant: String?,
    val categoryId: String?,
    val description: String?,
    val source: TransactionSource,
    val sourcePackage: String?,
    @Serializable(with = InstantMillisSerializer::class)
    val occurredAt: Instant,
    @Serializable(with = InstantMillisSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantMillisSerializer::class)
    val updatedAt: Instant,
) {
    val isIncome: Boolean get() = type == TransactionType.INCOME
    val isExpense: Boolean get() = type == TransactionType.EXPENSE
}
