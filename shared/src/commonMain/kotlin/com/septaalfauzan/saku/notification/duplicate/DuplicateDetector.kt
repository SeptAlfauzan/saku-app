package com.septaalfauzan.saku.notification.duplicate

import com.septaalfauzan.saku.domain.model.DuplicateKey
import com.septaalfauzan.saku.domain.repository.TransactionRepository

const val DUPLICATE_WINDOW_MS = 120_000L

class DuplicateDetector(private val repository: TransactionRepository) {
    suspend fun isDuplicate(key: DuplicateKey, atMillis: Long): Boolean =
        repository.findRecentDuplicate(
            key = key,
            withinStartMillis = atMillis - DUPLICATE_WINDOW_MS,
            withinEndMillis = atMillis + DUPLICATE_WINDOW_MS,
        ) != null
}
