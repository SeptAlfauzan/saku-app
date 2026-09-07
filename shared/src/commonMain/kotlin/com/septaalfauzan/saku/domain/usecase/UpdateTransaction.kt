package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlin.time.Clock

class UpdateTransaction(private val repository: TransactionRepository) {

    suspend fun store(updated: Transaction) {
        repository.update(updated.copy(updatedAt = Clock.System.now()))
    }
}