package com.septaalfauzan.saku.notification.provider

import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.notification.model.ParsedTransaction
import com.septaalfauzan.saku.notification.normalizer.AmountNormalizer
import com.septaalfauzan.saku.notification.normalizer.CurrencyNormalizer

abstract class ProviderParser(
    protected val packageName: String,
    private val expenseWords: List<String>,
    private val incomeWords: List<String>,
    private val merchantWords: List<String>,
) : NotificationParser {

    override fun canParse(data: NotificationData): Boolean = data.packageName == packageName

    override fun parse(data: NotificationData): ParsedTransaction? {
        if (data.packageName != packageName) return null
        val text = buildString {
            data.title?.let { append(it).append("\n") }
            data.body?.let { append(it) }
        }.trim().ifBlank { return null }

        val lower = text.lowercase()
        val type = when {
            incomeWords.any { lower.contains(it) } -> TransactionType.INCOME
            expenseWords.any { lower.contains(it) } -> TransactionType.EXPENSE
            else -> return null
        }
        val amount = AmountNormalizer.firstToken(text)?.let { AmountNormalizer.normalize(it) } ?: return null

        return ParsedTransaction(
            type = type,
            amount = amount,
            currency = CurrencyNormalizer.normalize(text) ?: "IDR",
            rawMerchant = merchantFrom(text),
            occurredAt = null,
            description = null,
            confidence = 0.0,
        )
    }

    private fun merchantFrom(text: String): String? {
        val lower = text.lowercase()
        for (word in merchantWords) {
            val idx = lower.lastIndexOf(word)
            if (idx < 0) continue
            val rest = text.substring(idx + word.length).trim()
            val cleaned = rest.split(Regex("""[\n.!?:,]+""")).first().trim()
            if (cleaned.isNotEmpty()) return cleaned
        }
        return null
    }
}
