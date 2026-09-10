package com.septaalfauzan.saku.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.septaalfauzan.saku.data.entity.NotificationSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationSourceDao {
    @Query("SELECT * FROM notification_sources ORDER BY providerId ASC")
    fun observeAll(): Flow<List<NotificationSourceEntity>>

    @Query("UPDATE notification_sources SET enabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM notification_sources")
    suspend fun count(): Long

    @Query("DELETE FROM notification_sources WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<NotificationSourceEntity>)
}
