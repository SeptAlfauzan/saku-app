# OCR, Scanner & Visual Transformation — Test Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add unit + integration + Compose UI tests covering the OCR error-message fix, scanner/gallery change, and `NumberVisualTransformation`.

**Architecture:** Three test layers, all local-JVM: (1) `shared/commonTest` unit + Ktor `MockEngine` integration tests, (2) `androidApp/src/test` pure-JVM unit tests (new source set), (3) `androidApp/src/test` Robolectric + Compose UI tests. One testability-only prod change: `ScannerViewModel` gains an injectable `ocrDispatcher`.

**Tech Stack:** Kotlin 2.4.10, Ktor 3.5.2 (`ktor-client-mock`), kotlin-test, JUnit 4, Robolectric 4.16.1, Compose Multiplatform 1.11.1 (`ui-test-junit4`), Compose material3 1.11.0-alpha07, kotlinx-coroutines-test.

## Global Constraints

- All tests run on local JVM — no device/emulator, no instrumented tests.
- Do not scan/hyphenate new source-set tree names: `androidApp/src/test` (standard AGP layout) and `shared/src/commonTest`.
- Ktor version: `3.5.2`. Compose UI-test artifacts: `org.jetbrains.compose.ui:ui-test-junit4` and `org.jetbrains.compose.ui:ui-test-manifest` at `composeMultiplatform` version `1.11.1`.
- Do not move `NumberVisualTransformation` out of `androidApp`.
- CameraX live-capture path (`hasPermission = true`) is never exercised in UI tests.
- Each commit stages ONLY the files listed in the step. The repo has unrelated uncommitted changes right now — never `git add -A` or `git add .`.
- `NumberVisualTransformation` uses `.` as the thousands separator; invoice check: no behavior change, tests only.

---

### Task 1: Build wiring — version catalog + test dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`
- Modify: `androidApp/build.gradle.kts`

**Interfaces:**
- Consumes: nothing.
- Produces: libs entries `ktor-client-mock`, `compose-ui-testJunit4`, `compose-ui-testManifest` used by later tasks; `androidApp` test source set compiled by `:androidApp:testDebugUnitTest`.

- [ ] **Step 1: Add versions and libraries to `gradle/libs.versions.toml`**

In the `[versions]` block add:
```toml
ktor = "3.5.2"
```
In the `[libraries]` block add:
```toml
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
compose-ui-testJunit4 = { module = "org.jetbrains.compose.ui:ui-test-junit4", version.ref = "composeMultiplatform" }
compose-ui-testManifest = { module = "org.jetbrains.compose.ui:ui-test-manifest", version.ref = "composeMultiplatform" }
```

- [ ] **Step 2: Add MockEngine dependency to `shared/build.gradle.kts`**

In `commonTest.dependencies` block (after `implementation(libs.koin.test)`):
```kotlin
implementation("io.ktor:ktor-client-mock:3.5.2")
```

- [ ] **Step 3: Add unit test options + dependencies to `androidApp/build.gradle.kts`**

In the `android { }` block, after `buildFeatures`:
```kotlin
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
```
In `dependencies { }`, after `implementation(libs.androidx.exifinterface)`:
```kotlin
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.testCore)
    testImplementation(libs.kotlinx.coroutinesTest)
    testImplementation(libs.compose.ui.testJunit4)
    testImplementation(libs.compose.ui.testManifest)
```

- [ ] **Step 4: Verify Gradle resolves deps**

Run: `./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest`
Expected: both tasks complete; existing tests pass; no dependency resolution errors. (Kotlin dependencies may download on first run.)

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml shared/build.gradle.kts androidApp/build.gradle.kts
git commit -m "build: add test dependencies for OCR and scanner test coverage"
```

---

### Task 2: `NetworkRequestTest` — OCR error-message matrix (shared/commonTest)

**Files:**
- Create: `shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/remote/NetworkRequestTest.kt`

**Interfaces:**
- Consumes: `com.septaalfauzan.saku.data.remote.NetworkResult`, `HttpClient.safeRequest<T>` (existing production code in `shared/src/commonMain/.../data/remote/NetworkRequest.kt`).
- Produces: nothing for later tasks; regression coverage of the OCR error-message fix.

- [ ] **Step 1: Write the test file**

```kotlin
package com.septaalfauzan.saku.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Serializable
private data class Dummy(val id: Int, val name: String)

