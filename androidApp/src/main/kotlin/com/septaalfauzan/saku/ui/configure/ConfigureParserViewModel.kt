package com.septaalfauzan.saku.ui.configure

import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.septaalfauzan.saku.di.AndroidContextHolder
import com.septaalfauzan.saku.domain.model.KeywordType
import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.ParserKeyword
import com.septaalfauzan.saku.domain.usecase.AddNotificationSource
import com.septaalfauzan.saku.domain.usecase.DeleteNotificationSource
import com.septaalfauzan.saku.domain.usecase.ObserveNotificationSources
import com.septaalfauzan.saku.domain.usecase.ObserveParserKeywords
import com.septaalfauzan.saku.domain.usecase.UpdateParserKeywords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

data class InstalledApp(
    val label: String,
    val packageName: String,
)

data class ConfigureParserUiState(
    val installedApps: List<InstalledApp> = emptyList(),
    val filteredApps: List<InstalledApp> = emptyList(),
    val searchQuery: String = "",
    val selectedPackage: String? = null,
    val selectedLabel: String = "",
    val expenseWords: List<String> = emptyList(),
    val incomeWords: List<String> = emptyList(),
    val merchantWords: List<String> = emptyList(),
    val isExistingSource: Boolean = false,
    val testNotificationText: String = "",
)

class ConfigureParserViewModel(
    observeNotificationSources: ObserveNotificationSources,
    private val addNotificationSource: AddNotificationSource,
    private val deleteNotificationSource: DeleteNotificationSource,
    private val updateParserKeywords: UpdateParserKeywords,
    private val observeParserKeywords: ObserveParserKeywords,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigureParserUiState())
    val uiState: StateFlow<ConfigureParserUiState> = _uiState.asStateFlow()

    val sources: StateFlow<List<NotificationSource>> = observeNotificationSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        val pm = AndroidContextHolder.applicationContext.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { InstalledApp(label = it.loadLabel(pm).toString(), packageName = it.packageName) }
            .sortedBy { it.label.lowercase() }
        Log.d("APPS", "apps $apps")
        _uiState.value = _uiState.value.copy(installedApps = apps)
    }

    fun onSearchQueryChanged(query: String) {
        val filtered = if (query.isBlank()) emptyList()
        else _uiState.value.installedApps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }.take(20)
        _uiState.value = _uiState.value.copy(searchQuery = query, filteredApps = filtered)
    }

    fun onSelectApp(packageName: String) {
        val app = _uiState.value.installedApps.find { it.packageName == packageName }
        val isExisting = sources.value.any { it.packageName == packageName }
        viewModelScope.launch {
            observeParserKeywords(packageName).collect { keywordList ->
                _uiState.value = _uiState.value.copy(
                    selectedPackage = packageName,
                    selectedLabel = app?.label ?: packageName,
                    searchQuery = app?.label ?: packageName,
                    filteredApps = emptyList(),
                    isExistingSource = isExisting,
                    expenseWords = keywordList.filter { it.keywordType == KeywordType.EXPENSE }.map { it.keyword },
                    incomeWords = keywordList.filter { it.keywordType == KeywordType.INCOME }.map { it.keyword },
                    merchantWords = keywordList.filter { it.keywordType == KeywordType.MERCHANT }.map { it.keyword },
                )
            }
        }
    }

    fun onAddKeyword(type: KeywordType, word: String) {
        if (word.isBlank()) return
        val trimmed = word.trim().lowercase()
        _uiState.value = when (type) {
            KeywordType.EXPENSE -> _uiState.value.copy(expenseWords = _uiState.value.expenseWords + trimmed)
            KeywordType.INCOME -> _uiState.value.copy(incomeWords = _uiState.value.incomeWords + trimmed)
            KeywordType.MERCHANT -> _uiState.value.copy(merchantWords = _uiState.value.merchantWords + trimmed)
        }
    }

    fun onRemoveKeyword(type: KeywordType, word: String) {
        _uiState.value = when (type) {
            KeywordType.EXPENSE -> _uiState.value.copy(expenseWords = _uiState.value.expenseWords - word)
            KeywordType.INCOME -> _uiState.value.copy(incomeWords = _uiState.value.incomeWords - word)
            KeywordType.MERCHANT -> _uiState.value.copy(merchantWords = _uiState.value.merchantWords - word)
        }
    }

    fun onSave() {
        val state = _uiState.value
        val pkg = state.selectedPackage ?: return
        viewModelScope.launch {
            if (!state.isExistingSource) {
                addNotificationSource(pkg, state.selectedLabel.ifBlank { pkg }, emptyList())
            }
            updateParserKeywords(pkg, state.expenseWords, state.incomeWords, state.merchantWords)
            _uiState.value = _uiState.value.copy(selectedPackage = null, searchQuery = "")
        }
    }

    fun onDeleteApp(packageName: String) {
        viewModelScope.launch {
            deleteNotificationSource(packageName)
            if (_uiState.value.selectedPackage == packageName) onDismissApp()
        }
    }

    fun onTestTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(testNotificationText = text)
    }

    fun onDismissApp() {
        _uiState.value = _uiState.value.copy(
            selectedPackage = null,
            selectedLabel = "",
            expenseWords = emptyList(),
            incomeWords = emptyList(),
            merchantWords = emptyList(),
            isExistingSource = false,
            searchQuery = "",
            filteredApps = emptyList(),
            testNotificationText = "",
        )
    }
}
