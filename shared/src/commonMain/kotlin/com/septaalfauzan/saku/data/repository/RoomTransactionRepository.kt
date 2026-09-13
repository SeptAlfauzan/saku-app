package com.septaalfauzan.saku.data.repository

import androidx.room3.withWriteTransaction
import com.septaalfauzan.saku.data.dao.CategoryDao
import com.septaalfauzan.saku.data.dao.TransactionDao
import com.septaalfauzan.saku.data.database.AppDatabase
import com.septaalfauzan.saku.data.database.CategorySeed
import com.septaalfauzan.saku.data.entity.TransactionEntity
import com.septaalfauzan.saku.data.entity.toDomain
import com.septaalfauzan.saku.data.entity.toEntity
import com.septaalfauzan.saku.domain.importexport.ApplyResult
import com.septaalfauzan.saku.domain.importexport.UndoSnapshot
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.DuplicateKey
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomTransactionRepository(
    private val db: AppDatabase,
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

    override fun observePending(): Flow<List<Transaction>> =
        transactionDao.observePending().map { list -> list.map { it.toDomain() } }

    override suspend fun setStatus(id: String, status: TransactionStatus) {
        transactionDao.setStatus(id, status.name)
    }

    override suspend fun findRecentDuplicate(
        key: DuplicateKey,
        withinStartMillis: Long,
        withinEndMillis: Long,
    ): Transaction? =
        transactionDao.findRecentDuplicate(
            sourcePackage = key.sourcePackage,
            type = key.type.name,
            amount = key.amount,
            startMillis = withinStartMillis,
            endMillis = withinEndMillis,
        )?.toDomain()

    override suspend fun getAll(): List<Transaction> =
        transactionDao.getAll().map { it.toDomain() }

    override suspend fun applyImport(changes: List<Transaction>): ApplyResult = db.withWriteTransaction {
        val existing = transactionDao.getAll().associateBy { it.id }
        val created = mutableListOf<String>()
        val updated = mutableListOf<String>()
        val previous = mutableMapOf<String, TransactionEntity>()
        changes.forEach { tx ->
            val entity = tx.toEntity()
            val current = existing[tx.id]
            if (current != null) {
                previous[tx.id] = current
                transactionDao.update(entity)
                updated += tx.id
            } else {
                transactionDao.insert(entity)
                created += tx.id
            }
        }
        ApplyResult(
            newCount = created.size,
            updateCount = updated.size,
            snapshot = UndoSnapshot(created, previous.mapValues { it.value.toDomain() }),
        )
    }

    override suspend fun undoImport(snapshot: UndoSnapshot) = db.withWriteTransaction {
        snapshot.previousById.forEach { (_, tx) -> transactionDao.update(tx.toEntity()) }
        transactionDao.deleteByIds(snapshot.newIds)
    }
}
