package com.septaalfauzan.saku.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.usecase.GetMonthlySummary
import com.septaalfauzan.saku.domain.usecase.ObserveTransactions
import com.septaalfauzan.saku.util.monthLabel
import kotlin.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

data class DashboardUiState(
    val income: Long = 0L,
    val expense: Long = 0L,
    val balance: Long = 0L,
    val monthLabel: String = "",
    val recentTransactions: List<Transaction> = emptyList(),
)

class DashboardViewModel(
    observeTransactions: ObserveTransactions,
    getMonthlySummary: GetMonthlySummary,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = observeTransactions()
        .map { txs -> txs.filter { it.status == TransactionStatus.CONFIRMED } }
        .flatMapLatest { allTx ->
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            getMonthlySummary(now.year, now.month.number).map { summary ->
                DashboardUiState(
                    income = summary.income,
                    expense = summary.expense,
                    balance = summary.income - summary.expense,
                    monthLabel = monthLabel(now.year, now.month.number),
                    recentTransactions = allTx.take(10),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(),
        )
}
