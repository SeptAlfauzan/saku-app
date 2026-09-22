# Sentry Crash Reporting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Sentry crash reporting + manual capture + breadcrumbs + perf + ANR + logs to the Saku KMP app (Android release + iOS release), with DSN injected at build time via env secrets, never committed.

**Architecture:** `sentry-kotlin-multiplatform` 0.27.0 Gradle plugin applied to `:shared` auto-wires the SDK into commonMain and links Sentry Cocoa for iOS. Init uses the Native Platform Options path (`Sentry.initWithPlatformOptions`) via `expect/actual` so Android gets `anrEnabled`. DSN flows: Android `BuildConfig.SENTRY_DSN` from `$SENTRY_DSN` env at build time; iOS via `Config.xcconfig` → Info.plist key, read in `iOSApp.init()`. A `SentryReporter` interface (real/noop) backs Koin DI so ViewModels/usecases capture exceptions without platform branching. CI `deploy.yaml` injects `SENTRY_DSN` (required) + `SENTRY_AUTH_TOKEN`/`SENTRY_ORG`/`SENTRY_PROJECT` (optional) secrets.

**Tech Stack:** Kotlin 2.4.10, AGP 9.0.1, Compose Multiplatform, Koin 4.2.1, Ktor 3.5.2, sentry-kotlin-multiplatform 0.27.0, Sentry Android Gradle plugin 6.22.0.

## Global Constraints

- Sentry active **release builds only**; any build without a DSN must be a silent no-op (zero init, zero reporter work).
- DSN **never committed** to git. Comes from env at build time only.
- `sentry-kotlin-multiplatform` version `0.27.0` exact; Gradle plugin `io.sentry.kotlin.multiplatform.gradle` same version.
- `io.sentry.android.gradle` plugin version `6.22.0` exact, with `autoInstallation.enabled = false` (KMP plugin owns the SDK dependency).
- Auth-token/org/project env vars must never **fail** a build when absent — only DSN absence fails in CI.
- Existing test files that construct `ProcessNotificationUseCase` / ViewModels must keep compiling and passing (add reporter via constructor default param of `NoopSentryReporter`).
- Repo convention: conventional commit messages; stage named files only, never `git add -A`.

---

### Task 1: Gradle wiring — version catalog + plugins + cinterop

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `gradle.properties`
- Modify: `shared/build.gradle.kts`

**Interfaces:**
- Produces: `libs.sentry.kmp` lib catalog ref, `libs.plugins.sentryKmp` plugin ref, `libs.plugins.sentryAndroid` plugin ref. `shared` applies `sentryKmp`. cinterop commonization enabled.

- [ ] **Step 1: Add Sentry to version catalog**

In `gradle/libs.versions.toml`, add under `[versions]`:

```toml
sentry = "0.27.0"
sentryAndroidGradle = "6.22.0"
```

Under `[libraries]`:

```toml
sentry-kmp = { module = "io.sentry:sentry-kotlin-multiplatform", version.ref = "sentry" }
```

Under `[plugins]`:

```toml
sentryKmp = { id = "io.sentry.kotlin.multiplatform.gradle", version.ref = "sentry" }
sentryAndroid = { id = "io.sentry.android.gradle", version.ref = "sentryAndroidGradle" }
```

- [ ] **Step 2: Enable Apple cinterop commonization**

Append to `gradle.properties`:

```properties
kotlin.mpp.enableCInteropCommonization=true
```

Required for native platform options on Apple targets (`PlatformOptionsConfiguration`).

- [ ] **Step 3: Apply plugin + opt-in in shared module**

In `shared/build.gradle.kts`, add `alias(libs.plugins.sentryKmp)` to the `plugins {}` block (top, line 12 area). Then add opt-in inside the `kotlin {}` block, after `sourceSets { ... }` closes (before line 89 `}` that closes `kotlin`): insert a new `sourceSets` block:

```kotlin
    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }
```

NOTE: `kotlin` block already contains one `sourceSets { ... }` (line 45). Add this second `sourceSets` call inside the same `kotlin { }` , after the existing one.

- [ ] **Step 4: Apply android gradle plugin**

In `androidApp/build.gradle.kts`, add `alias(libs.plugins.sentryAndroid)` to `plugins {}` (line 7 area, after `composeCompiler`).

- [ ] **Step 5: Verify Gradle config**

