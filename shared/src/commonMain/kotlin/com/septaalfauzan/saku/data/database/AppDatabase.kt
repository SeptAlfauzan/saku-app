package com.septaalfauzan.saku.data.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.septaalfauzan.saku.data.dao.CategoryDao
import com.septaalfauzan.saku.data.dao.TransactionDao
import com.septaalfauzan.saku.data.entity.CategoryEntity
import com.septaalfauzan.saku.data.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers

const val DATABASE_NAME = "saku.db"

@Database(entities = [TransactionEntity::class, CategoryEntity::class], version = 1, exportSchema = true)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

expect fun createDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

fun buildRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder.setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