class NetworkRequestTest {

    private fun client(engine: MockEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json()
        }
    }

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun twoHundredWithValidBodyReturnsSuccess() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"id":1,"name":"ok"}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }
        val result = client(engine).safeRequest<Dummy> { get("https://test/api") }
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.id)
    }

    @Test
    fun twoHundredWithUndecodableBodyReturnsSerializationError() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"not_a_dummy":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }
        val result = client(engine).safeRequest<Dummy> { get("https://test/api") }
        assertTrue(result is NetworkResult.Failure)
        assertTrue((result as NetworkResult.Failure).errorMessage.startsWith("Serialization error:"))
    }

    @Test
    fun fourHundredWithErrorJsonReturnsExtractedError() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"error":"bad image"}""",
                status = HttpStatusCode.BadRequest,
                headers = jsonHeaders(),
            )
        }
        val result = client(engine).safeRequest<Dummy> { get("https://test/api") }
        assertEquals("bad image", (result as NetworkResult.Failure).errorMessage)
    }

    @Test
    fun fourHundredWithBlankErrorFallsBackToRawBody() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"error":""}""",
                status = HttpStatusCode.BadRequest,
                headers = jsonHeaders(),
            )
        }
        val result = client(engine).safeRequest<Dummy> { get("https://test/api") }
        assertEquals("""{"error":""}""", (result as NetworkResult.Failure).errorMessage)
    }

    @Test
    fun fourHundredWithNonJsonBodyReturnsRawBody() = runTest {
        val engine = MockEngine {
            respond(
                content = "oops",
                status = HttpStatusCode.BadRequest,
            )
        }
        val result = client(engine).safeRequest<Dummy> { get("https://test/api") }
        assertEquals("oops", (result as NetworkResult.Failure).errorMessage)
    }

    @Test
    fun thrownTransportExceptionBecomesNetworkError() = runTest {
        val engine = MockEngine { throw RuntimeException("boom") }
        val result = client(engine).safeRequest<Dummy> { get("https://test/api") }
        assertEquals("Network error: boom", (result as NetworkResult.Failure).errorMessage)
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :shared:testAndroidHostTest`
Expected: `NetworkRequestTest` 6 tests PASS.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/remote/NetworkRequestTest.kt
git commit -m "test: cover OCR error-message handling in NetworkRequest"
```

---

### Task 3: `ReceiptResponseTest` — paymentMethod null handling + fallbacks (shared/commonTest)

**Files:**
- Create: `shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/remote/response/ReceiptResponseTest.kt`

**Interfaces:**
- Consumes: `com.septaalfauzan.saku.data.remote.response.ReceiptResponse`, `ReceiptData`, `ReceiptData.toDomain()` (existing).
- Produces: nothing for later tasks; regression coverage of the scanner `payment_method` null→`"-"` fix.

- [ ] **Step 1: Write the test file**

```kotlin
package com.septaalfauzan.saku.data.remote.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ReceiptResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun wire(
        paymentMethod: String? = null,
        merchantName: String? = null,
        currency: String? = null,
    ) = """{"data":{
        "merchant_name":${merchantName?.let { "\"$it\"" } ?: "null"},
        "transaction_date":null,
        "transaction_time":null,
        "currency":${currency?.let { "\"$it\"" } ?: "null"},
        "subtotal":null,
        "tax":null,
        "discount":null,
        "total":25000,
        "payment_method":${paymentMethod?.let { "\"$it\"" } ?: "null"},
        "items":[]
    }}"""

    @Test
    fun nullPaymentMethodMapsToDash() {
        val receipt = json.decodeFromString<ReceiptResponse>(wire()).data.toDomain()
        assertEquals("-", receipt.paymentMethod)
    }

    @Test
    fun presentPaymentMethodIsPreserved() {
        val receipt = json.decodeFromString<ReceiptResponse>(wire(paymentMethod = "GoPay")).data.toDomain()
        assertEquals("GoPay", receipt.paymentMethod)
    }

    @Test
    fun nullableFieldsFallBackToDefaults() {
        val receipt = json.decodeFromString<ReceiptResponse>(wire()).data.toDomain()
        assertEquals("", receipt.merchantName)
        assertEquals("", receipt.transactionDate)
        assertEquals("", receipt.transactionTime)
        assertEquals("IDR", receipt.currency)
        assertEquals(0L, receipt.tax)
        assertEquals(0L, receipt.discount)
        assertEquals(null, receipt.subtotal)
    }

    @Test
    fun roundTripPreservesValues() {
        val data = ReceiptData(
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
        val decoded = json.decodeFromString<ReceiptData>(json.encodeToString(ReceiptData.serializer(), data))
        assertEquals(data, decoded)
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :shared:testAndroidHostTest`
Expected: `ReceiptResponseTest` 4 tests PASS.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/remote/response/ReceiptResponseTest.kt
git commit -m "test: cover payment method fallback in ReceiptResponse mapping"
```

---

### Task 4: `OcrRepositoryIntegrationTest` — full OCR HTTP pipeline (shared/commonTest)

**Files:**
- Create: `shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/repository/OcrRepositoryIntegrationTest.kt`

**Interfaces:**
- Consumes: `ApiServiceImpl(HttpClient, String)`, `RemoteOcrRepository(ApiService)` (existing).
- Produces: nothing for later tasks; integration proof of receipt JSON → domain mapping + error surfacing end-to-end.

- [ ] **Step 1: Write the test file**

```kotlin
package com.septaalfauzan.saku.data.repository

import com.septaalfauzan.saku.data.remote.ApiServiceImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OcrRepositoryIntegrationTest {

    private fun repositoryReturning(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): RemoteOcrRepository {
        val engine = MockEngine {
            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json() }
        }
        return RemoteOcrRepository(ApiServiceImpl(client, "https://test"))
    }

    private fun receiptJson(paymentMethod: String? = null) = """{"data":{
        "merchant_name":"Alfamart",
        "transaction_date":"16-09-2026",
        "transaction_time":"10:00",
        "currency":"IDR",
        "subtotal":10000,
        "tax":0,
        "discount":500,
        "total":9500,
        "payment_method":${paymentMethod?.let { "\"$it\"" } ?: "null"},
        "items":[{"name":"Nasi","quantity":1,"unit_price":10000,"total_price":10000}]
    }}"""

    @Test
    fun successWithoutPaymentMethodYieldsDash() = runTest {
        val repo = repositoryReturning(receiptJson())
        val receipt = repo.getOcrReceiptValue("abc", "image/jpeg")
        assertEquals(9500L, receipt.total)
        assertEquals("Alfamart", receipt.merchantName)
        assertEquals("-", receipt.paymentMethod)
        assertEquals(1, receipt.items.size)
    }

    @Test
    fun successWithPaymentMethodPreservesIt() = runTest {
        val repo = repositoryReturning(receiptJson(paymentMethod = "GoPay"))
        val receipt = repo.getOcrReceiptValue("abc", "image/jpeg")
        assertEquals("GoPay", receipt.paymentMethod)
    }

    @Test
    fun errorResponseSurfacesExtractedMessage() = runTest {
        val repo = repositoryReturning(
            content = """{"error":"bad image"}""",
            status = HttpStatusCode.BadRequest,
        )
        val e = assertFailsWith<Exception> { repo.getOcrReceiptValue("abc", "image/jpeg") }
        assertEquals("bad image", e.message)
    }

    @Test
    fun errorResponseSurfacesRawBodyWhenNotJson() = runTest {
        val repo = repositoryReturning(
            content = "oops",
            status = HttpStatusCode.BadRequest,
        )
        val e = assertFailsWith<Exception> { repo.getOcrReceiptValue("abc", "image/jpeg") }
        assertEquals("oops", e.message)
    }

    @Test
    fun transportFailureSurfacesNetworkError() = runTest {
        val engine = MockEngine { throw RuntimeException("boom") }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json() }
        }
        val repo = RemoteOcrRepository(ApiServiceImpl(client, "https://test"))
        val e = assertFailsWith<Exception> { repo.getOcrReceiptValue("abc", "image/jpeg") }
        assertEquals("Network error: boom", e.message)
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :shared:testAndroidHostTest`
Expected: `OcrRepositoryIntegrationTest` 5 tests PASS.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/repository/OcrRepositoryIntegrationTest.kt
git commit -m "test: add OCR HTTP pipeline integration test"
```

