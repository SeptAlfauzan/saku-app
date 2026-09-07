package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class ObserveCategories(private val repository: TransactionRepository) {
    operator fun invoke(): Flow<List<Category>> = repository.observeCategories()
}
