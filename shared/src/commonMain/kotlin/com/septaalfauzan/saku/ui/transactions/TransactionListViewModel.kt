package com.septaalfauzan.saku.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.usecase.ObserveTransactions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    private val dateRangeFlow = MutableStateFlow<Pair<Long?, Long>?>(null)
    private val _transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    val transactionsFlow: StateFlow<List<Transaction>> = _transactionsFlow.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionListState> = combine(
        dateRangeFlow.flatMapLatest { pair ->
            observeTransactions(pair?.first, pair?.second)
        },
        filterFlow,
        _transactionsFlow,
    ) { txs, filter, manualTxs ->
        val confirmed = txs.filter { it.status == TransactionStatus.CONFIRMED }
        val filtered = when (filter) {
            TransactionFilter.ALL -> confirmed
            TransactionFilter.INCOME -> confirmed.filter { it.type == TransactionType.INCOME }
            TransactionFilter.EXPENSE -> confirmed.filter { it.type == TransactionType.EXPENSE }
        }
        val transactions = if (manualTxs.isNotEmpty()) manualTxs else filtered
        TransactionListState(filter = filter, transactions = transactions)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionListState(),
    )

    fun updateTransactions(transactions: List<Transaction>) {
        _transactionsFlow.value = transactions
    }

    fun filterByDates(startDateMils: Long, endDateMils: Long) {
        dateRangeFlow.value = startDateMils to endDateMils
    }

    fun setFilter(filter: TransactionFilter) {
        filterFlow.value = filter
    }
}