---

### Task 5: `ScannerViewModel` — injectable OCR dispatcher (prod change)

**Files:**
- Modify: `androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerViewModel.kt`

**Interfaces:**
- Consumes: existing constructor params.
- Produces: `ScannerViewModel(getReceiptValue, addTransaction, observeCategories, ocrDispatcher: CoroutineDispatcher = Dispatchers.IO)` — later tasks construct with a `TestDispatcher`.

- [ ] **Step 1: Add imports**

In `androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerViewModel.kt` add:
```kotlin
import kotlinx.coroutines.CoroutineDispatcher
```

- [ ] **Step 2: Add constructor param**

Change the class declaration to:
```kotlin
class ScannerViewModel(
    private val getReceiptValue: GetReceiptValue,
    private val addTransaction: AddTransaction,
    private val observeCategories: ObserveCategories,
    private val ocrDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
```

- [ ] **Step 3: Use the dispatcher in scanReceipt**

Change `fun scanReceipt(imagePath: String, context: Context) {` body's launch from `viewModelScope.launch(Dispatchers.IO)` to `viewModelScope.launch(ocrDispatcher)`.

- [ ] **Step 4: Verify app still compiles**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (default param keeps existing call sites and DI valid).

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerViewModel.kt
git commit -m "refactor: injectable OCR dispatcher in ScannerViewModel"
```

---

### Task 6: Test utilities + `ScannerViewModelTest` (androidApp/src/test)

**Files:**
- Create: `androidApp/src/test/kotlin/com/septaalfauzan/saku/testutil/FakeRepositories.kt`
- Create: `androidApp/src/test/kotlin/com/septaalfauzan/saku/testutil/MainDispatcherRule.kt`
- Create: `androidApp/src/test/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerViewModelTest.kt`

**Interfaces:**
- Consumes: `ScannerViewModel` with `ocrDispatcher` param (Task 5), `OcrRepository`, `TransactionRepository`, `Receipt`, `Category`, `AddEditUiState`, `ScannerPhase`, `ScannerEvent`, `StateUi`.
- Produces: `FakeOcrRepository`, `FakeTransactionRepository`, `MainDispatcherRule` — reused by UI-test tasks.

- [ ] **Step 1: Write `FakeRepositories.kt`**

```kotlin
package com.septaalfauzan.saku.testutil