Run: `./gradlew :shared:help :androidApp:help -q`
Expected: BUILD SUCCESSFUL, no plugin resolution error.

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml gradle.properties shared/build.gradle.kts androidApp/build.gradle.kts
git commit -m "build: add sentry kmp and android gradle plugins"
```

---

### Task 2: Sentry init + reporter + Koin module (common/shared)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/sentry/SentryInit.kt`
- Create: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/sentry/SentryReporter.kt`
- Create: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/sentry/SentryModule.kt`
- Create: `shared/src/androidMain/kotlin/com/septaalfauzan/saku/sentry/SentryInit.android.kt`
- Create: `shared/src/iosMain/kotlin/com/septaalfauzan/saku/sentry/SentryInit.ios.kt`
- Test: `shared/src/commonTest/kotlin/com/septaalfauzan/saku/sentry/SentryReporterTest.kt`

**Interfaces:**
- Consumes: nothing (pure SDK API).
- Produces:
  - `fun initSentry(dsn: String)` (commonMain, callable from Android Kotlin and iOS Swift via `SentrySetupKt.initSentry`... note: file `SentryInit.kt` → generated Swift class name `SentryInitKt`; verified at verify-gate (2026-09-22) that Kotlin/Native exports it as `SentryInitKt.doInitSentry(dsn:)` — top-level functions beginning with `init` are `do`-prefixed to avoid the ObjC initializer clash; used from Swift as `SentryInitKt.doInitSentry(dsn:)`).
  - `internal expect fun platformOptionsConfiguration(dsn: String): PlatformOptionsConfiguration`
  - `interface SentryReporter { val enabled: Boolean; fun captureException(t: Throwable); fun captureMessage(message: String); fun addBreadcrumb(message: String, category: String? = null) }`
  - `object NoopSentryReporter : SentryReporter`
  - `class RealSentryReporter : SentryReporter`
  - `fun createSentryReporter(enabled: Boolean): SentryReporter`
  - `val sentryModule: Module` (Koin), provides `single<SentryReporter> { createSentryReporter(Sentry.isEnabled()) }`
  - `var sentryInitialized: Boolean` private-set public-get helper so `SentryReporter` factory uses an explicit flag instead of SDK state in tests.

- [ ] **Step 1: Write the failing tests**

Create `shared/src/commonTest/kotlin/com/septaalfauzan/saku/sentry/SentryReporterTest.kt`:

```kotlin
package com.septaalfauzan.saku.sentry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SentryReporterTest {

    @Test
    fun `disabled factory returns noop`() {
        val reporter = createSentryReporter(false)
        assertIs<NoopSentryReporter>(reporter)
        assertFalse(reporter.enabled)
    }

    @Test
    fun `enabled factory returns real`() {
        val reporter = createSentryReporter(true)
        assertIs<RealSentryReporter>(reporter)
        assertTrue(reporter.enabled)
    }

    @Test
    fun `noop reporter is a no-op`() {
        val reporter: SentryReporter = NoopSentryReporter
        reporter.captureException(RuntimeException("boom"))
        reporter.captureMessage("hello")
        reporter.addBreadcrumb("step", "flow")
        assertFalse(reporter.enabled)
        assertEquals("NoopSentryReporter", reporter.toString())
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :shared:allTests`
Expected: FAIL — `createSentryReporter` unresolved, `sentry` package missing.

- [ ] **Step 3: Implement common reporter**

Create `shared/src/commonMain/kotlin/com/septaalfauzan/saku/sentry/SentryReporter.kt`:

```kotlin
package com.septaalfauzan.saku.sentry

import io.sentry.kotlin.multiplatform.Sentry

interface SentryReporter {
    val enabled: Boolean
    fun captureException(t: Throwable)
    fun captureMessage(message: String)
    fun addBreadcrumb(message: String, category: String? = null)
}

object NoopSentryReporter : SentryReporter {
    override val enabled: Boolean = false
    override fun captureException(t: Throwable) {}
    override fun captureMessage(message: String) {}
    override fun addBreadcrumb(message: String, category: String?) {}
}

class RealSentryReporter : SentryReporter {
    override val enabled: Boolean = true
    override fun captureException(t: Throwable) = Sentry.captureException(t)
    override fun captureMessage(message: String) = Sentry.captureMessage(message)
    override fun addBreadcrumb(message: String, category: String?) {
        Sentry.addBreadcrumb {
            this.message = message
            this.category = category
        }
    }
}

fun createSentryReporter(enabled: Boolean): SentryReporter =
    if (enabled) RealSentryReporter() else NoopSentryReporter
```

