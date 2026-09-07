package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthlySummaryTest {

    private fun tx(
        id: String,
        type: TransactionType,
        amount: Long,
        epochMs: Long,
    ) = Transaction(
        id = id,
        type = type,
        amount = amount,
        currency = "IDR",
        merchant = null,
        categoryId = null,
        description = null,
        source = TransactionSource.MANUAL,
        sourcePackage = null,
        occurredAt = Instant.fromEpochMilliseconds(epochMs),
        createdAt = Instant.fromEpochMilliseconds(epochMs),
        updatedAt = Instant.fromEpochMilliseconds(epochMs),
    )

    // 2026-09-10 00:00 UTC and friends
    private val sep10 = 1_788_998_400_000L
    private val sep01 = 1_788_220_800_000L
    private val sep30 = 1_790_726_400_000L
    private val aug31 = 1_788_134_400_000L
    private val oct01 = 1_790_812_800_000L

    @Test
    fun sumsOnlyTransactionsInsideTargetMonth() {
        val txs = listOf(
            tx("in", TransactionType.INCOME, 6_500_000, sep10),
            tx("exp", TransactionType.EXPENSE, 350_000, sep10),
            tx("exp2", TransactionType.EXPENSE, 100_000, sep10),
            tx("before", TransactionType.EXPENSE, 999, aug31),
            tx("after", TransactionType.EXPENSE, 888, oct01),
        )
        val summary = sumMonthly(txs, 2026, 9, TimeZone.UTC)
        assertEquals(6_500_000L, summary.income)
        assertEquals(450_000L, summary.expense)
    }

    @Test
    fun handlesMonthBoundaryFirstAndLastDay() {
        val txs = listOf(
            tx("first", TransactionType.EXPENSE, 1_000, sep01),
            tx("last", TransactionType.EXPENSE, 2_000, sep30),
        )
        val summary = sumMonthly(txs, 2026, 9, TimeZone.UTC)
        assertEquals(3_000L, summary.expense)
    }

    @Test
    fun ignoresTransfers() {
        val txs = listOf(
            tx("transfer", TransactionType.TRANSFER, 1_000_000, sep10),
            tx("expense", TransactionType.EXPENSE, 50_000, sep10),
        )
        val summary = sumMonthly(txs, 2026, 9, TimeZone.UTC)
        assertEquals(0L, summary.income)
        assertEquals(50_000L, summary.expense)
    }

    @Test
    fun emptyMonthReturnsZero() {
        val summary = sumMonthly(emptyList(), 2026, 9, TimeZone.UTC)
        assertEquals(0L, summary.income)
        assertEquals(0L, summary.expense)
    }
}
