package com.septaalfauzan.saku.notification.engine

import com.septaalfauzan.saku.notification.category.CategoryResolver
import com.septaalfauzan.saku.notification.confidence.ConfidenceEngine
import com.septaalfauzan.saku.notification.detector.TransactionDetector
import com.septaalfauzan.saku.notification.merchant.MerchantResolver
import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.notification.model.ParsedTransaction
import com.septaalfauzan.saku.notification.provider.ParserRegistry

class NotificationParserEngine(
    private val parserRegistry: ParserRegistry,
) {
    fun process(data: NotificationData): ParsedTransaction? {
        if (!TransactionDetector.isTransaction(data)) return null

        val parsed = parserRegistry.parse(data) ?: return null

        val canonicalMerchant = parsed.rawMerchant?.let { MerchantResolver.resolve(it) }
        val merchant = canonicalMerchant ?: parsed.rawMerchant?.trim()?.takeIf { it.isNotEmpty() }
        val categoryId = canonicalMerchant?.let { CategoryResolver.resolve(it) }

        // AI fallback (deferred): when confidence is below threshold AND deterministic
        // parsing left fields unresolved, a future AiTransactionParser may be invoked
        // here as the final fallback before dedup/persistence. Not built in this plan.
        val confidence = ConfidenceEngine.score(
            hasAmount = parsed.amount != null,
            hasType = parsed.type != null,
            hasCanonicalMerchant = canonicalMerchant != null,
            hasCategory = categoryId != null,
        )

        return parsed.copy(
            merchant = merchant,
            categoryId = categoryId,
            occurredAt = parsed.occurredAt ?: data.postedAt,
            confidence = confidence,
        )
    }
}