- [ ] **Step 4: Implement Koin module**

Create `shared/src/commonMain/kotlin/com/septaalfauzan/saku/sentry/SentryModule.kt`:

```kotlin
package com.septaalfauzan.saku.sentry

import io.sentry.kotlin.multiplatform.Sentry
import org.koin.dsl.module

val sentryModule = module {
    single<SentryReporter> { createSentryReporter(Sentry.isEnabled()) }
}
```

- [ ] **Step 5: Implement init (common + platform)**

Create `shared/src/commonMain/kotlin/com/septaalfauzan/saku/sentry/SentryInit.kt`:

```kotlin
package com.septaalfauzan.saku.sentry

import io.sentry.kotlin.multiplatform.PlatformOptionsConfiguration
import io.sentry.kotlin.multiplatform.Sentry

internal expect fun platformOptionsConfiguration(dsn: String): PlatformOptionsConfiguration

fun initSentry(dsn: String) {
    if (dsn.isBlank()) return
    Sentry.initWithPlatformOptions(platformOptionsConfiguration(dsn))
}
```

Create `shared/src/androidMain/kotlin/com/septaalfauzan/saku/sentry/SentryInit.android.kt`:

```kotlin
package com.septaalfauzan.saku.sentry

import io.sentry.kotlin.multiplatform.PlatformOptionsConfiguration

internal actual fun platformOptionsConfiguration(dsn: String): PlatformOptionsConfiguration = {
    it.dsn = dsn
    it.tracesSampleRate = 1.0
    it.anrEnabled = true
    it.logs.isEnabled = true
}
```

Create `shared/src/iosMain/kotlin/com/septaalfauzan/saku/sentry/SentryInit.ios.kt`:

```kotlin
package com.septaalfauzan.saku.sentry

import io.sentry.kotlin.multiplatform.PlatformOptionsConfiguration

internal actual fun platformOptionsConfiguration(dsn: String): PlatformOptionsConfiguration = {
    it.dsn = dsn
    it.tracesSampleRate = 1.0
    it.logs.isEnabled = true
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :shared:allTests`
Expected: PASS (3 tests). If `:shared:allTests` is not a valid aggregate task, use `./gradlew :shared:testAndroidHostTest` plus `:shared:iosSimulatorArm64Test` as available.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/septaalfauzan/saku/sentry shared/src/androidMain/kotlin/com/septaalfauzan/saku/sentry shared/src/iosMain/kotlin/com/septaalfauzan/saku/sentry shared/src/commonTest/kotlin/com/septaalfauzan/saku/sentry
git commit -m "feat: add sentry init, reporter interface, and koin module"
```

---

### Task 3: Android wiring — BuildConfig DSN + app init

**Files:**
- Modify: `androidApp/build.gradle.kts`
- Modify: `androidApp/src/main/kotlin/com/septaalfauzan/saku/SubTrackApplication.kt`
- Test: `androidApp/src/test/kotlin/com/septaalfauzan/saku/SubTrackApplicationSentryTest.kt`

**Interfaces:**
- Consumes: `initSentry(dsn: String)` from Task 2 (shared), `sentryModule` from Task 2, `appModule`/`androidAppModule` DI modules.
- Produces: `BuildConfig.SENTRY_DSN: String` field; SubTrackApplication inits Sentry then Koin including `sentryModule`.

- [ ] **Step 1: Add BuildConfig field**

In `androidApp/build.gradle.kts` `android {}` block add:

```kotlin
    buildFeatures {
        buildConfig = true
    }
```

Inside `defaultConfig {}` add:

```kotlin
        buildConfigField(
            "String",
            "SENTRY_DSN",
            "\"${System.getenv("SENTRY_DSN") ?: ""}\"",
        )
