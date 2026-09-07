package com.septaalfauzan.saku.domain.repository

import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeCategories(): Flow<List<Category>>
    suspend fun insert(transaction: Transaction)
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: String)
}