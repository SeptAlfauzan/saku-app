package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.repository.TransactionRepository

class DeleteTransaction(private val repository: TransactionRepository) {
    suspend operator fun invoke(id: String) {
        repository.delete(id)
    }
}