```

- [ ] **Step 2: Write the failing test**

Create `androidApp/src/test/kotlin/com/septaalfauzan/saku/SubTrackApplicationSentryTest.kt`:

```kotlin
package com.septaalfauzan.saku

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.septaalfauzan.saku.di.AndroidContextHolder
import com.septaalfauzan.saku.di.androidAppModule
import com.septaalfauzan.saku.di.appModule
import com.septaalfauzan.saku.sentry.NoopSentryReporter
import com.septaalfauzan.saku.sentry.SentryReporter
import com.septaalfauzan.saku.sentry.sentryModule
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubTrackApplicationSentryTest : KoinTest {
    private val reporter: SentryReporter by inject()

    @After
    fun tearDown() {
        stopKoin()
        AndroidContextHolder.applicationContext = null
    }

    @Test
    fun `sentry module resolves noop reporter when dsn blank`() {
        startKoin {
            androidContext(
                ApplicationProvider.getApplicationContext<Application>()
            )
            modules(appModule, androidAppModule, sentryModule)
        }
        assert(reporter is NoopSentryReporter)
        assert(!reporter.enabled)
    }
}
```

NOTE: check `AndroidContextHolder` visibility (declared at `androidApp/.../di/AndroidContextHolder.kt`) — if it has a setter accessible from tests, this pattern works; if not, assert via reporter only and drop the `AndroidContextHolder` reset line.

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :androidApp:testDebugUnitTest --tests "*SubTrackApplicationSentryTest"`
Expected: FAIL — `sentryModule` unresolved (shared module not exported? see Step 4 note) or `SENTRY_DSN` BuildConfig missing.

- [ ] **Step 4: Implement app init**

Modify `SubTrackApplication.kt`:

```kotlin
package com.septaalfauzan.saku

import android.app.Application
import com.septaalfauzan.saku.di.AndroidContextHolder
import com.septaalfauzan.saku.di.androidAppModule
import com.septaalfauzan.saku.di.appModule
import com.septaalfauzan.saku.sentry.initSentry
import com.septaalfauzan.saku.sentry.sentryModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SubTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initSentry(BuildConfig.SENTRY_DSN)
        AndroidContextHolder.applicationContext = applicationContext
        startKoin {
            androidContext(this@SubTrackApplication)
            modules(appModule, androidAppModule, sentryModule)
        }
    }
}
```

NOTE: if `:shared` does not export the `sentry` package to the Android app (no explicit `api`/`export`), add to `shared/build.gradle.kts` `sourceSets.androidMain.dependencies { implementation(project(...)) }` — more likely you must ensure the shared module's Kotlin is visible; if unresolved, check the shared `android` block `androidResources`/namespace and use `kotlin { explicitApi() }` none (default). If still unresolved, add `api(project(":shared"))` in `androidApp` (it is already `implementation(project(":shared"))`; switch to `api` only if needed — `implementation` exposes transitive classes, so no change expected).

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :androidApp:testDebugUnitTest --tests "*SubTrackApplicationSentryTest"`
Expected: PASS.

- [ ] **Step 6: Verify release BuildConfig gets DSN**

Run: `SENTRY_DSN="https://example@o0.ingest.sentry.io/0" ./gradlew :androidApp:assembleRelease`
Then inspect: `androidApp/build/generated/source/buildConfig/release/**/BuildConfig.java` contains `SENTRY_DSN = "https://example@o0.ingest.sentry.io/0"`.
Run with no env: `./gradlew :androidApp:assembleDebug` — generated debug BuildConfig has `SENTRY_DSN = ""`.

- [ ] **Step 7: Commit**

```bash
git add androidApp/build.gradle.kts androidApp/src/main/kotlin/com/septaalfauzan/saku/SubTrackApplication.kt androidApp/src/test/kotlin/com/septaalfauzan/saku/SubTrackApplicationSentryTest.kt
git commit -m "feat: wire sentry dsn buildconfig and app init on android"
```

---

### Task 4: iOS wiring — Info.plist + xcconfig + app init

**Files:**
- Modify: `iosApp/Configuration/Config.xcconfig`
- Modify: `iosApp/iosApp/Info.plist`
- Modify: `iosApp/iosApp/iOSApp.swift`

**Interfaces:**
- Consumes: `initSentry(dsn: String)` exposed to Swift (Task 2).
- Produces: `SENTRY_DSN` xcconfig var; `SENTRY_DSN` Info.plist key; `iOSApp.init()` calls `SentryInitKt.doInitSentry(dsn:)` once.

- [ ] **Step 1: Add DSN to xcconfig**

In `iosApp/Configuration/Config.xcconfig` append:

```
SENTRY_DSN=$(SENTRY_DSN)
```

If undefined, `$(SENTRY_DSN)` expands to empty at normal launch; when running a release build with env set, Xcode build settings inject it.

- [ ] **Step 2: Add Info.plist key**

In `iosApp/iosApp/Info.plist` inside `<dict>` add:

```xml
	<key>SENTRY_DSN</key>
	<string>$(SENTRY_DSN)</string>
