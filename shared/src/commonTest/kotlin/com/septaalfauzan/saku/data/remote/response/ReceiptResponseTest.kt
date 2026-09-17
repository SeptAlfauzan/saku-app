package com.septaalfauzan.saku.data.remote.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ReceiptResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun wire(
        paymentMethod: String? = null,
        merchantName: String? = null,
        currency: String? = null,
    ) = """{"data":{
        "merchant_name":${merchantName?.let { "\"$it\"" } ?: "null"},
        "transaction_date":null,
        "transaction_time":null,
        "currency":${currency?.let { "\"$it\"" } ?: "null"},
        "subtotal":null,
        "tax":null,
        "discount":null,
        "total":25000,
        "payment_method":${paymentMethod?.let { "\"$it\"" } ?: "null"},
        "items":[]
    }}"""

    @Test
    fun nullPaymentMethodMapsToDash() {
        val receipt = json.decodeFromString<ReceiptResponse>(wire()).data.toDomain()
        assertEquals("-", receipt.paymentMethod)
    }

    @Test
    fun presentPaymentMethodIsPreserved() {
        val receipt = json.decodeFromString<ReceiptResponse>(wire(paymentMethod = "GoPay")).data.toDomain()
        assertEquals("GoPay", receipt.paymentMethod)
    }

    @Test
    fun nullableFieldsFallBackToDefaults() {
        val receipt = json.decodeFromString<ReceiptResponse>(wire()).data.toDomain()
        assertEquals("", receipt.merchantName)
        assertEquals("", receipt.transactionDate)
        assertEquals("", receipt.transactionTime)
        assertEquals("IDR", receipt.currency)
        assertEquals(0L, receipt.tax)
        assertEquals(0L, receipt.discount)
        assertEquals(null, receipt.subtotal)
    }

    @Test
    fun roundTripPreservesValues() {
        val data = ReceiptData(
            merchantName = "Alfamart",
            transactionDate = "16-09-2026",
            transactionTime = "10:00",
            currency = "IDR",
            subtotal = 10000,
            tax = 0,
            discount = 500,
            total = 9500,
            paymentMethod = "-",
            items = listOf(ReceiptItem("Nasi", 1, 10000, 10000)),
        )
        val decoded = json.decodeFromString<ReceiptData>(json.encodeToString(ReceiptData.serializer(), data))
        assertEquals(data, decoded)
    }
}
