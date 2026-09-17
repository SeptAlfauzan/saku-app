package com.septaalfauzan.saku.testutil

import com.septaalfauzan.saku.domain.importexport.ApplyResult
import com.septaalfauzan.saku.domain.importexport.UndoSnapshot
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.DuplicateKey
import com.septaalfauzan.saku.domain.model.Receipt
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.repository.OcrRepository
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeOcrRepository : OcrRepository {
    var result: Receipt? = null
    var error: Exception? = null

    override suspend fun getOcrReceiptValue(base64Image: String, imageType: String): Receipt {
        error?.let { throw it }
        return result ?: error("no OCR result configured")
    }
}

class FakeTransactionRepository(
    var categories: List<Category> = emptyList(),
) : TransactionRepository {
    val inserted = mutableListOf<Transaction>()

    override fun observeTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
    override fun observeCategories(): Flow<List<Category>> = flowOf(categories)
    override suspend fun insert(transaction: Transaction) { inserted += transaction }
    override suspend fun update(transaction: Transaction) {}
    override suspend fun delete(id: String) {}
    override fun observePending(): Flow<List<Transaction>> = flowOf(emptyList())
    override suspend fun setStatus(id: String, status: TransactionStatus) {}
    override suspend fun findRecentDuplicate(
        key: DuplicateKey,
        withinStartMillis: Long,
        withinEndMillis: Long,
    ): Transaction? = null
    override suspend fun getAll(): List<Transaction> = inserted
    override suspend fun applyImport(changes: List<Transaction>): ApplyResult =
        ApplyResult(0, 0, UndoSnapshot(emptyList(), emptyMap()))
    override suspend fun undoImport(snapshot: UndoSnapshot) {}
}
