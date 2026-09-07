package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class AddTransaction(private val repository: TransactionRepository) {

    operator fun invoke(
        type: com.septaalfauzan.saku.domain.model.TransactionType,
        amount: Long,
        currency: String = "IDR",
        merchant: String?,
        categoryId: String?,
        description: String?,
        occurredAt: Instant,
    ): Transaction {
        val now = Clock.System.now()
        val tx = Transaction(
            id = Uuid.random().toString(),
            type = type,
            amount = amount,
            currency = currency,
            merchant = merchant,
            categoryId = categoryId,
            description = description,
            source = TransactionSource.MANUAL,
            sourcePackage = null,
            occurredAt = occurredAt,
            createdAt = now,
            updatedAt = now,
        )
        return tx
    }

    suspend fun store(tx: Transaction) {
        repository.insert(tx)
    }
}