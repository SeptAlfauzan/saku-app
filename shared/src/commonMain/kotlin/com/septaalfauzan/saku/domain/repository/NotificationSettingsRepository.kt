package com.septaalfauzan.saku.domain.repository

import com.septaalfauzan.saku.domain.model.NotificationSource
import kotlinx.coroutines.flow.Flow

interface NotificationSettingsRepository {
    fun observeSources(): Flow<List<NotificationSource>>
    suspend fun setSourceEnabled(packageName: String, enabled: Boolean)
    fun observeTrackingEnabled(): Flow<Boolean>
    suspend fun setTrackingEnabled(enabled: Boolean)
    fun observeAutoConfirm(): Flow<Boolean>
    suspend fun setAutoConfirm(enabled: Boolean)
}
