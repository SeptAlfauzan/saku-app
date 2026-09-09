package com.septaalfauzan.saku.notification.merchant

object MerchantResolver {
    private val aliases = mapOf(
        "TOKOPEDIA" to "Tokopedia",
        "TOKOPEDIA.COM" to "Tokopedia",
        "PT TOKOPEDIA" to "Tokopedia",
        "MCDONALDS" to "McDonald's",
        "MCDONALD" to "McDonald's",
        "MCD" to "McDonald's",
        "SHOPEE" to "Shopee",
        "GRAB" to "Grab",
        "GOJEK" to "Gojek",
        "GOFOOD" to "GoFood",
        "NETFLIX" to "Netflix",
        "SPOTIFY" to "Spotify",
        "PLN" to "PLN",
    )

    fun resolve(raw: String?): String? {
        val normalized = raw?.trim()?.take(60)?.uppercase() ?: return null
        aliases[normalized]?.let { return it }
        for ((key, canonical) in aliases) {
            if (normalized.startsWith(key)) return canonical
        }
        return null
    }
}
