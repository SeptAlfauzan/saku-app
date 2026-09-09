package com.septaalfauzan.saku.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.usecase.ObservePending
import com.septaalfauzan.saku.domain.usecase.SetTransactionStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReviewQueueUiState(
    val pending: List<Transaction> = emptyList(),
)

class ReviewQueueViewModel(
    observePending: ObservePending,
    private val setTransactionStatus: SetTransactionStatus,
) : ViewModel() {

    val uiState: StateFlow<ReviewQueueUiState> = observePending()
        .map { ReviewQueueUiState(pending = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReviewQueueUiState(),
        )

    fun confirm(id: String) {
        viewModelScope.launch { setTransactionStatus(id, TransactionStatus.CONFIRMED) }
    }

    fun ignore(id: String) {
        viewModelScope.launch { setTransactionStatus(id, TransactionStatus.IGNORED) }
    }
}
