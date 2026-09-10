package com.septaalfauzan.saku.data.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AppDatabaseMigrationTest {

    private val v1TransactionsSql =
        "CREATE TABLE IF NOT EXISTS `transactions` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `amount` INTEGER NOT NULL, " +
            "`currency` TEXT NOT NULL, `merchant` TEXT, `categoryId` TEXT, `description` TEXT, `source` TEXT NOT NULL, " +
            "`sourcePackage` TEXT, `status` TEXT NOT NULL, `occurredAtMillis` INTEGER NOT NULL, `createdAtMillis` INTEGER NOT NULL, " +
            "`updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))"

    private val v1CategoriesSql =
        "CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, " +
            "`type` TEXT NOT NULL, PRIMARY KEY(`id`))"

    @Test
    fun migratingV1ToV2PreservesDataAndAddsConfidence() = runTest {
        val baseName = "migrate_v1_v2_${System.nanoTime()}.db"
        val fileName = File(System.getProperty("java.io.tmpdir"), baseName).path
        val driver = BundledSQLiteDriver()
        try {
            driver.open(fileName).use { conn ->
                conn.execSQL(v1TransactionsSql)
                conn.execSQL(v1CategoriesSql)
                conn.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                conn.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '37243a9bb77a903f49f93d0c109355c4')")
                conn.execSQL(
                    "INSERT INTO transactions (id,type,amount,currency,merchant,categoryId,description,source,sourcePackage,status,occurredAtMillis,createdAtMillis,updatedAtMillis) " +
                        "VALUES ('old1','EXPENSE',150000,'IDR','Tokopedia','shopping',NULL,'MANUAL',NULL,'CONFIRMED',1720000000000,1720000000000,1720000000000)",
                )
                conn.execSQL("PRAGMA user_version = 1")
            }
            val db = Room.databaseBuilder<AppDatabase>(
                factory = { AppDatabaseConstructor.initialize() },
                name = fileName,
            ).setDriver(BundledSQLiteDriver())
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
            val restored = db.transactionDao().getById("old1")
            assertNotNull(restored)
            assertEquals("Tokopedia", restored.merchant)
            assertEquals(150_000, restored.amount)
            assertEquals(0.0, restored.confidence)
            db.close()
        } finally {
            File(fileName).delete()
            File("$fileName.lck").delete()
        }
    }
}