import com.septaalfauzan.saku.domain.importexport.ApplyResult
import com.septaalfauzan.saku.domain.importexport.UndoSnapshot
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.DuplicateKey
import com.septaalfauzan.saku.domain.model.Receipt
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.repository.OcrRepository
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeOcrRepository : OcrRepository {
    var result: Receipt? = null
    var error: Exception? = null

    override suspend fun getOcrReceiptValue(base64Image: String, imageType: String): Receipt {
        error?.let { throw it }
        return result ?: error("no OCR result configured")
    }
}

class FakeTransactionRepository(
    var categories: List<Category> = emptyList(),
) : TransactionRepository {
    val inserted = mutableListOf<Transaction>()

    override fun observeTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
    override fun observeCategories(): Flow<List<Category>> = flowOf(categories)
    override suspend fun insert(transaction: Transaction) { inserted += transaction }
    override suspend fun update(transaction: Transaction) {}
    override suspend fun delete(id: String) {}
    override fun observePending(): Flow<List<Transaction>> = flowOf(emptyList())
    override suspend fun setStatus(id: String, status: TransactionStatus) {}
    override suspend fun findRecentDuplicate(
        key: DuplicateKey,
        withinStartMillis: Long,
        withinEndMillis: Long,
    ): Transaction? = null
    override suspend fun getAll(): List<Transaction> = inserted
    override suspend fun applyImport(changes: List<Transaction>): ApplyResult =
        ApplyResult(0, 0, UndoSnapshot(emptyList(), emptyMap()))
    override suspend fun undoImport(snapshot: UndoSnapshot) {}
}
```

- [ ] **Step 2: Write `MainDispatcherRule.kt`**

```kotlin
package com.septaalfauzan.saku.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

- [ ] **Step 3: Write `ScannerViewModelTest.kt`**

```kotlin
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

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
            kotlinx.serialization.json.Json.encodeToString(
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
```

