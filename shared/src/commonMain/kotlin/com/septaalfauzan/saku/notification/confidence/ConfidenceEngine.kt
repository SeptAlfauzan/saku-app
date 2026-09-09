package com.septaalfauzan.saku.notification.confidence

object ConfidenceEngine {
    const val AUTO_CONFIRM_THRESHOLD = 0.80

    fun score(
        hasAmount: Boolean,
        hasType: Boolean,
        hasCanonicalMerchant: Boolean,
        hasCategory: Boolean,
    ): Double {
        var total = 0.30 // transaction pattern matched
        if (hasAmount) total += 0.30
        if (hasType) total += 0.10
        if (hasCanonicalMerchant) total += 0.20
        if (hasCategory) total += 0.10
        return total
    }
}
