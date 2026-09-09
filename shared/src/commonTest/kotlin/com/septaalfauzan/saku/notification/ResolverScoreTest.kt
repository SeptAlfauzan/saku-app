package com.septaalfauzan.saku.notification

import com.septaalfauzan.saku.notification.category.CategoryResolver
import com.septaalfauzan.saku.notification.confidence.ConfidenceEngine
import com.septaalfauzan.saku.notification.merchant.MerchantResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResolverScoreTest {

    @Test
    fun merchantAliasesResolveToCanonical() {
        assertEquals("Tokopedia", MerchantResolver.resolve("TOKOPEDIA"))
        assertEquals("Tokopedia", MerchantResolver.resolve("TOKOPEDIA.COM"))
        assertEquals("McDonald's", MerchantResolver.resolve("MCDONALDS JAKARTA"))
        assertEquals("McDonald's", MerchantResolver.resolve("MCD"))
        assertNull(MerchantResolver.resolve("SEPTA ALFAUZAN"))
        assertNull(MerchantResolver.resolve(null))
    }

    @Test
    fun categoriesResolveFromCanonicalMerchant() {
        assertEquals("shopping", CategoryResolver.resolve("Tokopedia"))
        assertEquals("food", CategoryResolver.resolve("McDonald's"))
        assertEquals("transport", CategoryResolver.resolve("Grab"))
        assertEquals("entertainment", CategoryResolver.resolve("Netflix"))
        assertEquals("bills", CategoryResolver.resolve("PLN"))
        assertNull(CategoryResolver.resolve(null))
        assertNull(CategoryResolver.resolve("Unknown Vendor"))
    }

    @Test
    fun confidenceScoring() {
        val full = ConfidenceEngine.score(hasAmount = true, hasType = true, hasCanonicalMerchant = true, hasCategory = true)
        assertEquals(1.00, full, 0.001)
        val noMerchantNoCategory = ConfidenceEngine.score(hasAmount = true, hasType = true, hasCanonicalMerchant = false, hasCategory = false)
        assertEquals(0.70, noMerchantNoCategory, 0.001)
        val merchantOnlyMissingCategory = ConfidenceEngine.score(hasAmount = true, hasType = true, hasCanonicalMerchant = true, hasCategory = false)
        assertEquals(0.90, merchantOnlyMissingCategory, 0.001)
    }
}
