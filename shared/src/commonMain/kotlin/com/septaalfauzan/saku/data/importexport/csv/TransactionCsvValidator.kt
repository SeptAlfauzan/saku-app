package com.septaalfauzan.saku.data.importexport.csv

import com.septaalfauzan.saku.domain.importexport.CsvError
import com.septaalfauzan.saku.domain.importexport.CsvRowDraft
import com.septaalfauzan.saku.domain.importexport.TransactionDraft
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.math.roundToInt
import kotlin.time.Instant

object TransactionCsvValidator {

    data class Result(val draft: TransactionDraft?, val errors: List<CsvError>)

    fun buildDraft(
        draft: CsvRowDraft,
        categories: List<Category>,
        sources: List<NotificationSource>,
        now: Instant,
    ): Result {
        val errors = mutableListOf<CsvError>()
        val row = draft.lineNumber
        fun error(field: String, reason: String) { errors += CsvError(row, field, reason) }

        val type = TransactionCsvMapper.typeFromDisplay(draft.type)
        if (type == null) {
            if (draft.type.isNullOrBlank()) error("Type", "Required")
            else error("Type", "Must be Income or Expense")
        }

        val amount = draft.amount?.trim()?.let { raw ->
            if (raw.matches(Regex("""\d+"""))) raw.toLong()
            else { error("Amount", "Must be a non-negative whole number"); null }
        } ?: if (draft.amount.isNullOrBlank()) { error("Amount", "Required"); null } else null

        val currencyRaw = draft.currency?.trim().orEmpty()
        val currency = if (currencyRaw.equals("IDR", ignoreCase = true)) "IDR"
        else if (currencyRaw.isBlank()) "IDR"
        else { error("Currency", "Currency must be IDR"); "IDR" }

        val categoryId = draft.category?.let { name ->
            categories.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
                ?: run { error("Category", "No category named \"$name\""); null }
        }

        val source = draft.entryMethod?.let { TransactionCsvMapper.sourceFromDisplay(it) }
            ?: TransactionSource.MANUAL
        if (draft.entryMethod != null && source == TransactionSource.MANUAL &&
            !draft.entryMethod.equals("manual", ignoreCase = true)
        ) {
            error("Entry Method", "Unknown entry method")
        }

        val sourcePackage = draft.detectedFrom?.let { raw ->
            sources.firstOrNull {
                raw.equals(it.providerId, ignoreCase = true) || raw.equals(it.packageName, ignoreCase = true)
            }?.packageName ?: raw
        }

        val classifiedStatus = draft.status?.let { TransactionCsvMapper.statusFromDisplay(it) }
        if (draft.status != null && classifiedStatus == null) error("Status", "Unknown status")
        val status = classifiedStatus ?: TransactionStatus.CONFIRMED

        val occurredAt = if (draft.occurredAt.isNullOrBlank()) {
            error("Date & Time", "Required"); null
        } else {
            parseCsvDateTime(draft.occurredAt) ?: run {
                error("Date & Time", "Unrecognized date \"${draft.occurredAt}\""); null
            }
        }

        val createdAt = draft.createdAt?.let {
            parseCsvDateTime(it) ?: run { error("Created At", "Unrecognized date \"$it\""); null }
        }

        val confidence = if (draft.confidence.isNullOrBlank()) 0.0 else {
            val trimmed = draft.confidence.trim().removeSuffix("%")
            val value = trimmed.toDoubleOrNull()
            if (value != null && value in 0.0..100.0) value / 100.0
            else { error("Confidence", "Unrecognized confidence \"${draft.confidence}\""); 0.0 }
        }

        if (errors.isNotEmpty()) return Result(null, errors)

        val built = TransactionDraft(
            lineNumber = row,
            id = draft.id,
            type = type!!,
            amount = amount!!,
            currency = currency,
            merchant = draft.merchant?.ifBlank { null },
            categoryId = categoryId,
            notes = draft.notes?.ifBlank { null },
            source = source,
            sourcePackage = sourcePackage,
            status = status,
            occurredAt = occurredAt!!,
            createdAt = createdAt,
            confidence = confidence,
        )
        return Result(built, emptyList())
    }
}
