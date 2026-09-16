package com.septaalfauzan.saku.ui.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.septaalfauzan.saku.domain.model.AddEditFormState
import com.septaalfauzan.saku.domain.model.AddEditUiState
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.usecase.AddTransaction
import com.septaalfauzan.saku.domain.usecase.ObserveCategories
import com.septaalfauzan.saku.domain.usecase.ObserveTransactions
import com.septaalfauzan.saku.domain.usecase.UpdateTransaction
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


sealed interface AddEditEvent {
    data object Saved : AddEditEvent
    data object NavigateBack : AddEditEvent
}

class AddEditTransactionViewModel(
    private val observeCategories: ObserveCategories,
    private val observeTransactions: ObserveTransactions,
    private val addTransaction: AddTransaction,
    private val updateTransaction: UpdateTransaction,
    transactionId: String? = null,
    prefillJson: String? = null,
    private val editingScan: Boolean = false
) : ViewModel() {

    private val eventFlow = MutableStateFlow<AddEditEvent?>(null)

    val events: StateFlow<AddEditEvent?> = eventFlow

    private var existingCreatedAt: Instant? = null
    private var existingTransaction: Transaction? = null

    private val fieldState = MutableStateFlow(AddEditFormState())

    val uiState: StateFlow<AddEditUiState> = combine(
        fieldState,
        observeCategories(),
    ) { fields, categories ->
        AddEditUiState(
            editingId = fields.editingId,
            type = fields.type,
            amountInput = fields.amountInput,
            categoryId = fields.categoryId,
            merchant = fields.merchant,
            note = fields.note,
            occurredAtMillis = fields.occurredAtMillis,
            categories = categories,
            amountError = fields.amountError,
            categoryError = fields.categoryError,
            canSave = recomputeCanSave(fields),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AddEditUiState(),
    )

    init {
        if (transactionId != null) {
            if (!prefillJson.isNullOrBlank()) {
                ScanPrefill.decode(prefillJson)?.let { prefill ->
                    fieldState.value = fieldState.value.withPrefill(prefill)
                }
            } else {
                viewModelScope.launch {
                    val existing =
                        observeTransactions().first().firstOrNull { it.id == transactionId }
                    if (existing != null) {
                        existingCreatedAt = existing.createdAt
                        existingTransaction = existing
                        fieldState.value = fieldState.value.copy(
                            editingId = existing.id,
                            type = existing.type,
                            amountInput = existing.amount.toString(),
                            categoryId = existing.categoryId,
                            merchant = existing.merchant.orEmpty(),
                            note = existing.description.orEmpty(),
                            occurredAtMillis = existing.occurredAt.toEpochMilliseconds(),
                        )
                    }
                }
            }
        } else if (!prefillJson.isNullOrBlank()) {
            ScanPrefill.decode(prefillJson)?.let { prefill ->
                fieldState.value = fieldState.value.withPrefill(prefill)
            }
        }
    }

    fun updateType(type: TransactionType) {
        mutate { it.copy(type = type) }
    }

    fun updateAmount(input: String) {
        mutate { it.copy(amountInput = input) }
    }

    fun updateCategory(id: String?) {
        mutate { it.copy(categoryId = id) }
    }

    fun updateMerchant(value: String) {
        mutate { it.copy(merchant = value) }
    }

    fun updateNote(value: String) {
        mutate { it.copy(note = value) }
    }

    fun updateDate(millis: Long) {
        mutate { it.copy(occurredAtMillis = millis) }
    }

    private fun mutate(transform: (AddEditFormState) -> AddEditFormState) {
        val current = fieldState.value
        fieldState.value = transform(current).copy(
            amountError = null,
            categoryError = null,
        )
    }

    private fun recomputeCanSave(state: AddEditFormState): Boolean {
        val amount = InputValidation.parseAmountToLong(state.amountInput)
        val errors = InputValidation.validate(state.type, amount, state.categoryId)
        return errors.isEmpty()
    }

    fun save() {
        val state = fieldState.value
        val amount = InputValidation.parseAmountToLong(state.amountInput)
        val errors = InputValidation.validate(state.type, amount, state.categoryId)
        if (errors.isNotEmpty()) {
            fieldState.value = state.copy(
                amountError = errors.firstOrNull { it.startsWith("Amount") },
                categoryError = errors.firstOrNull { it.startsWith("Category") },
            )
            return
        }

        viewModelScope.launch {
            if (!editingScan) {

                val occurredAt = Instant.fromEpochMilliseconds(state.occurredAtMillis)
                val editingId = state.editingId
                if (editingId == null) {
                    val tx = addTransaction(
                        type = state.type,
                        amount = requireNotNull(amount),
                        merchant = state.merchant.ifBlank { null },
                        categoryId = state.categoryId,
                        description = state.note.ifBlank { null },
                        occurredAt = occurredAt,
                    )
                    addTransaction.store(tx)
                } else {
                    updateTransaction.store(
                        Transaction(
                            id = editingId,
                            type = state.type,
                            amount = requireNotNull(amount),
                            currency = "IDR",
                            merchant = state.merchant.ifBlank { null },
                            categoryId = state.categoryId,
                            description = state.note.ifBlank { null },
                            source = existingTransaction?.source ?: TransactionSource.MANUAL,
                            sourcePackage = existingTransaction?.sourcePackage,
                            status = TransactionStatus.CONFIRMED,
                            confidence = existingTransaction?.confidence ?: 0.0,
                            occurredAt = occurredAt,
                            createdAt = existingCreatedAt ?: Clock.System.now(),
                            updatedAt = Clock.System.now(),
                        )
                    )
                }
            }
            eventFlow.value = AddEditEvent.Saved
        }
    }

    fun onBack() {
        eventFlow.value = AddEditEvent.NavigateBack
    }

    fun consumeEvent() {
        eventFlow.value = null
    }
}
