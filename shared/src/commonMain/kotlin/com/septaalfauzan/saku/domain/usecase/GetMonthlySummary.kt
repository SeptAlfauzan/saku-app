package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

data class MonthlySummary(
    val income: Long,
    val expense: Long,
)

fun startOfMonthMillis(year: Int, month: Int, zone: TimeZone): Long {
    val first = LocalDate(year, month, 1)
    return first.atStartOfDayIn(zone).toEpochMilliseconds()
}

fun endOfMonthMillis(year: Int, month: Int, zone: TimeZone): Long {
    val first = LocalDate(year, month, 1)
    val nextMonth = first.plus(1, kotlinx.datetime.DateTimeUnit.MONTH)
    return nextMonth.atStartOfDayIn(zone).toEpochMilliseconds() - 1
}

fun sumMonthly(transactions: List<Transaction>, year: Int, month: Int, zone: TimeZone): MonthlySummary {
    val start = startOfMonthMillis(year, month, zone)
    val end = endOfMonthMillis(year, month, zone)
    var income = 0L
    var expense = 0L
    for (tx in transactions) {
        if (tx.occurredAt.toEpochMilliseconds() !in start..end) continue
        when (tx.type) {
            TransactionType.INCOME -> income += tx.amount
            TransactionType.EXPENSE -> expense += tx.amount
            TransactionType.TRANSFER -> Unit
        }
    }
    return MonthlySummary(income = income, expense = expense)
}

class GetMonthlySummary(private val observeTransactions: ObserveTransactions) {
    operator fun invoke(year: Int, month: Int, zone: TimeZone = TimeZone.currentSystemDefault()): Flow<MonthlySummary> =
        observeTransactions().map { txs -> sumMonthly(txs, year, month, zone) }
}