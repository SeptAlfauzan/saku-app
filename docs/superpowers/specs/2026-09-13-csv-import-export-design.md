# Design: Transaction CSV import & export

Date: 2026-09-13
Branch: `feat/csv-import-export`
PRD: Transaction Import & Export (CSV), v1

## Problem

BigPickle offers no way to bulk-add, bulk-edit, backup, or externally analyze
transactions. No CSV writer, parser, file picker, or settings screen exists in
the codebase today (the PRD's "raw export for debugging" does not exist in
`shared/src`). This feature adds a human-readable CSV export/import pair that
round-trips safely between the app and a spreadsheet.

## Approach

Domain-border CSV layer (pure commonMain, zero new dependencies):
`Transaction` ⇄ human-readable columns, enum/display-name/category resolution at
the border. Import runs parse → validate → classify → apply in a single Room
transaction with a session-scoped undo.

## Phasing (same branch, both land before merge)

- **Phase 1 — core**: CSV writer/parser + column mapping + validation + export
  filters + import classification + repository batch methods + tests. No
  platform I/O, all commonTest-able.
- **Phase 2 — platform + UI**: `SystemFileIo` expect/actual (Android SAF, iOS
  document picker), Settings screen, export flow, import flow, DI wiring, UI
  verification on device.

## Design

### 1. Core CSV layer

New `shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/importexport/`:

- `csv/CsvWriter.kt` — RFC 4180: quote fields containing `,` `"` `\n` `\r`,
  double `""` escaping, CRLF row endings.
- `csv/CsvParser.kt` — tokenizer: quoted fields, embedded newlines, trailing
  empty fields. Auto-detects delimiter: comma default; semicolon if the first
  non-empty line contains `;` and no `,`.
- `csv/TransactionCsvMapper.kt` — 14-column header per PRD §5.2:
  `Transaction ID, Type, Amount, Currency, Merchant, Category, Notes, Entry
  Method, Detected From, Status, Date & Time, Created At, Last Updated,
  Confidence`.
  - Type: title case; `Income` / `Expense` only. `TRANSFER` never exported,
    rejected on import.
  - Category: `categoryId` ⇄ display name via category lookup.
  - Source: `MANUAL` → `Manual`; `NOTIFICATION`/`SCAN` → `Auto-detected`.
  - Detected From: `sourcePackage` ⇄ display label via `NotificationSource`
    table; fallback raw package name. Import: input matching an existing source
    display label → its package name; else store input verbatim.
  - Status: `Confirmed` / `Pending`.
  - Confidence: `87%` ⇄ `Double`; blank → `0.0`.
  - Date & Time / Created At: `yyyy-MM-dd HH:mm:ss`, device local timezone.
    Import accepts ISO primary plus `yyyy-MM-dd HH:mm` and `yyyy-MM-dd`
    fallbacks. New rows with a valid `Created At` preserve it (restore
    scenario); otherwise it defaults to import time. Last Updated is never read
    from file (always import time).
  - Amount: positive `Long` integer; direction from Type.
  - Currency: `IDR`, case-insensitive; blank → `IDR`.
- `csv/TransactionCsvValidator.kt` — per-field validation returning
  `List<CsvError(row, field, reason)>` where `row` is the CSV line number
  (1 = header). Covers: missing/invalid type, non-positive amount, non-IDR
  currency, unparsable date, unknown category name, invalid status/confidence.

### 2. Repository + DAO (phase 1)

- `TransactionDao`: add `suspend fun getAll(): List<TransactionEntity>`.
- `TransactionRepository` + `RoomTransactionRepository`:
  - `suspend fun getAll(): List<Transaction>`.
  - `suspend fun applyImport(changes: List<Transaction>): ApplyResult` — every
    change applied inside a single `db.withTransaction`; id exists → update,
    else insert. Crash mid-apply → nothing persisted.
  - `suspend fun undoImport(undo: UndoSnapshot)` — restore overwritten
    originals, delete inserted ids.
  - `RoomTransactionRepository` gains an `AppDatabase` dependency (for
    `withTransaction`); small DI update in `AppModule.kt`.

### 3. Use cases (phase 1)

- `ExportTransactions` — filter (date range / type / category) → map → CSV
  string. Returns `(csv, excludedTransfers: Int)`.
