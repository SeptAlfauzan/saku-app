package com.septaalfauzan.saku.ui.scanner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.codec.binary.Base64
import com.septaalfauzan.saku.domain.model.Receipt
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.model.defaultExpenseId
import com.septaalfauzan.saku.domain.model.toItemsNote
import com.septaalfauzan.saku.domain.usecase.AddTransaction
import com.septaalfauzan.saku.domain.usecase.GetReceiptValue
import com.septaalfauzan.saku.domain.usecase.ObserveCategories
import com.septaalfauzan.saku.ui.state.StateUi
import com.septaalfauzan.saku.util.ImageCompressor
import com.septaalfauzan.saku.util.Logger
import com.septaalfauzan.saku.util.parseReceiptDate
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

enum class ScannerPhase { VIEWFINDER, SCANNING, RESULT }

enum class ScannerFlash { AUTO, ON, OFF }

data class ScannerUiState(
    val phase: ScannerPhase = ScannerPhase.VIEWFINDER,
    val capturedPath: String? = null,
    val flash: ScannerFlash = ScannerFlash.OFF,
    val message: String? = null,
)

sealed interface ScannerEvent {
    data object Saved : ScannerEvent
}

class ScannerViewModel(
    private val getReceiptValue: GetReceiptValue,
    private val addTransaction: AddTransaction,
    private val observeCategories: ObserveCategories,
) : ViewModel() {
    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()
    private val _scanOcrState = MutableStateFlow<StateUi<Receipt>>(StateUi.Idle)
    val scanOcrState: StateFlow<StateUi<Receipt>> = _scanOcrState.asStateFlow()
    private val _event = MutableStateFlow<ScannerEvent?>(null)
    val event: StateFlow<ScannerEvent?> = _event.asStateFlow()

    fun onCaptured(path: String, context: Context) {
        _state.value = _state.value.copy(phase = ScannerPhase.SCANNING, capturedPath = path)
        viewModelScope.launch {
            delay(1500)
            _state.value = _state.value.copy(phase = ScannerPhase.RESULT)
        }
        scanReceipt(path, context)
    }

    fun retake() {
        _state.value = ScannerUiState()
    }

    fun approve() {
        val result = _scanOcrState.value
        if (result !is StateUi.Success<Receipt>) return
        viewModelScope.launch {
            try {
                val receipt = result.data
                val categories = observeCategories().first()
                val tx = addTransaction(
                    type = TransactionType.EXPENSE,
                    amount = receipt.total,
                    merchant = receipt.merchantName.ifBlank { null },
                    categoryId = categories.defaultExpenseId(),
                    description = receipt.toItemsNote().ifBlank { null },
                    occurredAt = parseReceiptDate(receipt.transactionDate) ?: Clock.System.now(),
                    source = TransactionSource.SCAN,
                )
                addTransaction.store(tx)
                _event.value = ScannerEvent.Saved
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    message = e.message ?: "Failed to save transaction",
                )
            }
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun consumeEvent() {
        _event.value = null
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

    private fun fileToBase64(filePath: String): String {
        val file = File(filePath)
        val bytes = file.readBytes()
        return Base64.encodeBase64URLSafeString(bytes)
    }

    fun scanReceipt(imagePath: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _scanOcrState.value = StateUi.Loading
                val byteImage = File(imagePath).readBytes()
                val compressedImage = ImageCompressor.compress(byteImage)
                val compressedFileTemp = File.createTempFile("receipt_", ".jpeg", context.cacheDir).also {file ->
                    file.writeBytes(compressedImage.bytes)
                }
                val result = getReceiptValue.invoke(
                    fileToBase64(compressedFileTemp.path),
                    "image/jpeg"
                )
                _scanOcrState.value = StateUi.Success(result)
                Logger.d("ScanReceiptViewmodel", "result $result")
            } catch (e: Exception) {
                Logger.d("ScanReceiptViewmodel", "result $e")
                _scanOcrState.value = StateUi.Error(e.message ?: "Error occured")
            }
        }
    }
}
