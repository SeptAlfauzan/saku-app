# Sentry Crash Reporting — Design

**Date:** 2026-09-21
**Status:** Approved

## Context

Saku (Compose Multiplatform: `androidApp` + `iosApp` + `shared`) has no crash reporting. Add Sentry via `sentry-kotlin-multiplatform` 0.27.0 (latest, Jun 2026; Gradle plugin `io.sentry.kotlin.multiplatform.gradle`). Full coverage selected: crashes + unhandled exceptions, manual capture, breadcrumbs, perf tracing, Android ANR, logs.

DSN injected at build/init time — never committed. CI injects via GitHub Actions secrets.

## Architecture

- `shared/commonMain` — `SentryInit.kt`: `initSentry(dsn: String)`; blank dsn → no-op. Configures dsn, `tracesSampleRate = 1.0`, `logs.enabled = true`, `beforeSend` hook (PII scrub point).
- `shared/*/main` — `expect configureNativeOptions`; Android: ANR + VM tracing; iOS: Cocoa passthrough.
- `SentryReporter` facade (commonMain) behind interface — active impl + `noop` impl chosen by init result.
- Koin `SentryModule` — DI wiring; ViewModels resolve reporter.
- Platform entry: Android `SubTrackApplication.onCreate` `initSentry(BuildConfig.SENTRY_DSN)` before `startKoin`. iOS `iOSApp` init reads Info.plist key → `initSentry`.
- No-commit secrets: Android `buildConfigField` from `$SENTRY_DSN` env; iOS via `Config.xcconfig` → Info.plist.

## Build config

- `libs.versions.toml`: `sentry = "0.27.0"`, plugin + lib entries.
- `shared`: apply Sentry KMP plugin (auto-adds SDK to commonMain, Cocoa/SPM link).
- `androidApp`: `buildFeatures { buildConfig = true }`, `buildConfigField("String", "SENTRY_DSN", ...)` from env, empty fallback.
- iOS: `SENTRY_DSN` in `Config.xcconfig` (env fallback empty), Info.plist key, read via `Bundle`.

## Instrumentation

- Automatic: Kotlin uncaught, native crashes, lifecycle/UI breadcrumbs, ANR (Android), perf traces.
- Manual (via `SentryReporter`):
  - init breadcrumb
  - `ProcessNotificationUseCase` parse failures + per-notification breadcrumb
  - `ImportCsvViewModel`/`ExportCsvViewModel` `runCatching` failures (ImportCsvViewModel.kt:59,101,119; ExportCsvViewModel.kt:81) + count breadcrumbs on success
  - `NetworkRequest.kt` `SerializationException`/`ContentConvertException` → captureException; generic network failures → breadcrumb only (noise threshold)
  - `ScanPrefill` decode failure → breadcrumb

## CI (`deploy.yaml`)

- `build-android` → "Build release AAB": env `SENTRY_DSN: ${{ secrets.SENTRY_DSN }}`, `SENTRY_AUTH_TOKEN: ${{ secrets.SENTRY_AUTH_TOKEN }}`.
- New "Verify Sentry secrets" step: require `SENTRY_DSN` (fail if empty), warn only if auth token missing (symbol upload skipped, build continues).
- iOS has no CI job today; DSN set locally via env/xcconfig on release builds.

## Verification

- `./gradlew :shared:compileKotlinMetadata` clean.
- `./gradlew :androidApp:assembleRelease` with `SENTRY_DSN` set → BuildConfig populated, unhandled exception event reaches Sentry.
- Debug build (no DSN) → init no-op, zero reporter cost.
- iOS release build locally → event reaches Sentry; Info.plist carries DSN.
- Unit tests: `SentryReporter` noop path resolves from Koin; ViewModel tests unaffected (interface).

## Commits

Conventional style per step. Explicit file staging; never `git add -A`.