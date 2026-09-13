package com.septaalfauzan.saku.ui.importexport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.septaalfauzan.saku.domain.importexport.ExportFilter
import com.septaalfauzan.saku.domain.importexport.ExportTransactions
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

enum class DatePreset { ALL_TIME, THIS_MONTH, THIS_YEAR }

data class ExportCsvUiState(
    val categories: List<Category> = emptyList(),
    val datePreset: DatePreset = DatePreset.ALL_TIME,
    val dateStart: LocalDate? = null,
    val dateEnd: LocalDate? = null,
    val type: TransactionType? = null,
    val categoryId: String? = null,
    val building: Boolean = false,
    val message: String? = null,
)

class ExportCsvViewModel(
    private val exportTransactions: ExportTransactions,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportCsvUiState())
    val uiState: StateFlow<ExportCsvUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                categories = transactionRepository.observeCategories().first(),
            )
        }
    }

    fun setDatePreset(preset: DatePreset) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val (start, end) = when (preset) {
            DatePreset.ALL_TIME -> null to null
            DatePreset.THIS_MONTH -> LocalDate(today.year, today.month.number, 1) to today
            DatePreset.THIS_YEAR -> LocalDate(today.year, 1, 1) to today
        }
        _uiState.value = _uiState.value.copy(
            datePreset = preset,
            dateStart = start,
            dateEnd = end,
            message = null,
        )
    }

    fun setType(type: TransactionType?) {
        _uiState.value = _uiState.value.copy(type = type, message = null)
    }

    fun setCategory(id: String?) {
        _uiState.value = _uiState.value.copy(categoryId = id, message = null)
    }

    fun buildCsv(onResult: (ExportTransactions.Result) -> Unit) {
        val state = _uiState.value
        if (state.building) return
        _uiState.value = state.copy(building = true, message = null)
        viewModelScope.launch {
            runCatching {
                val zone = TimeZone.currentSystemDefault()
                exportTransactions.export(
                    ExportFilter(
                        dateStart = state.dateStart?.atStartOfDayIn(zone),
                        dateEnd = state.dateEnd?.plus(1, DateTimeUnit.DAY)?.atStartOfDayIn(zone)
                            ?.minus(1.nanoseconds),
                        type = state.type,
                        categoryId = state.categoryId,
                    ),
                )
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(building = false)
                onResult(result)
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    building = false,
                    message = "Ekspor gagal. Silakan coba lagi.",
                )
            }
        }
    }
}