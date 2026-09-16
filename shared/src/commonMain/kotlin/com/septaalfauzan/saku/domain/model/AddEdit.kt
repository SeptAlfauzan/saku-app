package com.septaalfauzan.saku.domain.model

import kotlin.time.Clock
import kotlinx.serialization.Serializable

data class AddEditFormState(
    val editingId: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val categoryId: String? = null,
    val merchant: String = "",
    val note: String = "",
    val occurredAtMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val amountError: String? = null,
    val categoryError: String? = null,
)

@Serializable
data class AddEditUiState(
    val editingId: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val categoryId: String? = null,
    val merchant: String = "",
    val note: String = "",
    val occurredAtMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val categories: List<Category> = emptyList(),
    val amountError: String? = null,
    val categoryError: String? = null,
    val canSave: Boolean = false,
)