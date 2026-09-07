package com.septaalfauzan.saku.data.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.room3.migration.Migration

val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE transactions ADD COLUMN confidence REAL NOT NULL DEFAULT 0.0")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `notification_sources` (`packageName` TEXT NOT NULL, `providerId` TEXT NOT NULL, " +
                "`enabled` INTEGER NOT NULL, PRIMARY KEY(`packageName`))",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `settings` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))",
        )
    }
}
