package com.septaalfauzan.saku.ui.addedit

import com.septaalfauzan.saku.domain.model.Receipt
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
) {
    companion object {
        fun fromReceipt(
            receipt: Receipt,
            fallbackMillis: Long = Clock.System.now().toEpochMilliseconds(),
        ): ScanPrefill = ScanPrefill(
            amount = receipt.total,
            merchant = receipt.merchantName,
            note = receipt.toItemsNote(),
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
        categoryId = null,
        merchant = prefill.merchant,
        note = prefill.note,
        occurredAtMillis = prefill.occurredAtMillis,
    )
