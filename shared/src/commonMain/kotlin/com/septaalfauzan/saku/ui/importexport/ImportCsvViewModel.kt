package com.septaalfauzan.saku.ui.importexport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.septaalfauzan.saku.domain.importexport.CsvError
import com.septaalfauzan.saku.domain.importexport.ImportAction
import com.septaalfauzan.saku.domain.importexport.ImportTransactions
import com.septaalfauzan.saku.domain.importexport.TransactionDraft
import com.septaalfauzan.saku.domain.importexport.UndoSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ImportCsvUiState {
    data object Idle : ImportCsvUiState
    data class Parsing(val fileName: String) : ImportCsvUiState

    data class Preview(
        val fileName: String,
        val newCount: Int = 0,
        val updateCount: Int = 0,
        val duplicateCount: Int = 0,
        val errors: List<CsvError> = emptyList(),
        val sample: List<TransactionDraft> = emptyList(),
        val skipDuplicates: Boolean = true,
    ) : ImportCsvUiState {
        val blocked: Boolean get() = errors.isNotEmpty()
        val importableCount: Int
            get() = if (skipDuplicates) newCount + updateCount
            else newCount + updateCount + duplicateCount
    }

    data class Applying(val previous: Preview) : ImportCsvUiState

    data class Done(
        val newCount: Int,
        val updateCount: Int,
        val skippedDuplicates: Int,
        val snapshot: UndoSnapshot,
    ) : ImportCsvUiState

    data class Failed(val reason: String) : ImportCsvUiState
}

class ImportCsvViewModel(
    private val importTransactions: ImportTransactions,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportCsvUiState>(ImportCsvUiState.Idle)
    val uiState: StateFlow<ImportCsvUiState> = _uiState

    private var pendingActions: List<ImportAction> = emptyList()

    fun onFilePicked(fileName: String, bytes: ByteArray) {
        if (_uiState.value != ImportCsvUiState.Idle) return
        _uiState.value = ImportCsvUiState.Parsing(fileName)
        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                val text = bytes.toString(Charsets.UTF_8)
                val parsed = importTransactions.parse(text)
                if (parsed.errors.isNotEmpty()) {
                    ImportCsvUiState.Preview(
                        fileName = fileName,
                        errors = parsed.errors,
                    )
                } else {
                    val classification = importTransactions.classify(parsed.drafts)
                    pendingActions = classification.actions
                    ImportCsvUiState.Preview(
                        fileName = fileName,
                        newCount = classification.newCount,
                        updateCount = classification.updateCount,
                        duplicateCount = classification.duplicateCount,
                        sample = parsed.drafts.take(20),
                    )
                }
            }.onSuccess { state -> _uiState.value = state }
                .onFailure { error ->
                    _uiState.value = ImportCsvUiState.Failed(error.message ?: "File tidak bisa dibaca")
                }
        }
    }

    fun setSkipDuplicates(skip: Boolean) {
        val preview = _uiState.value as? ImportCsvUiState.Preview ?: return
        if (preview.blocked) return
        _uiState.value = preview.copy(skipDuplicates = skip)
    }

    fun confirmImport() {
        val preview = _uiState.value as? ImportCsvUiState.Preview ?: return
        if (preview.blocked) return
        _uiState.value = ImportCsvUiState.Applying(preview)
        viewModelScope.launch {
            val actions = if (preview.skipDuplicates) {
                pendingActions.filterNot { it is ImportAction.DuplicateAction }
            } else {
                pendingActions
            }
            runCatching {
                val result = importTransactions.apply(actions)
                ImportCsvUiState.Done(
                    newCount = result.newCount,
                    updateCount = result.updateCount,
                    skippedDuplicates = if (preview.skipDuplicates) preview.duplicateCount else 0,
                    snapshot = result.snapshot,
                )
            }.onSuccess { state -> _uiState.value = state }
                .onFailure { error ->
                    _uiState.value = ImportCsvUiState.Failed(error.message ?: "Impor gagal")
                }
        }
    }

    fun undo() {
        val done = _uiState.value as? ImportCsvUiState.Done ?: return
        viewModelScope.launch {
            runCatching { importTransactions.undo(done.snapshot) }
            _uiState.value = ImportCsvUiState.Idle
        }
    }

    fun reset() {
        pendingActions = emptyList()
        _uiState.value = ImportCsvUiState.Idle
    }
}
