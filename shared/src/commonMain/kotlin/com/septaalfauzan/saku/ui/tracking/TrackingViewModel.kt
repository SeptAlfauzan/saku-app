package com.septaalfauzan.saku.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.usecase.ObserveAutoConfirm
import com.septaalfauzan.saku.domain.usecase.ObserveNotificationSources
import com.septaalfauzan.saku.domain.usecase.ObserveTrackingEnabled
import com.septaalfauzan.saku.domain.usecase.SetAutoConfirm
import com.septaalfauzan.saku.domain.usecase.SetNotificationSourceEnabled
import com.septaalfauzan.saku.domain.usecase.SetTrackingEnabled
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrackingUiState(
    val trackingEnabled: Boolean = false,
    val autoConfirm: Boolean = true,
    val sources: List<NotificationSource> = emptyList(),
)

class TrackingViewModel(
    observeTrackingEnabled: ObserveTrackingEnabled,
    private val setTrackingEnabled: SetTrackingEnabled,
    observeAutoConfirm: ObserveAutoConfirm,
    private val setAutoConfirm: SetAutoConfirm,
    observeSources: ObserveNotificationSources,
    private val setSourceEnabled: SetNotificationSourceEnabled,
) : ViewModel() {

    val uiState: StateFlow<TrackingUiState> = combine(
        observeTrackingEnabled(),
        observeAutoConfirm(),
        observeSources(),
    ) { tracking, autoConfirm, sources ->
        TrackingUiState(trackingEnabled = tracking, autoConfirm = autoConfirm, sources = sources)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackingUiState(),
    )

    fun setTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch { setTrackingEnabled(enabled) }
    }

    fun setAutoConfirm(enabled: Boolean) {
        viewModelScope.launch { setAutoConfirm(enabled) }
    }

    fun setSourceEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch { setSourceEnabled(packageName, enabled) }
    }
}