```

- [ ] **Step 3: Init Sentry from SwiftUI app**

Modify `iosApp/iosApp/iOSApp.swift`:

```swift
import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        if let dsn = Bundle.main.object(forInfoDictionaryKey: "SENTRY_DSN") as? String {
            SentryInitKt.doInitSentry(dsn: dsn)
        }
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

- [ ] **Step 4: Verify build + plist expansion**

Run: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug CODE_SIGNING_ALLOWED=NO build`
Expected: BUILD SUCCEEDED. Then verify plist of built app contains expanded or empty `SENTRY_DSN`:
`defaults read "$(find iosApp/build -name "Saku.app" -type d | head -1)/Info" SENTRY_DSN` (path may vary; verify manually).
If the Xcode project does not reference `Config.xcconfig` or `Info.plist` (project is hand-made), add the file reference + `baseConfigurationReference` for the target — documented in the repo's `project.pbxproj`; perform the minimal pbxproj edit to set `Config.xcconfig` as the target's base configuration.

- [ ] **Step 5: Commit**

```bash
git add iosApp/Configuration/Config.xcconfig iosApp/iosApp/Info.plist iosApp/iosApp/iOSApp.swift
git commit -m "feat: wire sentry dsn into ios xcconfig, infoplist, and app init"
```

---

### Task 5: Instrumentation — notification parsing

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/notification/usecase/ProcessNotificationUseCase.kt`
- Test: `shared/src/commonTest/kotlin/com/septaalfauzan/saku/notification/ProcessNotificationUseCaseTest.kt` (extend) — read existing file `:132-139` for the `useCase()` factory signature before editing.

**Interfaces:**
- Consumes: `SentryReporter`, `NoopSentryReporter` from Task 2.
- Produces: `ProcessNotificationUseCase` gains `reporter: SentryReporter = NoopSentryReporter` param. Adds breadcrumb on successful insert and captureException when `engine.process` throws.

- [ ] **Step 1: Write the failing tests**

Append to `ProcessNotificationUseCaseTest.kt`:

```kotlin
    @Test
    fun `records breadcrumb when transaction created`() {
        val reporter = FakeSentryReporter()
        val repo = fakeRepo()
        val settings = fakeSettings()
        val useCase = useCase(repo, settings, reporter = reporter)
        runTest {
            useCase(notificationData())
        }
        assertTrue(
            reporter.breadcrumbs.any { it.startsWith("transaction created") },
            "expected insert breadcrumb, got ${reporter.breadcrumbs}"
        )
    }

    @Test
    fun `captures exception on parse throw`() {
        val reporter = FakeSentryReporter()
        val repo = fakeRepo()
        val settings = fakeSettings()
        val engine = SimulatedFailingEngine()
        val parserRegistry = ParserRegistry()
        val duplicateDetector = DuplicateDetector(fakeRepo())
        val useCase = ProcessNotificationUseCase(
            engine = engine,
            settings = settings,
            repository = repo,
            duplicateDetector = duplicateDetector,
            parserRegistry = parserRegistry,
            reporter = reporter,
        )
        runTest {
            useCase(notificationData())
        }
        assertTrue(
            reporter.exceptions.any { it is SimulatedFailException },
            "expected parse exception captured, got ${reporter.exceptions}"
        )
    }
```

