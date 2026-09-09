package com.septaalfauzan.saku.notification.usecase

import com.septaalfauzan.saku.domain.model.DuplicateKey
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import com.septaalfauzan.saku.notification.confidence.ConfidenceEngine
import com.septaalfauzan.saku.notification.duplicate.DuplicateDetector
import com.septaalfauzan.saku.notification.engine.NotificationParserEngine
import com.septaalfauzan.saku.notification.model.NotificationData
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first

class ProcessNotificationUseCase(
    private val engine: NotificationParserEngine,
    private val settings: NotificationSettingsRepository,
    private val repository: TransactionRepository,
    private val duplicateDetector: DuplicateDetector,
    private val clock: () -> Instant = { Clock.System.now() },
) {
    suspend operator fun invoke(notification: NotificationData) {
        if (!settings.observeTrackingEnabled().first()) return

        val enabled = settings.observeSources().first()
            .any { it.packageName == notification.packageName && it.enabled }
        if (!enabled) return

        val parsed = engine.process(notification) ?: return
        val type = parsed.type ?: return
        val amount = parsed.amount ?: return

        val occurredAt = parsed.occurredAt ?: notification.postedAt
        val now = clock()

        val key = DuplicateKey(notification.packageName, type, amount)
        if (duplicateDetector.isDuplicate(key, occurredAt.toEpochMilliseconds())) return

        val autoConfirm = settings.observeAutoConfirm().first()
        val status = if (autoConfirm && parsed.confidence >= ConfidenceEngine.AUTO_CONFIRM_THRESHOLD) {
            TransactionStatus.CONFIRMED
        } else {
            TransactionStatus.PENDING_REVIEW
        }

        repository.insert(
            Transaction(
                id = Uuid.random().toString(),
                type = type,
                amount = amount,
                currency = parsed.currency ?: "IDR",
                merchant = parsed.merchant,
                categoryId = parsed.categoryId,
                description = parsed.description,
                source = TransactionSource.NOTIFICATION,
                sourcePackage = notification.packageName,
                status = status,
                confidence = parsed.confidence,
                occurredAt = occurredAt,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}
