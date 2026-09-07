package com.septaalfauzan.saku.notification.model

import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.time.Instant

data class ParsedTransaction(
    val type: TransactionType? = null,
    val amount: Long? = null,
    val currency: String? = null,
    val rawMerchant: String? = null,
    val merchant: String? = null,
    val categoryId: String? = null,
    val occurredAt: Instant? = null,
    val description: String? = null,
    val confidence: Double = 0.0,
)
