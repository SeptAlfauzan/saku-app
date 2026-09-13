package com.septaalfauzan.saku.data.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.septaalfauzan.saku.data.dao.CategoryDao
import com.septaalfauzan.saku.data.dao.TransactionDao
import com.septaalfauzan.saku.data.database.AppDatabase
import com.septaalfauzan.saku.data.database.AppDatabaseConstructor
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomTransactionRepositoryImportTest {

    private fun buildInMemory(): AppDatabase =
        Room.inMemoryDatabaseBuilder<AppDatabase>(
            factory = { AppDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver()).build()

    private fun tx(
        id: String,
        type: TransactionType = TransactionType.EXPENSE,
        amount: Long = 1000,
    ) = Transaction(
        id = id, type = type, amount = amount, currency = "IDR",
        merchant = null, categoryId = null, description = null,
        source = TransactionSource.MANUAL, sourcePackage = null,
        status = TransactionStatus.CONFIRMED, confidence = 0.0,
        occurredAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
        createdAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
        updatedAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
    )

    @Test
    fun applyImportCreatesNewAndUpdatesExistingAtomically() = runTest {
        val db = buildInMemory()
        val dao: TransactionDao = db.transactionDao()
        val transactionDao = dao
        val categoryDao: CategoryDao = db.categoryDao()
        categoryDao.insertAll(com.septaalfauzan.saku.data.database.CategorySeed.categories.map {
            com.septaalfauzan.saku.data.entity.CategoryEntity(it.id, it.name, it.icon, it.type.name)
        })
        dao.insert(com.septaalfauzan.saku.data.entity.TransactionEntity(
            id = "existing", type = "EXPENSE", amount = 500, currency = "IDR",
            merchant = null, categoryId = null, description = null, source = "MANUAL",
            sourcePackage = null, status = "CONFIRMED", confidence = 0.0,
            occurredAtMillis = 1_720_000_000_000, createdAtMillis = 1_720_000_000_000,
            updatedAtMillis = 1_720_000_000_000,
        ))
        val repo = RoomTransactionRepository(db, transactionDao, categoryDao)

        val result = repo.applyImport(listOf(tx("new"), tx("existing", amount = 999)))

        assertEquals(1, result.newCount)
        assertEquals(1, result.updateCount)
        assertEquals(listOf("new"), result.snapshot.newIds)
        assertEquals(999, repo.getAll().first { it.id == "existing" }.amount)
        assertTrue(repo.getAll().any { it.id == "new" })
    }

    @Test
    fun undoRestoresExactlyTouchedRows() = runTest {
        val db = buildInMemory()
        val dao = db.transactionDao()
        val repo = RoomTransactionRepository(db, dao, db.categoryDao())
        dao.insert(com.septaalfauzan.saku.data.entity.TransactionEntity(
            id = "keep", type = "EXPENSE", amount = 1, currency = "IDR",
            merchant = null, categoryId = null, description = null, source = "MANUAL",
            sourcePackage = null, status = "CONFIRMED", confidence = 0.0,
            occurredAtMillis = 1, createdAtMillis = 1, updatedAtMillis = 1,
        ))
        dao.insert(com.septaalfauzan.saku.data.entity.TransactionEntity(
            id = "victim", type = "EXPENSE", amount = 2, currency = "IDR",
            merchant = "old", categoryId = null, description = null, source = "MANUAL",
            sourcePackage = null, status = "CONFIRMED", confidence = 0.0,
            occurredAtMillis = 1, createdAtMillis = 1, updatedAtMillis = 1,
        ))

        val result = repo.applyImport(listOf(tx("fresh"), tx("victim", amount = 9)))
        repo.undoImport(result.snapshot)

        assertEquals(setOf("keep", "victim"), repo.getAll().map { it.id }.toSet())
        assertEquals(2L, repo.getAll().first { it.id == "victim" }.amount)
    }

    @Test
    fun getAllReturnsSnapshotOfAllRows() = runTest {
        val db = buildInMemory()
        val dao = db.transactionDao()
        val repo = RoomTransactionRepository(db, dao, db.categoryDao())
        repo.applyImport(listOf(tx("a"), tx("b")))
        assertEquals(listOf("a", "b").sorted(), repo.getAll().map { it.id }.sorted())
    }
}