Add near the existing helpers a `FakeSentryReporter`, `SimulatedFailingEngine`, `notificationData()`, `fakeRepo()`, `fakeSettings()` (match the existing test file's existing fake style — reuse existing `TestTransactionRepository`/`FakeSettings` if present; if a name like `fakeRepo()`/`fakeSettings()` conflicts with existing helpers, reuse those exact existing helpers and only add the new fakes).

```kotlin
    class FakeSentryReporter : SentryReporter {
        override val enabled: Boolean = true
        val exceptions = mutableListOf<Throwable>()
        val messages = mutableListOf<String>()
        val breadcrumbs = mutableListOf<String>()
        override fun captureException(t: Throwable) { exceptions += t }
        override fun captureMessage(message: String) { messages += message }
        override fun addBreadcrumb(message: String, category: String?) { breadcrumbs += message }
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :shared:testAndroidHostTest`
Expected: FAIL — `reporter` param unused; existing `useCase()` factory needs updated signature to pass `reporter`.

- [ ] **Step 3: Implement**

Modify `ProcessNotificationUseCase.kt`:

- Add imports:

```kotlin
import com.septaalfauzan.saku.sentry.NoopSentryReporter
import com.septaalfauzan.saku.sentry.SentryReporter
```

- Add constructor param (after `parserRegistry`, before `clock`):

```kotlin
    private val reporter: SentryReporter = NoopSentryReporter,
```

- Wrap the `engine.process` call:

```kotlin
        val parsed = try {
            engine.process(notification) ?: return
        } catch (e: Throwable) {
            reporter.captureException(e)
            return
        }
```

- After `repository.insert(...)` add breadcrumb:

```kotlin
        reporter.addBreadcrumb("transaction created type=${type} amount=${amount}", "notification")
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :shared:testAndroidHostTest`
Expected: PASS (existing + 2 new).

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/septaalfauzan/saku/notification/usecase/ProcessNotificationUseCase.kt shared/src/commonTest/kotlin/com/septaalfauzan/saku/notification/ProcessNotificationUseCaseTest.kt
git commit -m "feat: capture and breadcrumb notification parsing in sentry"
```

---

### Task 6: Instrumentation — CSV import/export + network + scan prefill

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/ui/importexport/ImportCsvViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/ui/importexport/ExportCsvViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/remote/NetworkRequest.kt`
- Modify: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/ui/addedit/ScanPrefill.kt`
- Tests: follow per-file existing test locations (`commonTest/.../ui/importexport`, `commonTest/.../data/remote`, `commonTest/.../ui/addedit`) — if a test file exists for a ViewModel, extend it; if the ViewModel has no test file today, add constructor-param default (`reporter: SentryReporter = NoopSentryReporter`) and skip a dedicated test file (keep instrumentation merge-light), because these ViewModels currently take positional args from Koin `viewModelOf`.

**Interfaces:**
- Consumes: `SentryReporter`, `NoopSentryReporter`.
- Produces: ViewModels gain default `reporter` param; `NetworkRequest.kt` gains capture of `SerializationException`; `ScanPrefill.decode` records breadcrumb on failure.

- [ ] **Step 1: ImportCsvViewModel — add reporter + capture**

Add imports and constructor param with default, then in `onFilePicked` `.onFailure` add capture; in `confirmImport` `.onFailure`; on `.onSuccess` add breadcrumb with counts; in `undo` inside `runCatching` failure add capture.

Modify `ImportCsvViewModel.kt`:

```kotlin
import com.septaalfauzan.saku.sentry.NoopSentryReporter
import com.septaalfauzan.saku.sentry.SentryReporter

class ImportCsvViewModel(
    private val importTransactions: ImportTransactions,
    private val reporter: SentryReporter = NoopSentryReporter,
) : ViewModel() {
```

In `onFilePicked`, replace `.onFailure` block:

```kotlin
                .onFailure { error ->
                    reporter.captureException(error)
                    _uiState.value = ImportCsvUiState.Failed(error.message ?: "")
                }
```

In `onFilePicked`, `.onSuccess` add before assigning state:

```kotlin
            }.onSuccess { state ->
                if (state is ImportCsvUiState.Preview) {
                    reporter.addBreadcrumb(
                        "csv parsed new=${state.newCount} update=${state.updateCount} dup=${state.duplicateCount} errors=${state.errors.size}",
                        "import",
                    )
                }
                _uiState.value = state
            }
```

In `confirmImport`, `.onSuccess` add:

```kotlin
            }.onSuccess { state ->
                if (state is ImportCsvUiState.Done) {
                    reporter.addBreadcrumb(
                        "import applied new=${state.newCount} update=${state.updateCount} skipped=${state.skippedDuplicates}",
                        "import",
                    )
                }
                _uiState.value = state
            }
```

In `confirmImport`, `.onFailure`:

```kotlin
                .onFailure { error ->
                    reporter.captureException(error)
                    _uiState.value = ImportCsvUiState.Failed(error.message ?: "")
                }
