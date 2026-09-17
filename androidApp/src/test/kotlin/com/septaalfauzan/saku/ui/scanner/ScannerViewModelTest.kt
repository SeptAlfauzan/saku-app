package com.septaalfauzan.saku.ui.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.septaalfauzan.saku.domain.model.AddEditUiState
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.Receipt
import com.septaalfauzan.saku.domain.model.ReceiptItem
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.repository.TransactionRepository
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
import kotlin.test.assertTrue

private const val AUTHORITY_MISSING = "com.septaalfauzan.saku.missing"

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
        tx: TransactionRepository,
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

    private class NullAssetFileProvider : android.content.ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: android.net.Uri,
            projection: Array<String>?,
            selection: String?,
            selectionArgs: Array<String>?,
            sortOrder: String?,
        ): android.database.Cursor? = null

        override fun getType(uri: android.net.Uri): String? = null

        override fun insert(uri: android.net.Uri, values: android.content.ContentValues?): android.net.Uri? = null

        override fun delete(
            uri: android.net.Uri,
            selection: String?,
            selectionArgs: Array<String>?,
        ): Int = 0

        override fun update(
            uri: android.net.Uri,
            values: android.content.ContentValues?,
            selection: String?,
            selectionArgs: Array<String>?,
        ): Int = 0

        override fun openFile(uri: android.net.Uri, mode: String): android.os.ParcelFileDescriptor? = null
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
    fun approveFailureSetsMessageAndConsumeMessageClears() = runTest(mainRule.dispatcher.scheduler) {
        val ocr = FakeOcrRepository().apply { result = sampleReceipt() }
        val tx = object : TransactionRepository by FakeTransactionRepository(categories = sampleCategories()) {
            override suspend fun insert(transaction: Transaction) {
                throw Exception("db down")
            }
        }
        val vm = buildViewModel(ocr, tx)

        vm.scanReceipt(stubImageFile(), context)
        advanceTimeBy(1)
        runCurrent()
        advanceUntilIdle()
        vm.approve()
        advanceUntilIdle()

        assertEquals("db down", vm.state.value.message)

        vm.consumeMessage()
        assertNull(vm.state.value.message)
    }

    @Test
    fun approveSuccessEmitsSavedAndConsumeEventClears() = runTest(mainRule.dispatcher.scheduler) {
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

        vm.consumeEvent()
        assertNull(vm.event.value)
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

    @Test
    fun onCapturedUriCopiesBytesToCacheAndAdvancesPhase() = runTest(mainRule.dispatcher.scheduler) {
        val ocr = FakeOcrRepository().apply { result = sampleReceipt() }
        val tx = FakeTransactionRepository(categories = sampleCategories())
        val vm = buildViewModel(ocr, tx)

        val source = java.io.File(context.cacheDir, "shared_source.jpg")
        source.writeBytes(ByteArray(128) { 0x42 })
        val uri = android.net.Uri.fromFile(source)

        vm.onCaptured(uri, context)

        val scanningState = vm.state.value
        assertEquals(ScannerPhase.SCANNING, scanningState.phase)
        val copied = java.io.File(scanningState.capturedPath!!)
        assertTrue(
            copied.path.startsWith(java.io.File(context.cacheDir, "shared_image_").path),
            "temp copy lives in cacheDir under shared_image_ prefix",
        )
        org.junit.Assert.assertArrayEquals(source.readBytes(), copied.readBytes())

        advanceTimeBy(1500)
        runCurrent()
        assertEquals(ScannerPhase.RESULT, vm.state.value.phase)

        advanceUntilIdle()
        assertIs<StateUi.Success<Receipt>>(vm.scanOcrState.value)
    }

    @Test
    fun onCapturedUnresolvableUriSurfacesMessage() = runTest(mainRule.dispatcher.scheduler) {
        val ocr = FakeOcrRepository()
        val tx = FakeTransactionRepository(categories = sampleCategories())
        val vm = buildViewModel(ocr, tx)

        // Robolectric's shadow returns an UnregisteredInputStream (throws on read) for an
        // unregistered content:// authority instead of a null stream, so register a provider
        // that yields no asset file; prod's openInputStream then resolves to null and the
        // "Unable to open URI" handling under test fires exactly as on device.
        val provider = NullAssetFileProvider()
        provider.attachInfo(
            context,
            android.content.pm.ProviderInfo().apply {
                authority = AUTHORITY_MISSING
                packageName = context.packageName
                name = NullAssetFileProvider::class.java.name
            },
        )
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal(
            AUTHORITY_MISSING,
            provider,
        )
        val uri = android.net.Uri.parse("content://$AUTHORITY_MISSING/images/1")

        vm.onCaptured(uri, context)

        val state = vm.state.value
        assertEquals(ScannerPhase.VIEWFINDER, state.phase)
        assertEquals(
            "Unable to open URI: content://com.septaalfauzan.saku.missing/images/1",
            state.message,
        )
        assertNull(
            state.capturedPath,
            "no capture phase when the shared image cannot be opened",
        )
    }
}
