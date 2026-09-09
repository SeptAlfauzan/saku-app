package com.septaalfauzan.saku.notification.generic

import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.notification.model.ParsedTransaction
import com.septaalfauzan.saku.notification.normalizer.AmountNormalizer
import com.septaalfauzan.saku.notification.normalizer.CurrencyNormalizer
import com.septaalfauzan.saku.notification.normalizer.TypeDetector
import com.septaalfauzan.saku.notification.provider.NotificationParser

class GenericNotificationParser : NotificationParser {
    override fun canParse(data: NotificationData): Boolean = true

    override fun parse(data: NotificationData): ParsedTransaction? {
        val text = buildString {
            data.title?.let { append(it).append("\n") }
            data.body?.let { append(it) }
        }.trim().ifBlank { return null }

        val type = TypeDetector.detect(text) ?: return null
        val amount = AmountNormalizer.firstToken(text)?.let { AmountNormalizer.normalize(it) } ?: return null

        return ParsedTransaction(
            type = type,
            amount = amount,
            currency = CurrencyNormalizer.normalize(text),
            rawMerchant = null,
            occurredAt = null,
            description = null,
            confidence = 0.0,
        )
    }
}
