# OCR, Scanner/Gallery & Visual Transformation — Test Coverage Design

**Date:** 2026-09-16

## Context

Three recent feature/fix changes landed without test coverage:

1. **OCR error-message fix** (`shared/.../data/remote/NetworkRequest.kt`) — non-2xx responses now decode `ErrorResponse.error`, falling back to the raw body when the field is blank or the body is not JSON. Also `safeRequest` wraps thrown exceptions as `"Network error: ..."`.
2. **Scanner/gallery change** — `ReceiptResponse.paymentMethod` is now nullable; `ReceiptData.toDomain()` maps `null` to `"-"`. Scanner screen split into `ScannerScreen`, `ScannerResultContent`, `ScannerViewfinderContent`; gallery image pick flow added; OCR VM flow connects capture/gallery → compression → OCR API → `StateUi<Receipt>` → save.
3. **Visual transformation** (`NumberVisualTransformation`) — thousands separator (`.` every 3 digits) with offset mapping, wired into the Add/Edit amount field.

Goal: add unit and integration test coverage for all three. Full stack agreed: pure-logic units + OCR HTTP integration + ViewModel + Compose UI tests, all local-JVM (no device/emulator).

## Test Strategy

All tests run on the local JVM:

- `shared/commonTest` — pure Kotlin + Ktor `MockEngine` HTTP tests (run via `:shared:testAndroidHostTest`).
- `androidApp/src/test` (new source set) — Robolectric for `Context`/`File`/`Bitmap` shadows, Compose UI rule for composable rendering.

No instrumented/device tests. CameraX live-camera path is excluded from UI tests (see "Out of scope").

## Production Changes (minimal, testability-only)

- `ScannerViewModel`: add constructor parameter `ocrDispatcher: CoroutineDispatcher = Dispatchers.IO` and use it for the `scanReceipt` launch (currently hardcoded `Dispatchers.IO`). Behavior-neutral; keeps default identical, allows deterministic test dispatching.

No other production code changes. `NumberVisualTransformation` stays in androidApp (no move to shared).

## New Tests

### shared/commonTest

**`data/remote/NetworkRequestTest.kt`** (unit, MockEngine)
Coverage matrix for `safeRequest`/`handleResponse`:
- 2xx + valid body → `Success`.
- 2xx + body that fails deserialization → `Failure("Serialization error: ...")`.
- 4xx + `{"error": "msg"}` → `Failure("msg")`.
- 4xx + `{"error": ""}` → `Failure(raw body)`.
- 4xx + non-JSON body → `Failure(raw body)`.
- transport exception → `Failure("Network error: ...")`.

**`data/remote/response/ReceiptResponseTest.kt`** (unit)
- Decode JSON without `payment_method` → `toDomain().paymentMethod == "-"`.
- Decode with `payment_method` present → value preserved.
- Nullable fallbacks: `merchantName`/dates → `""`, `currency` → `"IDR"`, `tax`/`discount` → `0L`.
- Serialization round-trip of `ReceiptData`.

**`data/repository/OcrRepositoryIntegrationTest.kt`** (integration, MockEngine)
Full HTTP pipeline `ApiServiceImpl` → `handleResponse` → `ReceiptData.toDomain` → `RemoteOcrRepository`:
- 200 + valid receipt JSON without `payment_method` → `Receipt.paymentMethod == "-"`.
- 200 + `payment_method` present → preserved.
- 400 + `{"error": "..."}` → thrown `Exception` with the message.
- 400 + non-JSON body → thrown `Exception` with raw body.
- transport failure → thrown `Exception` with `"Network error: ..."`.

### androidApp/src/test

Shared fakes in `com/septaalfauzan/saku/testutil/`:
- `FakeOcrRepository` — configurable `Receipt` return / injected throw.
- `FakeTransactionRepository` — in-memory insert capture, `observeCategories` emits fixture categories.

**`ui/ScannerViewModelTest.kt`** (Robolectric + coroutines `MainDispatcherRule`)
- `onCaptured` → phase `SCANNING` immediately, `capturedPath` set; after 1500 ms virtual advance → `RESULT`.
- `scanReceipt` success → OCR state `Loading` → `Success(receipt)`.
- `scanReceipt` failure → `Loading` → `Error(message)`.
- `approve` on `Success` → stores `Transaction` with `source = SCAN`, emits `ScannerEvent.Saved`, correct merchant/amount/category.
- `approve` when not `Success` → no-op (no store, no event).
- `retake` → state resets to defaults.
- `toggleFlash` → `OFF → AUTO → ON → OFF`.
- `updateStateFromEditValue`: valid JSON updates Success receipt; invalid JSON no-op.
- `consumeMessage`/`consumeEvent` null out.

**`ui/components/NumberVisualTransformationTest.kt`** (pure JVM, no Robolectric)
- Formatting: `0→"0"`, `123→"123"`, `1234→"1.234"`, `12345→"12.345"`, `1234567→"1.234.567"`.
- Non-digit stripping: `"12ab34"` → `"12.34"`.
- Empty input → empty output.
- Offset mapping: `originalToTransformed`/`transformedToOriginal` round-trip for boundary and mid offsets.

**`ui/ScannerResultContentTest.kt`** (Compose UI test under Robolectric)
- `Success` renders merchant, date, payment method (incl. null → `"-"`), items note, discount, total.
- Approve/Edit/Retake buttons enabled on `Success`; callbacks fire.
- `Error` renders message (OCR error fix visible); buttons disabled.
- `Loading`/`Idle` render processing indicator; buttons disabled.
- Uses `capturedPath = null` to skip bitmap decode (no real image).

**`ui/ScannerViewfinderContentTest.kt`** (Compose UI test under Robolectric)
- `hasPermission = false` → permission title + grant button; tapping invokes `onGrantPermission`.
- Gallery button invokes `onPickImage`.
- `scanning = true` → shutter click disabled; `scanning = false` → `onShutter` invoked.
- Only `hasPermission = false` branch exercised (avoids CameraX initialization in the true branch).

**`ui/components/NumberVisualTransformationUiTest.kt`** (Compose UI test)
- TextField configured with `NumberVisualTransformation` renders `1.234` for input `1234`; proves transformation wires into an editable field without cursor crash.

## Build Wiring

**`gradle/libs.versions.toml`:**
- version `ktor = "3.5.2"`.
- libraries: `ktor-client-mock`, `compose-ui-testJunit4`, `compose-ui-testManifest`.

**`shared/build.gradle.kts`:** `commonTest.dependencies` += `ktor-client-mock`.

**`androidApp/build.gradle.kts`:**
- `testOptions { unitTests { isIncludeAndroidResources = true; isReturnDefaultValues = true } }`.
- `testImplementation`: `kotlin-test`, `junit`, `robolectric`, `androidx-testCore`, `compose-ui-testJunit4`, `compose-ui-testManifest`.

## Verification

- `./gradlew :shared:testAndroidHostTest`
- `./gradlew :androidApp:testDebugUnitTest`
- Watch for compose `ui-test-junit4` 1.11.1 vs material3 1.11.0-alpha07 skew; if conflict, fall back to `androidx.compose.ui:ui-test-junit4` with a matching BOM.

## Out of Scope

- Live CameraX capture path (native init, cannot run under Robolectric).
- Device/instrumented compose tests (`withDeviceTestBuilder` remains unused).
- Moving `NumberVisualTransformation` to `shared`.
- Refactoring `ScannerResultContent`/`ScannerViewfinderContent` API — tested as-is.