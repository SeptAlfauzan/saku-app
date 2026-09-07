package com.septaalfauzan.saku.data.repository

import com.septaalfauzan.saku.data.dao.CategoryDao
import com.septaalfauzan.saku.data.dao.TransactionDao
import com.septaalfauzan.saku.data.database.CategorySeed
import com.septaalfauzan.saku.data.entity.toDomain
import com.septaalfauzan.saku.data.entity.toEntity
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomTransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
) : TransactionRepository {

    private val seedMutex = Mutex()
    private var seeded = false
    private val categoryOrder: Map<String, Int> =
        CategorySeed.categories.mapIndexed { index, category -> category.id to index }.toMap()

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeCategories(): Flow<List<Category>> =
        flow {
            ensureSeeded()
            emitAll(categoryDao.observeAll())
        }.map { list ->
            list.map { it.toDomain() }.sortedBy { categoryOrder[it.id] ?: Int.MAX_VALUE }
        }

    private suspend fun ensureSeeded() {
        if (seeded) return
        seedMutex.withLock {
            if (seeded) return
            if (categoryDao.count() == 0L) {
                categoryDao.insertAll(CategorySeed.categories.map { it.toEntity() })
            }
            seeded = true
        }
    }

    override suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction.toEntity())
    }

    override suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun delete(id: String) {
        transactionDao.getById(id)?.let { transactionDao.delete(it) }
    }
}
