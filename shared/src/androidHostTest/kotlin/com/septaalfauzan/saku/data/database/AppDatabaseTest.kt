package com.septaalfauzan.saku.data.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.septaalfauzan.saku.data.dao.CategoryDao
import com.septaalfauzan.saku.data.dao.TransactionDao
import com.septaalfauzan.saku.data.entity.CategoryEntity
import com.septaalfauzan.saku.data.entity.TransactionEntity
import com.septaalfauzan.saku.data.repository.RoomNotificationSettingsRepository
import com.septaalfauzan.saku.data.repository.RoomTransactionRepository
import com.septaalfauzan.saku.domain.model.DuplicateKey
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppDatabaseTest {

    private fun buildInMemory(): AppDatabase =
        Room.inMemoryDatabaseBuilder<AppDatabase>(
            factory = { AppDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver())
            .build()

    private fun tx(
        id: String,
        type: TransactionType,
        amount: Long,
        status: TransactionStatus = TransactionStatus.CONFIRMED,
    ) = Transaction(
        id = id,
        type = type,
        amount = amount,
        currency = "IDR",
        merchant = null,
        categoryId = null,
        description = null,
        source = TransactionSource.MANUAL,
        sourcePackage = null,
        status = status,
        confidence = 0.0,
        occurredAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
        createdAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
        updatedAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
    )

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
            status = "CONFIRMED",
            confidence = 0.0,
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
        val older = TransactionEntity(
            id = "a", type = "EXPENSE", amount = 1, currency = "IDR",
            merchant = null, categoryId = null, description = null,
            source = "MANUAL", sourcePackage = null,
            status = "CONFIRMED", confidence = 0.0,
            occurredAtMillis = 1_000, createdAtMillis = 1_000, updatedAtMillis = 1_000,
        )
        val newer = TransactionEntity(
            id = "b", type = "INCOME", amount = 2, currency = "IDR",
            merchant = null, categoryId = null, description = null,
            source = "MANUAL", sourcePackage = null,
            status = "CONFIRMED", confidence = 0.0,
            occurredAtMillis = 2_000, createdAtMillis = 2_000, updatedAtMillis = 2_000,
        )
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
        dao.insert(
            TransactionEntity(
                id = "a", type = "EXPENSE", amount = 10_000, currency = "IDR",
                merchant = null, categoryId = null, description = null,
                source = "MANUAL", sourcePackage = null,
                status = "CONFIRMED", confidence = 0.0,
                occurredAtMillis = 1_000, createdAtMillis = 1_000, updatedAtMillis = 1_000,
            ),
        )
        dao.insert(
            TransactionEntity(
                id = "b", type = "EXPENSE", amount = 5_000, currency = "IDR",
                merchant = null, categoryId = null, description = null,
                source = "MANUAL", sourcePackage = null,
                status = "CONFIRMED", confidence = 0.0,
                occurredAtMillis = 2_000, createdAtMillis = 2_000, updatedAtMillis = 2_000,
            ),
        )
        dao.insert(
            TransactionEntity(
                id = "c", type = "INCOME", amount = 99_000, currency = "IDR",
                merchant = null, categoryId = null, description = null,
                source = "MANUAL", sourcePackage = null,
                status = "CONFIRMED", confidence = 0.0,
                occurredAtMillis = 1_500, createdAtMillis = 1_500, updatedAtMillis = 1_500,
            ),
        )
        assertEquals(15_000L, dao.amountSumBetween("EXPENSE", 0L, 3_000L))
        assertEquals(5_000L, dao.amountSumBetween("EXPENSE", 1_500L, 3_000L))
        db.close()
    }

    @Test
    fun inMemoryDatabaseIsFresh() = runTest {
        val db = buildInMemory()
        assertEquals(0L, db.categoryDao().count())
        db.close()
    }

    @Test
    fun repositorySeedsAndReturnsAllCategories() = runTest {
        val db = buildInMemory()
        val repo = RoomTransactionRepository(db.transactionDao(), db.categoryDao())
        val categories = repo.observeCategories().first()
        val expenseIds = categories.filter { it.type == TransactionType.EXPENSE }.map { it.id }
        val incomeIds = categories.filter { it.type == TransactionType.INCOME }.map { it.id }
        assertEquals(
            listOf("food", "transport", "shopping", "bills", "entertainment", "health", "education", "insurance", "other_expense"),
            expenseIds,
        )
        assertEquals(
            listOf("salary", "freelance", "cashback", "interest", "other_income"),
            incomeIds,
        )
        db.close()
    }

    @Test
    fun repositorySeedsOnlyOnce() = runTest {
        val db = buildInMemory()
        val repo = RoomTransactionRepository(db.transactionDao(), db.categoryDao())
        val catDao = db.categoryDao()
        repo.observeCategories().first()
        repo.observeCategories().first()
        assertEquals(14L, catDao.count())
        db.close()
    }

    @Test
    fun observePendingReturnsOnlyPendingReviews() = runTest {
        val db = buildInMemory()
        val repo = RoomTransactionRepository(db.transactionDao(), db.categoryDao())
        repo.insert(tx("pending1", TransactionType.EXPENSE, 50_000, status = TransactionStatus.PENDING_REVIEW).copy(sourcePackage = "com.bca"))
        repo.insert(tx("confirmed1", TransactionType.INCOME, 1_000, status = TransactionStatus.CONFIRMED).copy(sourcePackage = "com.bca"))
        assertContentEquals(listOf("pending1"), repo.observePending().first().map { it.id })
        db.close()
    }

    @Test
    fun setStatusMovesTransaction() = runTest {
        val db = buildInMemory()
        val repo = RoomTransactionRepository(db.transactionDao(), db.categoryDao())
        repo.insert(tx("p1", TransactionType.EXPENSE, 10_000, status = TransactionStatus.PENDING_REVIEW))
        repo.setStatus("p1", TransactionStatus.CONFIRMED)
        assertTrue(repo.observePending().first().isEmpty())
        db.close()
    }

    @Test
    fun findRecentDuplicateFindsWithinWindow() = runTest {
        val db = buildInMemory()
        val repo = RoomTransactionRepository(db.transactionDao(), db.categoryDao())
        val existing = tx("dup1", TransactionType.EXPENSE, 150_000, status = TransactionStatus.CONFIRMED)
            .copy(sourcePackage = "com.bca", occurredAt = Instant.fromEpochMilliseconds(1_000_000_000_000))
        repo.insert(existing)
        val hit = repo.findRecentDuplicate(
            DuplicateKey("com.bca", TransactionType.EXPENSE, 150_000),
            withinStartMillis = 1_000_000_000_000 - 120_000,
            withinEndMillis = 1_000_000_000_000 + 120_000,
        )
        assertEquals("dup1", hit?.id)
        db.close()
    }

    @Test
    fun settingsRepositorySeedsAndFlows() = runTest {
        val db = buildInMemory()
        val repo = RoomNotificationSettingsRepository(db.sourceDao(), db.settingsDao(), db.keywordDao())
        assertEquals(false, repo.observeTrackingEnabled().first())
        assertEquals(true, repo.observeAutoConfirm().first())
        val sources = repo.observeSources().first()
        assertEquals(4, sources.size)
        repo.setSourceEnabled("com.bca", false)
        assertEquals(false, repo.observeSources().first().first { it.packageName == "com.bca" }.enabled)
        repo.setTrackingEnabled(true)
        assertEquals(true, repo.observeTrackingEnabled().first())
        db.close()
    }
}
