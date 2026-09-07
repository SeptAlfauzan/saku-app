package com.septaalfauzan.saku.data.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.septaalfauzan.saku.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAtMillis DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Insert
    suspend fun insert(entity: TransactionEntity)

    @Update
    suspend fun update(entity: TransactionEntity)

    @Delete
    suspend fun delete(entity: TransactionEntity)

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE type = :type AND occurredAtMillis BETWEEN :startMillis AND :endMillis"
    )
    suspend fun amountSumBetween(type: String, startMillis: Long, endMillis: Long): Long

    @Query("SELECT * FROM transactions WHERE status = 'PENDING_REVIEW' ORDER BY occurredAtMillis DESC")
    fun observePending(): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: String)

    @Query(
        "SELECT * FROM transactions WHERE sourcePackage = :sourcePackage AND type = :type AND amount = :amount " +
            "AND occurredAtMillis BETWEEN :startMillis AND :endMillis LIMIT 1",
    )
    suspend fun findRecentDuplicate(
        sourcePackage: String,
        type: String,
        amount: Long,
        startMillis: Long,
        endMillis: Long,
    ): TransactionEntity?
}
