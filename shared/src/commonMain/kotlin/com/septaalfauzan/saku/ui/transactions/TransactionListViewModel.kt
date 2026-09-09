package com.septaalfauzan.saku.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.usecase.ObserveTransactions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class TransactionFilter { ALL, INCOME, EXPENSE }

data class TransactionListState(
    val filter: TransactionFilter = TransactionFilter.ALL,
    val transactions: List<Transaction> = emptyList(),
)

class TransactionListViewModel(
    observeTransactions: ObserveTransactions,
) : ViewModel() {

    private val filterFlow = MutableStateFlow(TransactionFilter.ALL)

    val uiState: StateFlow<TransactionListState> = combine(
        observeTransactions(),
        filterFlow,
    ) { txs, filter ->
        val confirmed = txs.filter { it.status == TransactionStatus.CONFIRMED }
        val filtered = when (filter) {
            TransactionFilter.ALL -> confirmed
            TransactionFilter.INCOME -> confirmed.filter { it.type == TransactionType.INCOME }
            TransactionFilter.EXPENSE -> confirmed.filter { it.type == TransactionType.EXPENSE }
        }
        TransactionListState(filter = filter, transactions = filtered)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionListState(),
    )

    fun setFilter(filter: TransactionFilter) {
        filterFlow.value = filter
    }
}
