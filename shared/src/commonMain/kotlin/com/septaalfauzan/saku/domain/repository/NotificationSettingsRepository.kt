package com.septaalfauzan.saku.domain.repository

import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.ParserKeyword
import kotlinx.coroutines.flow.Flow

interface NotificationSettingsRepository {
    fun observeSources(): Flow<List<NotificationSource>>
    suspend fun setSourceEnabled(packageName: String, enabled: Boolean)
    fun observeTrackingEnabled(): Flow<Boolean>
    suspend fun setTrackingEnabled(enabled: Boolean)
    fun observeAutoConfirm(): Flow<Boolean>
    suspend fun setAutoConfirm(enabled: Boolean)

    fun observeKeywords(packageName: String): Flow<List<ParserKeyword>>
    suspend fun getKeywords(packageName: String): List<ParserKeyword>
    suspend fun upsertKeywords(
        packageName: String,
        expenseWords: List<String>,
        incomeWords: List<String>,
        merchantWords: List<String>,
    )
    suspend fun deleteSource(packageName: String)
    suspend fun addSource(source: NotificationSource, keywords: List<ParserKeyword>)
}
