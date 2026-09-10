package com.septaalfauzan.saku.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.usecase.GetMonthlySummary
import com.septaalfauzan.saku.domain.usecase.ObservePending
import com.septaalfauzan.saku.domain.usecase.ObserveTrackingEnabled
import com.septaalfauzan.saku.domain.usecase.ObserveTransactions
import com.septaalfauzan.saku.extension.isToday
import com.septaalfauzan.saku.ui.tracking.TrackingUiState
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
    observePending: ObservePending,
    observeTrackingEnabled: ObserveTrackingEnabled,
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
    val burnRateState: StateFlow<List<Float>> = uiState
        .map { state ->
            state.recentTransactions.filter { transaction ->
                transaction.occurredAt.isToday() &&
                        transaction.type == TransactionType.EXPENSE
            }
                .sortedBy { item -> item.occurredAt }
                .map { item -> item.amount / 10.toFloat() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
    val needReviewState: StateFlow<Int> = observePending().map { state -> state.size }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0,

        )

    val notificationListenerState: StateFlow<Boolean> = observeTrackingEnabled().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
}
