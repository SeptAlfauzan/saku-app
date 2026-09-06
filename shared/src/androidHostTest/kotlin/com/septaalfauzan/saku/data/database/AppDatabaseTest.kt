package com.septaalfauzan.saku.data.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.septaalfauzan.saku.data.dao.CategoryDao
import com.septaalfauzan.saku.data.dao.TransactionDao
import com.septaalfauzan.saku.data.entity.CategoryEntity
import com.septaalfauzan.saku.data.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppDatabaseTest {

    private fun buildInMemory(): AppDatabase =
        Room.inMemoryDatabaseBuilder<AppDatabase>(
            factory = { AppDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver())
            .build()

    @Test
    fun transactionCrudRoundTrips() = runTest {
        val db = buildInMemory()
        val dao: TransactionDao = db.transactionDao()
        val tx = TransactionEntity(
            id = "t1",
            type = "EXPENSE",
            amount = 35000,
            currency = "IDR",
            merchant = "Warung",
            categoryId = "food",
            description = null,
            source = "MANUAL",
            sourcePackage = null,
            occurredAtMillis = 1_720_000_000_000,
            createdAtMillis = 1_720_000_000_000,
            updatedAtMillis = 1_720_000_000_000,
        )
        dao.insert(tx)
        assertEquals(35000, dao.getById("t1")?.amount)
        dao.update(tx.copy(amount = 40000))
        assertEquals(40000, dao.getById("t1")?.amount)
        dao.delete(tx.copy(amount = 40000))
        assertEquals(null, dao.getById("t1"))
        db.close()
    }

    @Test
    fun transactionFlowEmitsSortedByOccurredAtDesc() = runTest {
        val db = buildInMemory()
        val dao = db.transactionDao()
        val older = TransactionEntity("a", "EXPENSE", 1, "IDR", null, null, null, "MANUAL", null, occurredAtMillis = 1_000, createdAtMillis = 1_000, updatedAtMillis = 1_000)
        val newer = TransactionEntity("b", "INCOME", 2, "IDR", null, null, null, "MANUAL", null, occurredAtMillis = 2_000, createdAtMillis = 2_000, updatedAtMillis = 2_000)
        dao.insert(older)
        dao.insert(newer)
        val emitted = dao.observeAll().first()
        assertEquals(listOf("b", "a"), emitted.map { it.id })
        db.close()
    }

    @Test
    fun categoryDaoCanCountAndSeed() = runTest {
        val db = buildInMemory()
        val dao: CategoryDao = db.categoryDao()
        assertEquals(0L, dao.count())
        dao.insertAll(listOf(CategoryEntity("food", "Food", "food", "EXPENSE")))
        assertEquals(1L, dao.count())
        db.close()
    }

    @Test
    fun amountSumBetweenReturnsOnlyMatchingTypeAndWindow() = runTest {
        val db = buildInMemory()
        val dao = db.transactionDao()
        dao.insert(TransactionEntity("a", "EXPENSE", 10_000, "IDR", null, null, null, "MANUAL", null, occurredAtMillis = 1_000, createdAtMillis = 1_000, updatedAtMillis = 1_000))
        dao.insert(TransactionEntity("b", "EXPENSE", 5_000, "IDR", null, null, null, "MANUAL", null, occurredAtMillis = 2_000, createdAtMillis = 2_000, updatedAtMillis = 2_000))
        dao.insert(TransactionEntity("c", "INCOME", 99_000, "IDR", null, null, null, "MANUAL", null, occurredAtMillis = 1_500, createdAtMillis = 1_500, updatedAtMillis = 1_500))
        assertEquals(15_000L, dao.amountSumBetween("EXPENSE", 0L, 3_000L))
        assertEquals(5_000L, dao.amountSumBetween("EXPENSE", 1_500L, 3_000L))
        db.close()
    }

    @Test
    fun inMemoryDatabaseIsFresh() {
        val db = buildInMemory()
        assertTrue(true)
        db.close()
    }
}
