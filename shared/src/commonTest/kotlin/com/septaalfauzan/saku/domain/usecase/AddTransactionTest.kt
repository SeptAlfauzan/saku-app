package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.importexport.ApplyResult
import com.septaalfauzan.saku.domain.importexport.UndoSnapshot
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.DuplicateKey
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FakeTransactionRepository : TransactionRepository {
    override fun observeTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
    override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())
    override suspend fun insert(transaction: Transaction) = Unit
    override suspend fun update(transaction: Transaction) = Unit
    override suspend fun delete(id: String) = Unit
    override fun observePending(): Flow<List<Transaction>> = flowOf(emptyList())
    override suspend fun setStatus(id: String, status: TransactionStatus) = Unit
    override suspend fun findRecentDuplicate(
        key: DuplicateKey,
        withinStartMillis: Long,
        withinEndMillis: Long,
    ): Transaction? = null
    override suspend fun getAll(): List<Transaction> = emptyList()
    override suspend fun applyImport(changes: List<Transaction>): ApplyResult =
        ApplyResult(0, 0, UndoSnapshot(emptyList(), emptyMap()))
    override suspend fun undoImport(snapshot: UndoSnapshot) = Unit
}

class AddTransactionTest {

    private fun useCase() = AddTransaction(FakeTransactionRepository())

    @Test
    fun defaultSourceIsManual() {
        val tx = useCase().invoke(
            type = TransactionType.EXPENSE,
            amount = 120_000,
            merchant = "Alfamart",
            categoryId = null,
            description = null,
            occurredAt = Clock.System.now(),
        )
        assertEquals(TransactionSource.MANUAL, tx.source)
        assertNull(tx.sourcePackage)
    }

    @Test
    fun passesThroughCallerSourceAndPackage() {
        val tx = useCase().invoke(
            type = TransactionType.EXPENSE,
            amount = 120_000,
            merchant = "Alfamart",
            categoryId = null,
            description = null,
            occurredAt = Clock.System.now(),
            source = TransactionSource.SCAN,
            sourcePackage = "receipt_scan",
        )
        assertEquals(TransactionSource.SCAN, tx.source)
        assertEquals("receipt_scan", tx.sourcePackage)
    }

    @Test
    fun scanSourceLeavesSourcePackageNull() {
        val tx = useCase().invoke(
            type = TransactionType.EXPENSE,
            amount = 120_000,
            merchant = "Alfamart",
            categoryId = null,
            description = null,
            occurredAt = Clock.System.now(),
            source = TransactionSource.SCAN,
        )
        assertEquals(TransactionSource.SCAN, tx.source)
        assertNull(tx.sourcePackage)
    }
}