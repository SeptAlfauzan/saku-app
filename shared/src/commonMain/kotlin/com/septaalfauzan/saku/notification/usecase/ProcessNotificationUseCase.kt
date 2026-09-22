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
import com.septaalfauzan.saku.notification.provider.ParserRegistry
import com.septaalfauzan.saku.sentry.NoopSentryReporter
import com.septaalfauzan.saku.sentry.SentryReporter
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first

class ProcessNotificationUseCase(
    private val engine: NotificationParserEngine,
    private val settings: NotificationSettingsRepository,
    private val repository: TransactionRepository,
    private val duplicateDetector: DuplicateDetector,
    private val parserRegistry: ParserRegistry,
    private val reporter: SentryReporter = NoopSentryReporter,
    private val clock: () -> Instant = { Clock.System.now() },
) {
    private var rebuildDone = false

    suspend operator fun invoke(notification: NotificationData) {
        if (!settings.observeTrackingEnabled().first()) return

        if (!rebuildDone) {
            rebuildParserRegistry()
            rebuildDone = true
        }

        val enabled = settings.observeSources().first()
            .any { it.packageName == notification.packageName && it.enabled }
        if (!enabled) return

        val parsed = try {
            engine.process(notification) ?: return
        } catch (e: Throwable) {
            reporter.captureException(e)
            return
        }
        val type = parsed.type ?: return
        val amount = parsed.amount ?: return

        val occurredAt = parsed.occurredAt ?: notification.postedAt
        val now = clock()

        val key = DuplicateKey(notification.packageName, type, amount)
        val isDuplicateWithInTimeRange = duplicateDetector.isDuplicate(key, occurredAt.toEpochMilliseconds())
        if (isDuplicateWithInTimeRange) return

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
        reporter.addBreadcrumb("transaction created type=${type} amount=${amount}", "notification")
    }

    private suspend fun rebuildParserRegistry() {
        val sources = settings.observeSources().first()
        val allKeywords = sources.associate { it.packageName to settings.getKeywords(it.packageName) }
        parserRegistry.rebuild(sources, allKeywords)
    }
}
