package com.septaalfauzan.saku.domain.model

import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun sampleTransaction() = Transaction(
        id = "tx-1",
        type = TransactionType.EXPENSE,
        amount = 54990,
        currency = "IDR",
        merchant = "Spotify",
        categoryId = "entertainment",
        description = "Monthly",
        source = TransactionSource.MANUAL,
        sourcePackage = null,
        status = TransactionStatus.CONFIRMED,
        confidence = 0.0,
        occurredAt = Instant.fromEpochMilliseconds(1_000_000_000_000),
        createdAt = Instant.fromEpochMilliseconds(1_000_000_000_100),
        updatedAt = Instant.fromEpochMilliseconds(1_000_000_000_100),
    )

    @Test
    fun transactionRoundTripsWithEpochMillisInstants() {
        val wire = json.encodeToString(Transaction.serializer(), sampleTransaction())
        val decoded = json.decodeFromString(Transaction.serializer(), wire)
        assertEquals(sampleTransaction(), decoded)
    }

    @Test
    fun categoryRoundTrips() {
        val category = Category("food", "Food", "food", TransactionType.EXPENSE)
        val decoded = json.decodeFromString(Category.serializer(), json.encodeToString(Category.serializer(), category))
        assertEquals(category, decoded)
    }

    @Test
    fun transactionSourceEqualsManual() {
        val tx = sampleTransaction()
        assertEquals(TransactionSource.MANUAL, tx.source)
    }

    @Test
    fun instantSerializerStoresEpochMillis() {
        val wire = json.encodeToString(InstantMillisSerializer, sampleTransaction().occurredAt)
        assertEquals("1000000000000", wire)
    }

    @Test
    fun notificationTransactionSerializesWithStatusAndConfidence() {
        val tx = sampleTransaction().copy(
            source = TransactionSource.NOTIFICATION,
            status = TransactionStatus.PENDING_REVIEW,
            confidence = 0.70,
        )
        val decoded = json.decodeFromString(Transaction.serializer(), json.encodeToString(Transaction.serializer(), tx))
        assertEquals(tx, decoded)
    }
}