Note: `ROBOLECTRIC` quirk — if `ImageCompressor.compress` throws under the Robolectric shadow of `BitmapFactory.decodeByteArray` (returns null for arbitrary bytes), replace `ByteArray(128)` in `stubImageFile` with Base64-decoded bytes of a 1x1 PNG:
`"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="`
via `java.util.Base64.getDecoder().decode(...)`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :androidApp:testDebugUnitTest --tests "com.septaalfauzan.saku.ui.scanner.ScannerViewModelTest"`
Expected: `ScannerViewModelTest` 9 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/test/kotlin/com/septaalfauzan/saku/testutil/ androidApp/src/test/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerViewModelTest.kt
git commit -m "test: cover ScannerViewModel OCR flow"
```

---

### Task 7: `NumberVisualTransformationTest` — pure JVM unit tests (androidApp/src/test)

**Files:**
- Create: `androidApp/src/test/kotlin/com/septaalfauzan/saku/ui/components/NumberVisualTransformationTest.kt`

**Interfaces:**
- Consumes: `com.septaalfauzan.saku.ui.components.NumberVisualTransformation` (existing).
- Produces: nothing for later tasks.

- [ ] **Step 1: Write the test file**

```kotlin
package com.septaalfauzan.saku.ui.components

import androidx.compose.ui.text.AnnotatedString
import kotlin.test.Test
import kotlin.test.assertEquals

class NumberVisualTransformationTest {

    private val sut = NumberVisualTransformation()

    private fun format(input: String): String =
        sut.filter(AnnotatedString(input)).text.text

    @Test
    fun groupsDigitsWithDotSeparator() {
        assertEquals("0", format("0"))
        assertEquals("123", format("123"))
        assertEquals("1.234", format("1234"))
        assertEquals("12.345", format("12345"))
        assertEquals("1.234.567", format("1234567"))
    }

    @Test
    fun stripsNonDigitCharacters() {
        assertEquals("12.34", format("12ab34"))
        assertEquals("1.234", format("1a2b3c4"))
    }

    @Test
    fun emptyInputProducesEmptyOutput() {
        assertEquals("", format(""))
    }

    @Test
    fun offsetMappingRoundTrips() {
        val result = sut.filter(AnnotatedString("1234"))

        assertEquals(5, result.offsetMapping.originalToTransformed(4))
        assertEquals(4, result.offsetMapping.transformedToOriginal(5))

        for (offset in 0..4) {
            val transformed = result.offsetMapping.originalToTransformed(offset)
            assertEquals(offset, result.offsetMapping.transformedToOriginal(transformed))
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :androidApp:testDebugUnitTest --tests "com.septaalfauzan.saku.ui.components.NumberVisualTransformationTest"`
Expected: `NumberVisualTransformationTest` 4 tests PASS.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/test/kotlin/com/septaalfauzan/saku/ui/components/NumberVisualTransformationTest.kt
git commit -m "test: cover NumberVisualTransformation formatting and offset mapping"
```

---

### Task 8: `ScannerResultContent` test tags + Compose UI test (androidApp/src/test)

**Files:**
- Modify: `androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerResultContent.kt`
- Create: `androidApp/src/test/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerResultContentTest.kt`

**Interfaces:**
- Consumes: `ScannerResultContent(capturedPath, scanOcrState, onApprove, onEdit, onRetake)`, `SakuTheme`, `StateUi`, `Receipt`.
- Produces: nothing for later tasks.

- [ ] **Step 1: Add test tags to `ScannerResultContent.kt`**

Add import:
```kotlin
import androidx.compose.ui.platform.testTag
```
Add `Modifier.testTag("approve_button")` to the approve `PillButton`, `Modifier.testTag("rescan_button")` to the rescan `PillButton` (the bottom full-width one).

- [ ] **Step 2: Write the test file**

```kotlin
package com.septaalfauzan.saku.ui.scanner

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.septaalfauzan.saku.domain.model.Receipt
import com.septaalfauzan.saku.domain.model.ReceiptItem
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import com.septaalfauzan.saku.ui.state.StateUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScannerResultContentTest {

    @get:Rule
    val compose = createComposeRule()

    private fun sampleReceipt(paymentMethod: String = "-") = Receipt(
        merchantName = "Alfamart",
        transactionDate = "16-09-2026",
        transactionTime = "10:00",
        currency = "IDR",
        subtotal = 10000,
        tax = 0,
        discount = 500,
        total = 9500,
        paymentMethod = paymentMethod,
        items = listOf(ReceiptItem("Nasi", 1, 10000, 10000)),
    )

    private fun setContent(
        state: StateUi<Receipt>,
        onApprove: () -> Unit = {},
        onEdit: () -> Unit = {},
        onRetake: () -> Unit = {},
    ) {
        compose.setContent {
            SakuTheme {
                ScannerResultContent(
                    capturedPath = null,
                    scanOcrState = state,
                    onApprove = onApprove,
                    onEdit = onEdit,
                    onRetake = onRetake,
                )
            }
        }
    }

    @Test
    fun successStateRendersReceiptFields() {
        setContent(StateUi.Success(sampleReceipt()))
        compose.onNodeWithText("Alfamart").assertExists()
        compose.onNodeWithText("16-09-2026").assertExists()
        compose.onNodeWithText("-").assertExists()
        compose.onNodeWithText("Rp. 9.500").assertExists()
    }

    @Test
    fun successStateShowsPaymentMethodFallbackDash() {
        setContent(StateUi.Success(sampleReceipt(paymentMethod = "-")))
        compose.onNodeWithText("-").assertExists()
    }

    @Test
    fun errorStateRendersMessageAndDisablesButtons() {
        setContent(StateUi.Error("bad image"))
        compose.onNodeWithText("bad image").assertExists()
        compose.onNodeWithTag("approve_button").assertIsNotEnabled()
        compose.onNodeWithTag("rescan_button").assertIsNotEnabled()
    }

    @Test
    fun loadingStateShowsProcessingAndDisablesButtons() {
        setContent(StateUi.Loading)
        compose.onNodeWithText("Memproses Struk").assertExists()
        compose.onNodeWithTag("approve_button").assertIsNotEnabled()
        compose.onNodeWithTag("rescan_button").assertIsNotEnabled()
    }

    @Test
    fun approveAndRetakeCallbacksFire() {
        var approved = false
        var retaken = false
        setContent(
            StateUi.Success(sampleReceipt()),
            onApprove = { approved = true },
            onRetake = { retaken = true },
        )
        compose.onNodeWithTag("approve_button").assertIsEnabled().performClick()
        compose.onNodeWithText("Pindai Ulang").performClick()
        org.junit.Assert.assertTrue(approved)
        org.junit.Assert.assertTrue(retaken)
    }
}
```

Note: `PillButton` may render disabled state visually but `assertIsNotEnabled` reads the clickable node's `enabled` semantics — if the disabled assertion fails because `PillButton` sets `enabled = false` only on its internal surface, wrap the assertion in `compose.onAllNodesWithTag("approve_button")` with `useUnmergedTree` and assert against the merged semantics of the `Modifier.testTag` node. If that fails, move the `testTag` onto the `PillButton`-internal `Surface` by adding a `modifier` slot — see check below.

- [ ] **Step 3: Run tests to verify they pass**

Run: `./gradlew :androidApp:testDebugUnitTest --tests "com.septaalfauzan.saku.ui.scanner.ScannerResultContentTest"`
Expected: `ScannerResultContentTest` 5 tests PASS.

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerResultContent.kt androidApp/src/test/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerResultContentTest.kt
git commit -m "test: cover ScannerResultContent states and callbacks"
```