- `ImportTransactions` pipeline:
  1. `parse(bytes)` → raw rows → drafts.
  2. `validate(drafts)` → `List<CsvError>`. Any error ⇒ import blocked, nothing
     imports (all-or-nothing at file scope).
  3. `classify(existing)` — `Transaction ID` found → update; missing/blank →
     create. Blank-ID rows matched for duplicates on date + amount + merchant
     (case-insensitive merchant).
  4. `resolveCategories` — unmatched name is a validation error (fix file,
     retry). No interactive category-mapping UI in v1.
  5. `confirm()` → `applyImport` → `ImportSummary` + `UndoSnapshot` held in the
     ViewModel (session-only undo).

### 4. Platform file bridge (phase 2)

`data/importexport/port/SystemFileIo.kt`:

```kotlin
class ImportedFile(val name: String, val bytes: ByteArray)

expect class SystemFileIo {
    suspend fun pickCsv(): ImportedFile?   // null = cancelled
    suspend fun saveCsv(fileName: String, content: String): Boolean
}
```

- **androidMain** (`SystemFileIo.android.kt`): `OpenDocument` /
  `CreateDocument` activity results bridged from a `MainActivity`-backed impl;
  read via `ContentResolver`; write with UTF-8 BOM.
- **iosMain** (`SystemFileIo.ios.kt`): `UIDocumentPickerViewController`
  (`UTType.commaSeparatedText` for read, `forExporting` for write), security
  scoped access, UIKit delegate → Kotlin `Continuation`.
- Wired via Koin (`appModule`): `systemFileIo()` actual factory.
- Export filename: `bigpickle-transactions-YYYYMMDD-HHmm.csv`; CRLF; decimal
  separator always `.`.

### 5. UI (phase 2)

- `Routes.SETTINGS` + `ui/settings/SettingsScreen.kt`. Gear entry in the
  Dashboard header and Transactions list top bar. Two rows: **Export
  Transactions** ("Save CSV — analyze in Sheets/Excel"), **Import
  Transactions** ("Load CSV — bulk create/edit").
- Export flow (`ui/data/ExportRoute`): filter form — date range (start/end
  pickers), type (All / Income / Expense), category (All / one category); note
  when transfers excluded; CTA → `saveCsv` → done screen "N transactions
  exported".
- Import flow (`ui/data/ImportRoute`):
  1. Select file — drop-zone card (brand look from
     `docs/superpowers/design/boro_money_tracker_csv_data_import`) →
     `pickCsv`.
  2. Preview — counts New / Will update / Possible duplicates / Errors. Errors
     > 0: CTA disabled, inline `Row · Field · Reason` list, "Fix file and
     retry". Else: duplicate toggle (skip ON by default), sample rows, CTA
     **Import N**; progress indicator for large files (parse/apply on
     `Dispatchers.Default`).
  3. Done — "Imported N transactions" + **Undo** (session-only) + dismiss.
- All strings localized (en/id), app design system (Hanken Grotesk, ink/crimson
  tokens, `SakuTheme`).

### 6. Error handling

- File-scope atomic: any validation error blocks apply.
- DB-scope atomic: apply inside single `withTransaction`; crash-safe.
- Session-scope undo: `UndoSnapshot` in ViewModel; reverts exactly touched rows.
- Parse-level failures (malformed CSV) surface as a friendly error, not a
  crash.

## Deviations from PRD

- **Any error blocks the whole file** (overrides PRD §6.5 partial import).
- **Income/Expense only**; `TRANSFER` excluded from export (counted) and
  rejected on import.
- **Currency: IDR only** (app hardcodes IDR today; stricter than §6.5).
- **No category-mapping resolver UI**; unmatched name = error.
- **Inline error list**, no downloadable errors.csv (FR-7 simplified).
- **Legacy raw-schema import dropped**; only comma/semicolon delimiter
  auto-detect kept (no legacy export exists in code).
- PRD §5.3 filters included as specified.

## Out of scope (v1)

- Custom column mapping for third-party CSVs (PRD §10 Phase 2).
- Category/account/budget/recurring export.
- Cloud backup/sync.
- Currency conversion.
- Platform file bridge beyond CSV (no JSON/zip backup).

## Testing

- **commonTest**: `CsvParserTest`, `CsvWriterTest`,
  `TransactionCsvMapperTest`, `TransactionCsvValidatorTest`,
  `ImportTransactionsTest`, `ExportTransactionsTest` — see section 1-3 for
  coverage (quoting, round-trip, classification, duplicates, error-blocks-all,
  undo, filters, transfer exclusion).
- **androidHostTest**: `applyImport` atomicity (forced mid-batch failure) and
  undo round-trip against real Room (existing `AppDatabaseTest` pattern).
- **Manual**: device verification of pick/save on Android + iOS.