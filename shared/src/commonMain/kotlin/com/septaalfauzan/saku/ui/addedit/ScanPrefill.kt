package com.septaalfauzan.saku.ui.addedit

import com.septaalfauzan.saku.domain.model.AddEditFormState
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.Receipt
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.model.toItemsNote
import com.septaalfauzan.saku.util.parseReceiptDate
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ScanPrefill(
    val amount: Long,
    val merchant: String,
    val note: String,
    val occurredAtMillis: Long,
    val type: TransactionType,
    val categoryId: String?,
    val categories: List<Category>
) {
    companion object {
        fun fromReceipt(
            receipt: Receipt,
            fallbackMillis: Long = Clock.System.now().toEpochMilliseconds(),
        ): ScanPrefill = ScanPrefill(
            amount = receipt.total,
            merchant = receipt.merchantName,
            note = receipt.toItemsNote(),
            type = receipt.transactionType,
            categoryId = receipt.categoryId,
            categories = receipt.categories,
            occurredAtMillis = parseReceiptDate(receipt.transactionDate)?.toEpochMilliseconds()
                ?: fallbackMillis,
        )

        fun decode(json: String): ScanPrefill? =
            runCatching { Json.decodeFromString(serializer(), json) }.getOrNull()
    }
}

fun AddEditFormState.withPrefill(prefill: ScanPrefill): AddEditFormState =
    copy(
        amountInput = prefill.amount.toString(),
        categoryId = prefill.categoryId,
        merchant = prefill.merchant,
        note = prefill.note,
        type = prefill.type,
        occurredAtMillis = prefill.occurredAtMillis,
    )
