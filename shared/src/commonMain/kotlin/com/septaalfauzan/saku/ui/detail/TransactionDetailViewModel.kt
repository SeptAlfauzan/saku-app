package com.septaalfauzan.saku.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.usecase.DeleteTransaction
import com.septaalfauzan.saku.domain.usecase.ObserveTransactions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailEvent {
    data object Deleted : DetailEvent
    data object EditRequested : DetailEvent
}

class TransactionDetailViewModel(
    private val observeTransactions: ObserveTransactions,
    private val deleteTransaction: DeleteTransaction,
    private val transactionId: String,
) : ViewModel() {

    private val eventFlow = MutableStateFlow<DetailEvent?>(null)

    val events: StateFlow<DetailEvent?> = eventFlow

    val uiState: StateFlow<Transaction?> =
        observeTransactions()
            .map { txs -> txs.firstOrNull { it.id == transactionId } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    fun requestEdit() {
        eventFlow.value = DetailEvent.EditRequested
    }

    fun requestDelete() {
        viewModelScope.launch {
            deleteTransaction(transactionId)
            eventFlow.value = DetailEvent.Deleted
        }
    }

    fun consumeEvent() {
        eventFlow.value = null
    }
}