---

### Task 9: `ScannerViewfinderContent` test tags + Compose UI test (androidApp/src/test)

**Files:**
- Modify: `androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerViewfinderContent.kt`
- Create: `androidApp/src/test/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerViewfinderContentTest.kt`

**Interfaces:**
- Consumes: `ScannerViewfinderContent(hasPermission, flashMode, shutterToken, scanning, onCaptured, onGrantPermission, onShutter, onPickImage)`.
- Produces: nothing for later tasks.

- [ ] **Step 1: Add test tags to `ScannerViewfinderContent.kt`**

Add import:
```kotlin
import androidx.compose.ui.platform.testTag
```
- In `ShutterButtons`, add `Modifier.testTag("shutter_button")` to the outer shutter `Box` (the one with `.clickable(enabled = !scanning, onClick = onClick)`).
- Add `Modifier.testTag("gallery_button")` to the pick-image `IconButton`.

- [ ] **Step 2: Write the test file**

```kotlin
package com.septaalfauzan.saku.ui.scanner

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScannerViewfinderContentTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setContent(
        hasPermission: Boolean,
        scanning: Boolean,
        onGrantPermission: () -> Unit = {},
        onShutter: () -> Unit = {},
        onPickImage: () -> Unit = {},
    ) {
        compose.setContent {
            SakuTheme {
                ScannerViewfinderContent(
                    hasPermission = hasPermission,
                    flashMode = ScannerFlash.OFF,
                    shutterToken = 0,
                    scanning = scanning,
                    onCaptured = {},
                    onGrantPermission = onGrantPermission,
                    onShutter = onShutter,
                    onPickImage = onPickImage,
                )
            }
        }
    }

    @Test
    fun permissionPromptShownWhenNoPermission() {
        setContent(hasPermission = false, scanning = false)
        compose.onNodeWithText("Izin kamera diperlukan").assertExists()
        compose.onNodeWithText("Beri Izin").assertExists()
    }

    @Test
    fun grantButtonInvokesCallback() {
        var granted = false
        setContent(hasPermission = false, scanning = false, onGrantPermission = { granted = true })
        compose.onNodeWithText("Beri Izin").performClick()
        assertTrue(granted)
    }

    @Test
    fun galleryButtonInvokesPickImage() {
        var picked = false
        setContent(hasPermission = false, scanning = false, onPickImage = { picked = true })
        compose.onNodeWithTag("gallery_button").performClick()
        assertTrue(picked)
    }

    @Test
    fun shutterDisabledWhileScanning() {
        setContent(hasPermission = false, scanning = true)
        compose.onNodeWithTag("shutter_button").assertIsNotEnabled()
    }

    @Test
    fun shutterInvokesCallbackWhenNotScanning() {
        var shutter = 0
        setContent(hasPermission = false, scanning = false, onShutter = { shutter++ })
        compose.onNodeWithTag("shutter_button").performClick()
        assertTrue(shutter == 1)
    }
}
```

