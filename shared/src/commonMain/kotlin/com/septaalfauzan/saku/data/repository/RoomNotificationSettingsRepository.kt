package com.septaalfauzan.saku.data.repository

import com.septaalfauzan.saku.data.dao.NotificationSourceDao
import com.septaalfauzan.saku.data.dao.ParserKeywordDao
import com.septaalfauzan.saku.data.dao.SettingsDao
import com.septaalfauzan.saku.data.database.NotificationSourceSeed
import com.septaalfauzan.saku.data.database.NotificationSourceSeed.KEY_AUTO_CONFIRM
import com.septaalfauzan.saku.data.database.NotificationSourceSeed.KEY_TRACKING_ENABLED
import com.septaalfauzan.saku.data.entity.SettingsEntity
import com.septaalfauzan.saku.data.entity.toDomain
import com.septaalfauzan.saku.data.entity.toEntity
import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.ParserKeyword
import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class RoomNotificationSettingsRepository(
    private val sourceDao: NotificationSourceDao,
    private val settingsDao: SettingsDao,
    private val keywordDao: ParserKeywordDao,
) : NotificationSettingsRepository {

    var onConfigChanged: (suspend () -> Unit)? = null

    private var seeded = false

    private suspend fun ensureSeeded() {
        if (seeded) return
        if (sourceDao.count() == 0L) {
            sourceDao.insertAll(NotificationSourceSeed.sources.map { it.toEntity() })
        }
        if (keywordDao.countByPackage("com.bca") == 0L) {
            val allKeywords = NotificationSourceSeed.defaultKeywords.values.flatten()
            keywordDao.insertAll(allKeywords.map { it.toEntity() })
        }
        if (settingsDao.count() == 0L) {
            settingsDao.upsert(SettingsEntity(KEY_TRACKING_ENABLED, "false"))
            settingsDao.upsert(SettingsEntity(KEY_AUTO_CONFIRM, "true"))
        }
        seeded = true
    }

    override fun observeSources(): Flow<List<NotificationSource>> =
        flow {
            ensureSeeded()
            emitAll(sourceDao.observeAll())
        }.map { list -> list.map { it.toDomain() } }

    override suspend fun setSourceEnabled(packageName: String, enabled: Boolean) {
        ensureSeeded()
        sourceDao.setEnabled(packageName, enabled)
        onConfigChanged?.invoke()
    }

    override fun observeTrackingEnabled(): Flow<Boolean> =
        flow {
            ensureSeeded()
            emitAll(settingsDao.observeValue(KEY_TRACKING_ENABLED))
        }.map { it == "true" }

    override suspend fun setTrackingEnabled(enabled: Boolean) =
        settingsDao.upsert(SettingsEntity(KEY_TRACKING_ENABLED, enabled.toString()))

    override fun observeAutoConfirm(): Flow<Boolean> =
        flow {
            ensureSeeded()
            emitAll(settingsDao.observeValue(KEY_AUTO_CONFIRM))
        }.map { it == "true" }

    override suspend fun setAutoConfirm(enabled: Boolean) =
        settingsDao.upsert(SettingsEntity(KEY_AUTO_CONFIRM, enabled.toString()))

    override fun observeKeywords(packageName: String): Flow<List<ParserKeyword>> =
        flow {
            ensureSeeded()
            emitAll(keywordDao.observeByPackage(packageName))
        }.map { list -> list.map { it.toDomain() } }

    override suspend fun getKeywords(packageName: String): List<ParserKeyword> {
        ensureSeeded()
        return keywordDao.getKeywords(packageName).map { it.toDomain() }
    }

    override suspend fun upsertKeywords(
        packageName: String,
        expenseWords: List<String>,
        incomeWords: List<String>,
        merchantWords: List<String>,
    ) {
        keywordDao.upsertAll(
            packageName = packageName,
            expenseWords = expenseWords,
            incomeWords = incomeWords,
            merchantWords = merchantWords,
        )
        onConfigChanged?.invoke()
    }

    override suspend fun deleteSource(packageName: String) {
        keywordDao.deleteByPackage(packageName)
        sourceDao.deleteByPackage(packageName)
        onConfigChanged?.invoke()
    }

    override suspend fun addSource(source: NotificationSource, keywords: List<ParserKeyword>) {
        sourceDao.insertAll(listOf(source.toEntity()))
        keywordDao.insertAll(keywords.map { it.toEntity() })
        onConfigChanged?.invoke()
    }
}