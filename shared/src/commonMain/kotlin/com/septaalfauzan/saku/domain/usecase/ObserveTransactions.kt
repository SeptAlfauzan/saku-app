package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class ObserveTransactions(private val repository: TransactionRepository) {
    operator fun invoke(
        startDateMils: Long? = null,
        endDateMils: Long? = null
    ): Flow<List<Transaction>> =
        if (startDateMils != null && endDateMils != null) repository.observeTransactions(
            startDateMils,
            endDateMils
        ) else repository.observeTransactions()
}