Note: never set `hasPermission = true` here — that branch initializes CameraX `ProcessCameraProvider` which is unsupported under Robolectric.

- [ ] **Step 3: Run tests to verify they pass**

Run: `./gradlew :androidApp:testDebugUnitTest --tests "com.septaalfauzan.saku.ui.scanner.ScannerViewfinderContentTest"`
Expected: `ScannerViewfinderContentTest` 5 tests PASS.

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerViewfinderContent.kt androidApp/src/test/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerViewfinderContentTest.kt
git commit -m "test: cover ScannerViewfinderContent permission and shutter states"
```

---

### Task 10: `NumberVisualTransformationUiTest` — TextField wiring (androidApp/src/test)

**Files:**
- Create: `androidApp/src/test/kotlin/com/septaalfauzan/saku/ui/components/NumberVisualTransformationUiTest.kt`

**Interfaces:**
- Consumes: `NumberVisualTransformation`, material3 `TextField`.
- Produces: nothing for later tasks.

- [ ] **Step 1: Write the test file**

```kotlin
package com.septaalfauzan.saku.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.KeyboardType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NumberVisualTransformationUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun textFieldRendersThousandsSeparator() {
        val value = mutableStateOf("")
        compose.setContent {
            TextField(
                value = value.value,
                onValueChange = { value.value = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = NumberVisualTransformation(),
            )
        }
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("1234")
        field.assertTextContains("1.234")
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :androidApp:testDebugUnitTest --tests "com.septaalfauzan.saku.ui.components.NumberVisualTransformationUiTest"`
Expected: `NumberVisualTransformationUiTest` 1 test PASS.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/test/kotlin/com/septaalfauzan/saku/ui/components/NumberVisualTransformationUiTest.kt
git commit -m "test: verify NumberVisualTransformation in editable text field"
```

---

### Task 11: Full verification

**Files:**
- None (verification only).

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest`
Expected: all tasks PASS — 16 commonTest + androidHostTest existing, 4 pure-JVM (shared task 7) + 9 VM (task 6) + 5 result + 5 viewfinder + 1 UI wiring (task 10).

- [ ] **Step 2: Sanity check no unrelated files were touched**

Run: `git status --short`
Expected: only test/build files listed in this plan are committed; pre-existing user WIP files remain as they were (untouched).