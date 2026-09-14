package com.septaalfauzan.saku.domain.importexport

import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.time.Instant

data class CsvError(val row: Int, val field: String, val reason: String)

data class CsvRowDraft(
    val lineNumber: Int,
    val id: String? = null,
    val type: String? = null,
    val amount: String? = null,
    val currency: String? = null,
    val merchant: String? = null,
    val category: String? = null,
    val notes: String? = null,
    val entryMethod: String? = null,
    val detectedFrom: String? = null,
    val status: String? = null,
    val occurredAt: String? = null,
    val createdAt: String? = null,
    val confidence: String? = null,
) {
    fun isEmpty(): Boolean =
        listOf(id, type, amount, currency, merchant, category, notes, entryMethod,
            detectedFrom, status, occurredAt, createdAt, confidence).all { it.isNullOrBlank() }
}

data class TransactionDraft(
    val lineNumber: Int,
    val id: String?,
    val type: TransactionType,
    val amount: Long,
    val currency: String,
    val merchant: String?,
    val categoryId: String?,
    val notes: String?,
    val source: TransactionSource,
    val sourcePackage: String?,
    val status: TransactionStatus,
    val occurredAt: Instant,
    val createdAt: Instant?,
    val confidence: Double,
) {
    fun toTransaction(now: Instant): Transaction = Transaction(
        id = id ?: kotlin.uuid.Uuid.random().toString(),
        type = type,
        amount = amount,
        currency = currency,
        merchant = merchant?.ifBlank { null },
        categoryId = categoryId,
        description = notes?.ifBlank { null },
        source = source,
        sourcePackage = sourcePackage?.ifBlank { null },
        status = status,
        confidence = confidence,
        occurredAt = occurredAt,
        createdAt = createdAt ?: now,
        updatedAt = now,
    )
}

data class ExportFilter(
    val dateStart: Instant? = null,
    val dateEnd: Instant? = null,
    val type: TransactionType? = null,
    val categoryId: String? = null,
)

data class ParsedRows(val drafts: List<TransactionDraft>, val errors: List<CsvError>)

sealed interface ImportAction {
    data class CreateAction(val draft: TransactionDraft) : ImportAction
    data class UpdateAction(val draft: TransactionDraft) : ImportAction
    data class DuplicateAction(val draft: TransactionDraft) : ImportAction
}

data class Classification(val actions: List<ImportAction>) {
    val newCount: Int get() = actions.count { it is ImportAction.CreateAction }
    val updateCount: Int get() = actions.count { it is ImportAction.UpdateAction }
    val duplicateCount: Int get() = actions.count { it is ImportAction.DuplicateAction }
}

data class UndoSnapshot(val newIds: List<String>, val previousById: Map<String, Transaction>)

data class ApplyResult(val newCount: Int, val updateCount: Int, val snapshot: UndoSnapshot)
