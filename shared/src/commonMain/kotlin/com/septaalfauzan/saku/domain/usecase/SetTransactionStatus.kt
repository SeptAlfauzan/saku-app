package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.repository.TransactionRepository

class SetTransactionStatus(private val repository: TransactionRepository) {
    suspend operator fun invoke(id: String, status: TransactionStatus) = repository.setStatus(id, status)
}
