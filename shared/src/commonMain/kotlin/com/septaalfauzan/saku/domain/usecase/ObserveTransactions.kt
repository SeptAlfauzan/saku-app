package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class ObserveTransactions(private val repository: TransactionRepository) {
    operator fun invoke(): Flow<List<Transaction>> = repository.observeTransactions()
}