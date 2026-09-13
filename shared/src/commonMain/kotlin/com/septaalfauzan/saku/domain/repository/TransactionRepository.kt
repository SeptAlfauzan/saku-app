package com.septaalfauzan.saku.domain.repository

import com.septaalfauzan.saku.domain.importexport.ApplyResult
import com.septaalfauzan.saku.domain.importexport.UndoSnapshot
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.DuplicateKey
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeCategories(): Flow<List<Category>>
    suspend fun insert(transaction: Transaction)
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: String)
    fun observePending(): Flow<List<Transaction>>
    suspend fun setStatus(id: String, status: TransactionStatus)
    suspend fun findRecentDuplicate(
        key: DuplicateKey,
        withinStartMillis: Long,
        withinEndMillis: Long,
    ): Transaction?
    suspend fun getAll(): List<Transaction>
    suspend fun applyImport(changes: List<Transaction>): ApplyResult
    suspend fun undoImport(snapshot: UndoSnapshot)
}
