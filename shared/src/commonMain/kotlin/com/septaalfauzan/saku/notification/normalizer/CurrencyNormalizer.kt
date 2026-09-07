package com.septaalfauzan.saku.notification.normalizer

object CurrencyNormalizer {
    fun normalize(text: String): String? {
        val lower = text.lowercase()
        return when {
            lower.contains("idr") || lower.contains("rp") -> "IDR"
            else -> null
        }
    }
}
