package com.septaalfauzan.saku.data.importexport

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.septaalfauzan.saku.data.database.AppDatabase
import com.septaalfauzan.saku.data.database.AppDatabaseConstructor
import com.septaalfauzan.saku.data.database.CategorySeed
import com.septaalfauzan.saku.data.database.NotificationSourceSeed
import com.septaalfauzan.saku.data.entity.CategoryEntity
import com.septaalfauzan.saku.data.entity.NotificationSourceEntity
import com.septaalfauzan.saku.data.repository.RoomNotificationSettingsRepository
import com.septaalfauzan.saku.data.repository.RoomTransactionRepository
import com.septaalfauzan.saku.domain.importexport.ExportFilter
import com.septaalfauzan.saku.domain.importexport.ExportTransactions
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportTransactionsTest {

    private fun build(): AppDatabase =
        Room.inMemoryDatabaseBuilder<AppDatabase>(
            factory = { AppDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver()).build()

    private fun tx(id: String, type: TransactionType, amount: Long, categoryId: String?) = Transaction(
        id = id, type = type, amount = amount, currency = "IDR",
        merchant = "Merchant $id", categoryId = categoryId, description = null,
        source = TransactionSource.NOTIFICATION, sourcePackage = "com.gojek.app",
        status = TransactionStatus.CONFIRMED, confidence = 0.75,
        occurredAt = Instant.fromEpochMilliseconds(1_770_000_000_000),
        createdAt = Instant.fromEpochMilliseconds(1_770_000_000_000),
        updatedAt = Instant.fromEpochMilliseconds(1_770_000_000_000),
    )

    private suspend fun seed(db: AppDatabase) {
        db.categoryDao().insertAll(CategorySeed.categories.map { CategoryEntity(it.id, it.name, it.icon, it.type.name) })
        db.sourceDao().insertAll(NotificationSourceSeed.sources.map { NotificationSourceEntity(it.packageName, it.providerId, it.enabled) })
    }

    @Test
    fun exportExcludesTransfersAndReportsCount() = runTest {
        val db = build()
        val dao = db.transactionDao()
        seed(db)
        val repo = RoomTransactionRepository(db, dao, db.categoryDao())
        repo.applyImport(listOf(
            tx("e1", TransactionType.EXPENSE, 1000, "transport"),
            tx("i1", TransactionType.INCOME, 500, "salary"),
            tx("t1", TransactionType.TRANSFER, 300, null),
        )).let { }

        val usecase = ExportTransactions(repo, RoomNotificationSettingsRepository(db.sourceDao(), db.settingsDao(), db.keywordDao()))
        val result = usecase.export(ExportFilter())

        assertEquals(2, result.exportedCount)
        assertEquals(1, result.excludedTransfers)
        assertTrue(result.csv.startsWith("Transaction ID,Type,Amount,Currency,Merchant,Category"))
        assertTrue(result.csv.contains("Merchant e1,Transport"))
        assertTrue(result.csv.contains("Auto-detected,gopay"))
        assertTrue(!result.csv.contains("t1"))
        assertTrue(result.csv.contains("75%"))
    }

    @Test
    fun exportAppliesTypeCategoryAndDateFilter() = runTest {
        val db = build()
        val dao = db.transactionDao()
        seed(db)
        val repo = RoomTransactionRepository(db, dao, db.categoryDao())
        repo.applyImport(listOf(
            tx("e1", TransactionType.EXPENSE, 1000, "transport"),
            tx("e2", TransactionType.EXPENSE, 2000, "food"),
            tx("i1", TransactionType.INCOME, 500, "salary"),
        )).let { }

        val usecase = ExportTransactions(repo, RoomNotificationSettingsRepository(db.sourceDao(), db.settingsDao(), db.keywordDao()))
        val byCategory = usecase.export(ExportFilter(categoryId = "transport"))
        assertEquals(1, byCategory.exportedCount)
        assertTrue(byCategory.csv.contains("e1") && !byCategory.csv.contains("e2"))

        val byType = usecase.export(ExportFilter(type = TransactionType.INCOME))
        assertEquals(1, byType.exportedCount)
        assertTrue(byType.csv.contains("i1"))
    }
}