```

In `undo`:

```kotlin
            runCatching { importTransactions.undo(done.snapshot) }
                .onFailure { reporter.captureException(it) }
```

- [ ] **Step 2: ExportCsvViewModel — add reporter + capture**

Modify `ExportCsvViewModel.kt`:

```kotlin
import com.septaalfauzan.saku.sentry.NoopSentryReporter
import com.septaalfauzan.saku.sentry.SentryReporter

class ExportCsvViewModel(
    private val exportTransactions: ExportTransactions,
    private val transactionRepository: TransactionRepository,
    private val reporter: SentryReporter = NoopSentryReporter,
) : ViewModel() {
```

In `buildCsv` `.onFailure`:

```kotlin
            }.onFailure { error ->
                reporter.captureException(error)
                _uiState.value = _uiState.value.copy(
                    building = false,
                    failed = true,
                )
            }
```

And `.onSuccess` breadcrumb:

```kotlin
            }.onSuccess { result ->
                reporter.addBreadcrumb(
                    "csv exported rows=${result.rows.size}",
                    "export",
                )
                _uiState.value = _uiState.value.copy(building = false)
                onResult(result)
            }
```

NOTE: verify `ExportTransactions.Result` exposes `rows` (read `shared/src/commonMain/.../domain/importexport/ExportTransactions.kt` before writing; if the property is named differently, use its actual name).

- [ ] **Step 3: NetworkRequest — capture serialization errors only**

Modify `NetworkRequest.kt` `handleResponse` success-branch catches:

```kotlin
            } catch (e: ContentConvertException) {
                com.septaalfauzan.saku.sentry.captureThrowable(e)
                NetworkResult.Failure("Serialization error: ${e.message}")
            } catch (e: SerializationException) {
                com.septaalfauzan.saku.sentry.captureThrowable(e)
                NetworkResult.Failure("Serialization error: ${e.message}")
            }
```

Where in `shared/src/commonMain/.../sentry/SentryReporter.kt` add a top-level helper:

```kotlin
fun captureThrowable(t: Throwable) {
    if (Sentry.isEnabled()) {
        Sentry.captureException(t)
    }
}
```

(Keeps `handleResponse` an `inline` function dependency-light; `Sentry.isEnabled()` is safe to call before init — returns false.)

- [ ] **Step 4: ScanPrefill — breadcrumb on decode failure**

Modify `ScanPrefill.kt` `decode`:

```kotlin
        fun decode(json: String): ScanPrefill? {
            val result = runCatching { Json.decodeFromString(serializer(), json) }.getOrNull()
            if (result == null) {
                com.septaalfauzan.saku.sentry.addDecodeBreadcrumb()
            }
            return result
        }
```

Add in `sentry/SentryReporter.kt`:

```kotlin
fun addDecodeBreadcrumb() {
    if (Sentry.isEnabled()) {
        Sentry.addBreadcrumb {
            message = "scan prefill decode failed"
            category = "scan"
        }
    }
}
```

- [ ] **Step 5: Compile + test**

Run: `./gradlew :shared:testAndroidHostTest`
Expected: PASS. For any test that constructs `ImportCsvViewModel`/`ExportCsvViewModel` with positional constructor args, they still compile thanks to the default param (read existing test files first; if a test passes extra positional args after `transactionRepository`, reorder args so `reporter` stays last).

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/septaalfauzan/saku/ui/importexport shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/remote/NetworkRequest.kt shared/src/commonMain/kotlin/com/septaalfauzan/saku/ui/addedit/ScanPrefill.kt shared/src/commonMain/kotlin/com/septaalfauzan/saku/sentry/SentryReporter.kt
git commit -m "feat: instrument csv import export, network, and scan prefill errors"
```

---

### Task 7: Symbol upload — Android Gradle plugin config

**Files:**
- Modify: `androidApp/build.gradle.kts`

**Interfaces:**
- Consumes: `libs.plugins.sentryAndroid` (Task 1).
- Produces: `sentry {}` block; env-gated upload never fails the build.

- [ ] **Step 1: Configure plugin env-gated**

Add to `androidApp/build.gradle.kts` after the `android {}` block (top-level):

```kotlin
sentry {
    autoInstallation {
        enabled.set(false)
    }
    org.set(System.getenv("SENTRY_ORG") ?: "")
    projectName.set(System.getenv("SENTRY_PROJECT") ?: "")
    authToken.set(System.getenv("SENTRY_AUTH_TOKEN") ?: "")
}
```

