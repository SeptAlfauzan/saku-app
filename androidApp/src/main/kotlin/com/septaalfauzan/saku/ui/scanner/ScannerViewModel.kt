package com.septaalfauzan.saku.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ScannerPhase { VIEWFINDER, SCANNING, RESULT }

enum class ScannerFlash { AUTO, ON, OFF }

data class ScannerUiState(
    val phase: ScannerPhase = ScannerPhase.VIEWFINDER,
    val capturedPath: String? = null,
    val flash: ScannerFlash = ScannerFlash.AUTO,
    val message: String? = null,
)

class ScannerViewModel : ViewModel() {
    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    fun onCaptured(path: String) {
        _state.value = _state.value.copy(phase = ScannerPhase.SCANNING, capturedPath = path)
        viewModelScope.launch {
            delay(1500)
            _state.value = _state.value.copy(phase = ScannerPhase.RESULT)
        }
    }

    fun retake() {
        _state.value = ScannerUiState()
    }

    fun approve() {
        _state.value = _state.value.copy(message = "Mock only — OCR not yet wired.")
    }

    fun edit() {
        _state.value = _state.value.copy(message = "Mock only — edit not yet wired.")
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun toggleFlash() {
        _state.value = _state.value.copy(
            flash = when (_state.value.flash) {
                ScannerFlash.AUTO -> ScannerFlash.ON
                ScannerFlash.ON -> ScannerFlash.OFF
                ScannerFlash.OFF -> ScannerFlash.AUTO
            },
        )
    }
}
