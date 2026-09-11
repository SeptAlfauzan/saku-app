package com.septaalfauzan.saku.notification

import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.DuplicateKey
import com.septaalfauzan.saku.domain.model.KeywordType
import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.ParserKeyword
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import com.septaalfauzan.saku.notification.duplicate.DuplicateDetector
import com.septaalfauzan.saku.notification.engine.NotificationParserEngine
import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.notification.provider.ParserRegistry
import com.septaalfauzan.saku.notification.usecase.ProcessNotificationUseCase
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProcessNotificationUseCaseTest {

    private class FakeSettingsRepository(
        overrideSourceEnabled: Boolean = true,
        overrideTracking: Boolean = true,
        overrideAutoConfirm: Boolean = true,
        initialKeywords: Map<String, List<ParserKeyword>> = emptyMap(),
    ) : NotificationSettingsRepository {
        private val sources = MutableStateFlow(listOf(NotificationSource("com.bca", "bca", overrideSourceEnabled)))
        private val tracking = MutableStateFlow(overrideTracking)
        private val autoConfirm = MutableStateFlow(overrideAutoConfirm)
        private val keywords = MutableStateFlow<Map<String, List<ParserKeyword>>>(initialKeywords)
        override fun observeSources(): Flow<List<NotificationSource>> = sources
        override suspend fun setSourceEnabled(packageName: String, enabled: Boolean) { sources.value = sources.value.map { if (it.packageName == packageName) it.copy(enabled = enabled) else it } }
        override fun observeTrackingEnabled(): Flow<Boolean> = tracking
        override suspend fun setTrackingEnabled(enabled: Boolean) { tracking.value = enabled }
        override fun observeAutoConfirm(): Flow<Boolean> = autoConfirm
        override suspend fun setAutoConfirm(enabled: Boolean) { autoConfirm.value = enabled }
        override fun observeKeywords(packageName: String): Flow<List<ParserKeyword>> = flowOf(keywords.value[packageName] ?: emptyList())
        override suspend fun getKeywords(packageName: String): List<ParserKeyword> = keywords.value[packageName] ?: emptyList()
        override suspend fun upsertKeywords(packageName: String, expenseWords: List<String>, incomeWords: List<String>, merchantWords: List<String>) {
            val kw = buildList {
                expenseWords.forEach { add(ParserKeyword(packageName, KeywordType.EXPENSE, it)) }
                incomeWords.forEach { add(ParserKeyword(packageName, KeywordType.INCOME, it)) }
                merchantWords.forEach { add(ParserKeyword(packageName, KeywordType.MERCHANT, it)) }
            }
            keywords.value = keywords.value + (packageName to kw)
        }
        override suspend fun deleteSource(packageName: String) {
            sources.value = sources.value.filterNot { it.packageName == packageName }
            keywords.value = keywords.value - packageName
        }
        override suspend fun addSource(source: NotificationSource, keywords: List<ParserKeyword>) {
            sources.value = sources.value + source
            this.keywords.value = this.keywords.value + (source.packageName to keywords)
        }
    }

    private class FakeTransactionRepository(
        private val dupes: List<Transaction> = emptyList(),
    ) : TransactionRepository {
        val inserted = mutableListOf<Transaction>()
        override fun observeTransactions(): Flow<List<Transaction>> = flowOf(inserted.toList())
        override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun observePending(): Flow<List<Transaction>> = flowOf(inserted.filter { it.status == TransactionStatus.PENDING_REVIEW })
        override suspend fun insert(transaction: Transaction) { inserted += transaction }
        override suspend fun update(transaction: Transaction) {
            val i = inserted.indexOfFirst { it.id == transaction.id }
            if (i >= 0) inserted[i] = transaction
        }
        override suspend fun delete(id: String) { inserted.removeAll { it.id == id } }
        override suspend fun setStatus(id: String, status: TransactionStatus) {
            val i = inserted.indexOfFirst { it.id == id }
            if (i >= 0) inserted[i] = inserted[i].copy(status = status)
        }
        override suspend fun findRecentDuplicate(key: DuplicateKey, withinStartMillis: Long, withinEndMillis: Long): Transaction? =
            dupes.firstOrNull {
                it.sourcePackage == key.sourcePackage && it.type == key.type && it.amount == key.amount &&
                    it.occurredAt.toEpochMilliseconds() in withinStartMillis..withinEndMillis
            }
    }

    private val bcaKeywords = listOf(
        ParserKeyword("com.bca", KeywordType.EXPENSE, "pembayaran"),
        ParserKeyword("com.bca", KeywordType.EXPENSE, "pembelian"),
        ParserKeyword("com.bca", KeywordType.EXPENSE, "debit"),
        ParserKeyword("com.bca", KeywordType.EXPENSE, "transaksi kartu"),
        ParserKeyword("com.bca", KeywordType.EXPENSE, "pengeluaran"),
        ParserKeyword("com.bca", KeywordType.INCOME, "transfer masuk"),
        ParserKeyword("com.bca", KeywordType.INCOME, "dana masuk"),
        ParserKeyword("com.bca", KeywordType.INCOME, "diterima"),
        ParserKeyword("com.bca", KeywordType.INCOME, "pemasukan"),
        ParserKeyword("com.bca", KeywordType.MERCHANT, "berhasil di "),
        ParserKeyword("com.bca", KeywordType.MERCHANT, "di "),
        ParserKeyword("com.bca", KeywordType.MERCHANT, "ke "),
        ParserKeyword("com.bca", KeywordType.MERCHANT, "dari "),
        ParserKeyword("com.bca", KeywordType.MERCHANT, "merchant "),
    )

    private fun bcaSettings(
        overrideSourceEnabled: Boolean = true,
        overrideTracking: Boolean = true,
        overrideAutoConfirm: Boolean = true,
    ) = FakeSettingsRepository(
        overrideSourceEnabled = overrideSourceEnabled,
        overrideTracking = overrideTracking,
        overrideAutoConfirm = overrideAutoConfirm,
        initialKeywords = mapOf("com.bca" to bcaKeywords),
    )

    private fun bcaNotification(body: String) = NotificationData(
        packageName = "com.bca",
        title = "BCA Mobile",
        body = body,
        postedAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
        notificationId = 7,
    )

    private fun useCase(repo: TransactionRepository, settings: NotificationSettingsRepository, dupes: List<Transaction> = emptyList()): ProcessNotificationUseCase {
        val registry = ParserRegistry()
        registry.rebuild(
            listOf(NotificationSource("com.bca", "bca", enabled = true)),
            mapOf("com.bca" to bcaKeywords),
        )
        return ProcessNotificationUseCase(
            engine = NotificationParserEngine(registry),
            settings = settings,
            repository = repo,
            duplicateDetector = DuplicateDetector(repo),
            parserRegistry = registry,
        )
    }

    @Test
    fun highConfidenceExpenseIsInsertedConfirmedWithAutoConfirm() = runTest {
        val repo = FakeTransactionRepository()
        useCase(repo, bcaSettings()).invoke(bcaNotification("Pembayaran Rp150.000 berhasil di TOKOPEDIA"))
        val tx = repo.inserted.single()
        assertEquals(TransactionSource.NOTIFICATION, tx.source)
        assertEquals("com.bca", tx.sourcePackage)
        assertEquals(TransactionStatus.CONFIRMED, tx.status)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(150_000L, tx.amount)
        assertEquals("Tokopedia", tx.merchant)
        assertEquals("shopping", tx.categoryId)
        assertEquals(1.00, tx.confidence, 0.001)
    }

    @Test
    fun lowConfidenceIncomeIsInsertedPendingReview() = runTest {
        val repo = FakeTransactionRepository()
        useCase(repo, bcaSettings()).invoke(bcaNotification("Transfer masuk Rp2.000.000 dari SEPTA ALFAUZAN"))
        val tx = repo.inserted.single()
        assertEquals(TransactionStatus.PENDING_REVIEW, tx.status)
        assertEquals(TransactionType.INCOME, tx.type)
        assertEquals(0.70, tx.confidence, 0.001)
    }

    @Test
    fun autoConfirmOffKeepsEvenHighConfidenceAsPending() = runTest {
        val repo = FakeTransactionRepository()
        useCase(repo, bcaSettings(overrideAutoConfirm = false)).invoke(bcaNotification("Pembayaran Rp150.000 berhasil di TOKOPEDIA"))
        assertEquals(TransactionStatus.PENDING_REVIEW, repo.inserted.single().status)
    }

    @Test
    fun trackingDisabledSkipsProcessing() = runTest {
        val repo = FakeTransactionRepository()
        useCase(repo, FakeSettingsRepository(overrideTracking = false)).invoke(bcaNotification("Pembayaran Rp150.000 berhasil di TOKOPEDIA"))
        assertTrue(repo.inserted.isEmpty())
    }

    @Test
    fun disabledSourceSkipsProcessing() = runTest {
        val repo = FakeTransactionRepository()
        useCase(repo, FakeSettingsRepository(overrideSourceEnabled = false)).invoke(bcaNotification("Pembayaran Rp150.000 berhasil di TOKOPEDIA"))
        assertTrue(repo.inserted.isEmpty())
    }

    @Test
    fun nonTransactionNotificationIsDropped() = runTest {
        val repo = FakeTransactionRepository()
        useCase(repo, bcaSettings()).invoke(bcaNotification("Nikmati promo cashback 50%"))
        assertTrue(repo.inserted.isEmpty())
    }

    @Test
    fun duplicateWithinWindowIsDropped() = runTest {
        val repo = FakeTransactionRepository(dupes = listOf(
            Transaction(
                id = "existing", type = TransactionType.EXPENSE, amount = 150_000, currency = "IDR",
                merchant = "Tokopedia", categoryId = "shopping", description = null,
                source = TransactionSource.NOTIFICATION, sourcePackage = "com.bca",
                status = TransactionStatus.CONFIRMED, confidence = 1.0,
                occurredAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
                createdAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
                updatedAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
            ),
        ))
        useCase(repo, bcaSettings()).invoke(bcaNotification("Pembayaran Rp150.000 berhasil di TOKOPEDIA"))
        assertTrue(repo.inserted.isEmpty())
    }
}
