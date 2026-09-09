package com.septaalfauzan.saku.notification.category

object CategoryResolver {
    private val map = mapOf(
        "Tokopedia" to "shopping",
        "Shopee" to "shopping",
        "McDonald's" to "food",
        "GoFood" to "food",
        "Grab" to "transport",
        "Gojek" to "transport",
        "Netflix" to "entertainment",
        "Spotify" to "entertainment",
        "PLN" to "bills",
    )

    fun resolve(canonicalMerchant: String?): String? = canonicalMerchant?.let { map[it] }
}
