package com.septaalfauzan.saku.ui.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.septaalfauzan.saku.domain.model.AddEditUiState
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.Receipt
import com.septaalfauzan.saku.domain.model.ReceiptItem
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.usecase.AddTransaction
import com.septaalfauzan.saku.domain.usecase.GetReceiptValue
import com.septaalfauzan.saku.domain.usecase.ObserveCategories
import com.septaalfauzan.saku.testutil.FakeOcrRepository
import com.septaalfauzan.saku.testutil.FakeTransactionRepository
import com.septaalfauzan.saku.testutil.MainDispatcherRule
import com.septaalfauzan.saku.ui.state.StateUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ScannerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun sampleReceipt() = Receipt(
        merchantName = "Alfamart",
        transactionDate = "16-09-2026",
        transactionTime = "10:00",
        currency = "IDR",
        subtotal = 10000,
        tax = 0,
        discount = 500,
        total = 9500,
        paymentMethod = "-",
        items = listOf(ReceiptItem("Nasi", 1, 10000, 10000)),
    )

    private fun sampleCategories() = listOf(
        Category("food", "Makanan", "restaurant", TransactionType.EXPENSE),
        Category("other_expense", "Lainnya", "receipt", TransactionType.EXPENSE),
    )

    private fun buildViewModel(
        ocr: FakeOcrRepository,
        tx: FakeTransactionRepository,
    ): ScannerViewModel = ScannerViewModel(
        GetReceiptValue(ocr),
        AddTransaction(tx),
        ObserveCategories(tx),
        mainRule.dispatcher,
    )

    private fun stubImageFile(name: String = "receipt_test.jpg"): String {
        val file = java.io.File(context.cacheDir, name)
        if (!file.exists()) file.writeBytes(ByteArray(128))
        return file.absolutePath
    }

    @Test
    fun onCapturedAdvancesPhaseToScanningThenResult() = runTest(mainRule.dispatcher.scheduler) {
        val ocr = FakeOcrRepository().apply { result = sampleReceipt() }
        val tx = FakeTransactionRepository(categories = sampleCategories())
        val vm = buildViewModel(ocr, tx)
        val path = stubImageFile()

        vm.onCaptured(path, context)

        assertEquals(ScannerPhase.SCANNING, vm.state.value.phase)
        assertEquals(path, vm.state.value.capturedPath)

        advanceTimeBy(1500)
        runCurrent()

        assertEquals(ScannerPhase.RESULT, vm.state.value.phase)
    }

    @Test
    fun scanReceiptSuccessReachesSuccessState() = runTest(mainRule.dispatcher.scheduler) {
        val ocr = FakeOcrRepository().apply { result = sampleReceipt() }
        val tx = FakeTransactionRepository(categories = sampleCategories())
        val vm = buildViewModel(ocr, tx)

        vm.scanReceipt(stubImageFile(), context)
        advanceTimeBy(1)
        runCurrent()
        advanceUntilIdle()

        val state = vm.scanOcrState.value
        assertIs<StateUi.Success<Receipt>>(state)
        assertEquals("Alfamart", state.data.merchantName)
        assertEquals("-", state.data.paymentMethod)
    }

    @Test
    fun scanReceiptFailureReachesErrorState() = runTest(mainRule.dispatcher.scheduler) {
        val ocr = FakeOcrRepository().apply { error = Exception("bad image") }
        val tx = FakeTransactionRepository(categories = sampleCategories())
        val vm = buildViewModel(ocr, tx)

        vm.scanReceipt(stubImageFile(), context)
        advanceTimeBy(1)
        runCurrent()
        advanceUntilIdle()

        val state = vm.scanOcrState.value
        assertEquals("bad image", (state as StateUi.Error).message)
    }

    @Test
    fun approveStoresScannedTransactionAndEmitsSaved() = runTest(mainRule.dispatcher.scheduler) {
        val ocr = FakeOcrRepository().apply { result = sampleReceipt() }
        val tx = FakeTransactionRepository(categories = sampleCategories())
        val vm = buildViewModel(ocr, tx)

        vm.scanReceipt(stubImageFile(), context)
        advanceTimeBy(1)
        runCurrent()
        advanceUntilIdle()
        vm.approve()
        advanceUntilIdle()

        assertEquals(ScannerEvent.Saved, vm.event.value)
        val stored = tx.inserted.single()
        assertEquals(TransactionType.EXPENSE, stored.type)
        assertEquals(9500L, stored.amount)
        assertEquals("Alfamart", stored.merchant)
        assertEquals(TransactionSource.SCAN, stored.source)
        assertEquals("other_expense", stored.categoryId)
    }

    @Test
    fun approveWithoutSuccessStateIsNoOp() = runTest(mainRule.dispatcher.scheduler) {
        val ocr = FakeOcrRepository()
        val tx = FakeTransactionRepository(categories = sampleCategories())
        val vm = buildViewModel(ocr, tx)

        vm.approve()
        advanceUntilIdle()

        assertNull(vm.event.value)
        assertEquals(0, tx.inserted.size)
    }

    @Test
    fun retakeResetsState() = runTest(mainRule.dispatcher.scheduler) {
        val ocr = FakeOcrRepository().apply { result = sampleReceipt() }
        val tx = FakeTransactionRepository(categories = sampleCategories())
        val vm = buildViewModel(ocr, tx)
        vm.onCaptured(stubImageFile(), context)
        vm.retake()

        assertEquals(ScannerUiState(), vm.state.value)
    }

    @Test
    fun toggleFlashCyclesAutoOnOff() = runTest(mainRule.dispatcher.scheduler) {
        val ocr = FakeOcrRepository()
        val tx = FakeTransactionRepository(categories = sampleCategories())
        val vm = buildViewModel(ocr, tx)

        assertEquals(ScannerFlash.OFF, vm.state.value.flash)
        vm.toggleFlash()
        assertEquals(ScannerFlash.AUTO, vm.state.value.flash)
        vm.toggleFlash()
        assertEquals(ScannerFlash.ON, vm.state.value.flash)
        vm.toggleFlash()
        assertEquals(ScannerFlash.OFF, vm.state.value.flash)
    }

    @Test
    fun updateStateFromEditValueUpdatesSuccessReceipt() = runTest(mainRule.dispatcher.scheduler) {
        val ocr = FakeOcrRepository().apply { result = sampleReceipt() }
        val tx = FakeTransactionRepository(categories = sampleCategories())
        val vm = buildViewModel(ocr, tx)

        vm.scanReceipt(stubImageFile(), context)
        advanceTimeBy(1)
        runCurrent()
        advanceUntilIdle()

        val edit = AddEditUiState(
            merchant = "Indomaret",
            amountInput = "24500",
            note = "belanja",
            categoryId = "food",
        )
        vm.updateStateFromEditValue(
            Json.encodeToString(
                AddEditUiState.serializer(),
                edit,
            ),
        )

        val state = vm.scanOcrState.value
        assertIs<StateUi.Success<Receipt>>(state)
        assertEquals("Indomaret", state.data.merchantName)
        assertEquals(24500L, state.data.total)
        assertEquals("belanja", state.data.note)
        assertEquals("food", state.data.categoryId)
    }

    @Test
    fun updateStateFromEditValueWithInvalidJsonIsNoOp() = runTest(mainRule.dispatcher.scheduler) {
        val ocr = FakeOcrRepository()
        val tx = FakeTransactionRepository(categories = sampleCategories())
        val vm = buildViewModel(ocr, tx)

        vm.updateStateFromEditValue("not json")
        // must not crash; state remains Idle
        assertIs<StateUi.Idle>(vm.scanOcrState.value)
    }
}
