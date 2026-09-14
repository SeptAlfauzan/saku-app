package com.septaalfauzan.saku.data.importexport

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.septaalfauzan.saku.data.database.AppDatabase
import com.septaalfauzan.saku.data.database.AppDatabaseConstructor
import com.septaalfauzan.saku.data.database.CategorySeed
import com.septaalfauzan.saku.data.database.NotificationSourceSeed
import com.septaalfauzan.saku.data.entity.CategoryEntity
import com.septaalfauzan.saku.data.entity.NotificationSourceEntity
import com.septaalfauzan.saku.data.entity.TransactionEntity
import com.septaalfauzan.saku.data.importexport.csv.parseCsvDateTime
import com.septaalfauzan.saku.data.repository.RoomNotificationSettingsRepository
import com.septaalfauzan.saku.data.repository.RoomTransactionRepository
import com.septaalfauzan.saku.domain.importexport.ImportAction
import com.septaalfauzan.saku.domain.importexport.ImportTransactions
import com.septaalfauzan.saku.domain.importexport.ParsedRows
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportTransactionsTest {

    private fun build(): AppDatabase =
        Room.inMemoryDatabaseBuilder<AppDatabase>(
            factory = { AppDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver()).build()

    private suspend fun seed(db: AppDatabase) {
        db.categoryDao().insertAll(CategorySeed.categories.map { CategoryEntity(it.id, it.name, it.icon, it.type.name) })
        db.sourceDao().insertAll(NotificationSourceSeed.sources.map { NotificationSourceEntity(it.packageName, it.providerId, it.enabled) })
    }

    private fun headers() = "Transaction ID,Type,Amount,Currency,Merchant,Category,Notes,Entry Method,Detected From,Status,Date & Time,Created At,Last Updated,Confidence"

    @Test
    fun parseProducesDraftsAndErrors() = runTest {
        val db = build(); seed(db)
        val u = ImportTransactions(
            RoomTransactionRepository(db, db.transactionDao(), db.categoryDao()),
            RoomNotificationSettingsRepository(db.sourceDao(), db.settingsDao(), db.keywordDao()),
        )
        val csv = headers() + "\r\n" +
            "id-9,Expense,90000,IDR,Warung,Transport,Grab,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n" +
            "id-8,Transfer,500,IDR,,,,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n" +
            ",Expense,1000,USD,,,,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n"
        val parsed: ParsedRows = u.parse(csv)
        assertEquals(1, parsed.drafts.size)
        assertEquals("id-9", parsed.drafts.single().id)
        assertEquals(2, parsed.errors.size)
        assertTrue(parsed.errors.any { it.field == "Type" && it.row == 3 })
        assertTrue(parsed.errors.any { it.field == "Currency" && it.row == 4 })
    }

    @Test
    fun classifyMarksUpdatesCreatesAndDuplicates() = runTest {
        val db = build(); seed(db)
        val dao = db.transactionDao()
        dao.insert(TransactionEntity(
            id = "id-1", type = "EXPENSE", amount = 1000, currency = "IDR",
            merchant = "Warung", categoryId = null, description = null, source = "MANUAL",
            sourcePackage = null, status = "CONFIRMED", confidence = 0.0,
            occurredAtMillis = parseCsvDateTime("2026-01-05 14:00:30")!!.toEpochMilliseconds(),
            createdAtMillis = parseCsvDateTime("2026-01-05 14:00:30")!!.toEpochMilliseconds(),
            updatedAtMillis = parseCsvDateTime("2026-01-05 14:00:30")!!.toEpochMilliseconds(),
        ))
        val u = ImportTransactions(
            RoomTransactionRepository(db, dao, db.categoryDao()),
            RoomNotificationSettingsRepository(db.sourceDao(), db.settingsDao(), db.keywordDao()),
        )
        val csv = headers() + "\r\n" +
            "id-1,Expense,1000,IDR,Warung,,,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n" +
            "id-2,Expense,1000,IDR,Warung,,,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n" +
            "id-3,Expense,999,IDR,,,Grab,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n" +
            ",Expense,1000,IDR,Warung,,,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n"
        val parsed = u.parse(csv)
        assertEquals(4, parsed.drafts.size)
        val classification = u.classify(parsed.drafts)
        assertEquals(1, classification.updateCount)
        assertEquals(2, classification.newCount)
        assertEquals(1, classification.duplicateCount)
        assertTrue(classification.actions[0] is ImportAction.UpdateAction)
        assertTrue(classification.actions[3] is ImportAction.DuplicateAction)
    }

    @Test
    fun applyWritesOnlyNonDuplicateActionsAndUndoReverts() = runTest {
        val db = build(); seed(db)
        val repo = RoomTransactionRepository(db, db.transactionDao(), db.categoryDao())
        val u = ImportTransactions(
            repo,
            RoomNotificationSettingsRepository(db.sourceDao(), db.settingsDao(), db.keywordDao()),
        )
        val csv = headers() + "\r\n" +
            "id-1,Expense,1000,IDR,Warung,,,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n" +
            "id-2,Expense,999,IDR,,,Grab,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n"
        val parsed = u.parse(csv)
        val classification = u.classify(parsed.drafts)
        val result = u.apply(classification.actions)
        assertEquals(2, result.newCount)
        u.undo(result.snapshot)
        assertEquals(0, repo.getAll().size)
    }
}
