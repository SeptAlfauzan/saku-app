package com.septaalfauzan.saku.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.septaalfauzan.saku.data.entity.ParserKeywordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParserKeywordDao {
    @Query("SELECT * FROM parser_keywords WHERE packageName = :packageName")
    fun observeByPackage(packageName: String): Flow<List<ParserKeywordEntity>>

    @Query("SELECT * FROM parser_keywords WHERE packageName = :packageName")
    suspend fun getKeywords(packageName: String): List<ParserKeywordEntity>

    @Query("SELECT * FROM parser_keywords")
    fun observeAll(): Flow<List<ParserKeywordEntity>>

    @Query("DELETE FROM parser_keywords WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ParserKeywordEntity>)

    @Query("SELECT COUNT(*) FROM parser_keywords WHERE packageName = :packageName")
    suspend fun countByPackage(packageName: String): Long
}