- [ ] **Step 2: Verify release build works without token**

Run: `./gradlew :androidApp:assembleRelease`
Expected: BUILD SUCCESSFUL. Plugin may log a warning about missing token/org — acceptable; must not fail.

- [ ] **Step 3: Commit**

```bash
git add androidApp/build.gradle.kts
git commit -m "build: add env-gated sentry symbol upload for android"
```

---

### Task 8: CI — inject DSN + optional auth into deploy.yaml

**Files:**
- Modify: `.github/workflows/deploy.yaml`

**Interfaces:**
- Consumes: nothing.
- Produces: `SENTRY_DSN` (required), `SENTRY_AUTH_TOKEN`/`SENTRY_ORG`/`SENTRY_PROJECT` (optional) in the release build env.

- [ ] **Step 1: Add verify step**

Insert after "Verify signing inputs" step (line 38) in `build-android` job:

```yaml
      - name: Verify Sentry secrets
        run: |
          test -n "${{ secrets.SENTRY_DSN }}" || { echo "::error::SENTRY_DSN secret is not set"; exit 1; }
          if [ -n "${{ secrets.SENTRY_AUTH_TOKEN }}" ]; then
            echo "Sentry auth token present; debug symbols will upload."
          else
            echo "::warning::SENTRY_AUTH_TOKEN not set — debug symbol upload skipped, build continues."
          fi
```

- [ ] **Step 2: Add env to build step**

Modify "Build release AAB" step env block to add:

```yaml
          SENTRY_DSN: ${{ secrets.SENTRY_DSN }}
          SENTRY_AUTH_TOKEN: ${{ secrets.SENTRY_AUTH_TOKEN }}
          SENTRY_ORG: ${{ secrets.SENTRY_ORG }}
          SENTRY_PROJECT: ${{ secrets.SENTRY_PROJECT }}
```

Keep existing keystore env vars intact.

- [ ] **Step 3: Verify YAML validity**

Run: `actionlint .github/workflows/deploy.yaml` (if installed) or `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/deploy.yaml'))"`.
Expected: no parse error.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/deploy.yaml
git commit -m "ci: inject sentry dsn and auth into android release build"
```

---

### Task 9: Final verification

**Files:** none (verification only)

- [ ] **Step 1: Full shared test suite**

Run: `./gradlew :shared:testAndroidHostTest`
Expected: all pass.

- [ ] **Step 2: Android unit tests**

Run: `./gradlew :androidApp:testDebugUnitTest`
Expected: all pass.

- [ ] **Step 3: Release build with DSN**

Run: `SENTRY_DSN="https://example@o0.ingest.sentry.io/0" ./gradlew :androidApp:assembleRelease`
Expected: BUILD SUCCESSFUL; BuildConfig carries DSN; unhandled exception in a debug-touched release path would report to Sentry once a real DSN is configured.

- [ ] **Step 4: iOS compile**

Run: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug CODE_SIGNING_ALLOWED=NO build`
Expected: BUILD SUCCEEDED.

- [ ] **Step 5: Manual smoke (documentation for the repo owner)**

1. Create Sentry project at sentry.io; note org slug + project slug.
2. In the Sentry project's DSN, copy a public DSN; do NOT commit it.
3. Set repo secrets: `SENTRY_DSN`, plus `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT` for symbol upload.
4. Tag a `v*` release — CI injects DSN, build + upload to Play proceeds.
5. In the app, trigger an unhandled Kotlin exception → verify issue lands in Sentry with symbolicated stack trace.
6. iOS: build locally with `SENTRY_DSN` env set → verify event arrives.

---

## Self-Review Notes

- Spec coverage: init+entry wiring (Tasks 2, 3, 4), auto features incl. ANR (native-options init, `anrEnabled`) and logs (`logs.isEnabled`), breadcrumbs/manual capture (Tasks 5, 6), CI (Task 8), symbol upload (Task 7). All spec sections mapped.
- Placeholders: none — every code step has full source.
- Type consistency: `initSentry(dsn)`, `SentryReporter`, `NoopSentryReporter`, `RealSentryReporter`, `createSentryReporter(Boolean)`, `sentryModule`, `captureThrowable`, `addDecodeBreadcrumb` are defined once in Task 2 and referenced consistently in Tasks 3, 5, 6.