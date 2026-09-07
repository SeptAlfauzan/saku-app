package com.septaalfauzan.saku.ui.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.usecase.AddTransaction
import com.septaalfauzan.saku.domain.usecase.ObserveCategories
import com.septaalfauzan.saku.domain.usecase.UpdateTransaction
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AddEditFormState(
    val editingId: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val categoryId: String? = null,
    val merchant: String = "",
    val note: String = "",
    val occurredAtMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val amountError: String? = null,
    val categoryError: String? = null,
)

data class AddEditUiState(
    val editingId: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val categoryId: String? = null,
    val merchant: String = "",
    val note: String = "",
    val occurredAtMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val categories: List<Category> = emptyList(),
    val amountError: String? = null,
    val categoryError: String? = null,
    val canSave: Boolean = false,
)

sealed interface AddEditEvent {
    data object Saved : AddEditEvent
    data object NavigateBack : AddEditEvent
}

class AddEditTransactionViewModel(
    private val observeCategories: ObserveCategories,
    private val addTransaction: AddTransaction,
    private val updateTransaction: UpdateTransaction,
    initialTransaction: Transaction? = null,
) : ViewModel() {

    private val eventFlow = MutableStateFlow<AddEditEvent?>(null)

    val events: StateFlow<AddEditEvent?> = eventFlow

    private val fieldState = MutableStateFlow(initialFieldState(initialTransaction))

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
        initialValue = initialUiState(initialTransaction),
    )

    private fun initialFieldState(existing: Transaction?): AddEditFormState = AddEditFormState(
        editingId = existing?.id,
        type = existing?.type ?: TransactionType.EXPENSE,
        amountInput = existing?.amount?.toString() ?: "",
        categoryId = existing?.categoryId,
        merchant = existing?.merchant.orEmpty(),
        note = existing?.description.orEmpty(),
        occurredAtMillis = existing?.occurredAt?.toEpochMilliseconds() ?: Clock.System.now().toEpochMilliseconds(),
    )

    private fun initialUiState(existing: Transaction?): AddEditUiState =
        initialFieldState(existing).let { fields ->
            AddEditUiState(
                editingId = fields.editingId,
                type = fields.type,
                amountInput = fields.amountInput,
                categoryId = fields.categoryId,
                merchant = fields.merchant,
                note = fields.note,
                occurredAtMillis = fields.occurredAtMillis,
                canSave = recomputeCanSave(fields),
            )
        }

    fun updateType(type: TransactionType) { mutate { it.copy(type = type) } }
    fun updateAmount(input: String) { mutate { it.copy(amountInput = input) } }
    fun updateCategory(id: String?) { mutate { it.copy(categoryId = id) } }
    fun updateMerchant(value: String) { mutate { it.copy(merchant = value) } }
    fun updateNote(value: String) { mutate { it.copy(note = value) } }
    fun updateDate(millis: Long) { mutate { it.copy(occurredAtMillis = millis) } }

    private fun mutate(transform: (AddEditFormState) -> AddEditFormState) {
        val current = fieldState.value
        fieldState.value = transform(current).copy(
            amountError = null,
            categoryError = null,
        )
    }

    private fun recomputeCanSave(state: AddEditFormState): Boolean {
        val amount = AddEditValidation.parseAmount(state.amountInput)
        val errors = AddEditValidation.validate(state.type, amount, state.categoryId)
        return errors.isEmpty()
    }

    fun save() {
        val state = fieldState.value
        val amount = AddEditValidation.parseAmount(state.amountInput)
        val errors = AddEditValidation.validate(state.type, amount, state.categoryId)
        if (errors.isNotEmpty()) {
            fieldState.value = state.copy(
                amountError = errors.firstOrNull { it.startsWith("Amount") },
                categoryError = errors.firstOrNull { it.startsWith("Category") },
            )
            return
        }
        viewModelScope.launch {
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
                        source = TransactionSource.MANUAL,
                        sourcePackage = null,
                        occurredAt = occurredAt,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                )
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
