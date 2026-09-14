package com.septaalfauzan.saku.data.importexport.csv

import com.septaalfauzan.saku.domain.importexport.CsvRowDraft
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.math.roundToInt

object TransactionCsvMapper {
    val headers: List<String> = listOf(
        "Transaction ID", "Type", "Amount", "Currency", "Merchant", "Category", "Notes",
        "Entry Method", "Detected From", "Status", "Date & Time", "Created At", "Last Updated", "Confidence",
    )

    /** Column index per normalized (trimmed, lowercase) header name. */
    fun headerIndexMap(headerRow: List<String>): Map<String, Int> =
        headerRow.mapIndexed { index, raw -> raw.trim().lowercase() to index }.toMap()

    fun fromRow(lineNumber: Int, values: List<String>, index: Map<String, Int>): CsvRowDraft {
        fun cell(key: String): String? =
            index[key]?.let { values.getOrNull(it)?.trim()?.ifBlank { null } }
        return CsvRowDraft(
            lineNumber = lineNumber,
            id = cell(HEADER_ID), type = cell(HEADER_TYPE), amount = cell(HEADER_AMOUNT),
            currency = cell(HEADER_CURRENCY), merchant = cell(HEADER_MERCHANT),
            category = cell(HEADER_CATEGORY), notes = cell(HEADER_NOTES),
            entryMethod = cell(HEADER_ENTRY_METHOD), detectedFrom = cell(HEADER_DETECTED_FROM),
            status = cell(HEADER_STATUS), occurredAt = cell(HEADER_OCCURRED_AT),
            createdAt = cell(HEADER_CREATED_AT), confidence = cell(HEADER_CONFIDENCE),
        )
    }

    fun toRow(
        tx: Transaction,
        categoryName: String?,
        sourceDisplay: String?,
    ): List<String?> = listOf(
        tx.id,
        typeToDisplay(tx.type),
        tx.amount.toString(),
        tx.currency,
        tx.merchant,
        categoryName?.takeIf { it.isNotBlank() },
        tx.description,
        sourceToDisplay(tx.source),
        if (tx.source == TransactionSource.MANUAL) null else sourceDisplay,
        statusToDisplay(tx.status),
        formatCsvDateTime(tx.occurredAt.toEpochMilliseconds()),
        formatCsvDateTime(tx.createdAt.toEpochMilliseconds()),
        formatCsvDateTime(tx.updatedAt.toEpochMilliseconds()),
        if (tx.source == TransactionSource.MANUAL) null else confidenceToDisplay(tx.confidence),
    )

    fun typeToDisplay(type: TransactionType): String = when (type) {
        TransactionType.INCOME -> "Income"
        TransactionType.EXPENSE -> "Expense"
        TransactionType.TRANSFER -> "Transfer"
    }

    fun typeFromDisplay(value: String?): TransactionType? = when (value?.trim()?.lowercase()) {
        "income" -> TransactionType.INCOME
        "expense" -> TransactionType.EXPENSE
        else -> null
    }

    fun statusToDisplay(status: TransactionStatus): String = when (status) {
        TransactionStatus.CONFIRMED -> "Confirmed"
        TransactionStatus.PENDING_REVIEW -> "Pending"
        TransactionStatus.IGNORED -> "Ignored"
    }

    fun statusFromDisplay(value: String?): TransactionStatus? = when (value?.trim()?.lowercase()) {
        "confirmed" -> TransactionStatus.CONFIRMED
        "pending" -> TransactionStatus.PENDING_REVIEW
        "ignored" -> TransactionStatus.IGNORED
        else -> null
    }

    fun sourceToDisplay(source: TransactionSource): String = when (source) {
        TransactionSource.MANUAL -> "Manual"
        TransactionSource.NOTIFICATION, TransactionSource.SCAN -> "Auto-detected"
    }

    fun sourceFromDisplay(value: String?): TransactionSource? = when (value?.trim()?.lowercase()) {
        "manual" -> TransactionSource.MANUAL
        "auto-detected" -> TransactionSource.NOTIFICATION
        else -> null
    }

    fun confidenceToDisplay(confidence: Double): String =
        (confidence * 100).roundToInt().toString() + "%"

    private const val HEADER_ID = "transaction id"
    private const val HEADER_TYPE = "type"
    private const val HEADER_AMOUNT = "amount"
    private const val HEADER_CURRENCY = "currency"
    private const val HEADER_MERCHANT = "merchant"
    private const val HEADER_CATEGORY = "category"
    private const val HEADER_NOTES = "notes"
    private const val HEADER_ENTRY_METHOD = "entry method"
    private const val HEADER_DETECTED_FROM = "detected from"
    private const val HEADER_STATUS = "status"
    private const val HEADER_OCCURRED_AT = "date & time"
    private const val HEADER_CREATED_AT = "created at"
    private const val HEADER_CONFIDENCE = "confidence"
}
