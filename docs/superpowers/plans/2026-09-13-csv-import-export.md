# CSV Import & Export Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a human-readable CSV export/import pair for transactions (via a new Settings screen) that round-trips safely between the Saku app and a spreadsheet, with validation that blocks the whole file on any row error.

**Architecture:** Domain-border CSV layer in `shared/commonMain` — `Transaction` ⇄ human-readable columns, enum/display-name/category resolution at the border. Import runs parse → validate → classify → apply in a single Room write transaction with a session-scoped undo. Platform I/O is Android-only for now (iOS UI is a placeholder; see Global Constraints). All screens live in `androidApp` (project convention — `shared` holds ViewModels only).

**Tech Stack:** Kotlin 2.x Multiplatform, Compose Multiplatform (UI on Android), Room 3.0.2 KMP (`androidx.room3`, `withWriteTransaction`), kotlinx-datetime 0.8.0, kotlinx-coroutines, Koin, kotlinx-serialization. No new dependencies.

## Global Constraints

- Tests are TDD: write the failing test, see it fail, implement, see it pass, commit — per task.
- Package root for shared code: `com.septaalfauzan.saku`. ViewModels live in `shared/src/commonMain/kotlin/com/septaalfauzan/saku/ui/…`; composables live in `androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/…`.
- Timestamps are `kotlin.time.Instant` (stdlib). `kotlinx.datetime` is only used for formatting/parsing to local time; never stored directly.
- `amount` is a whole-number `Long` (integer rupiah). No decimals anywhere.
- Only `INCOME` and `EXPENSE` participate in import/export. `TRANSFER` rows are never exported (counted) and rejected on import ("Type must be Income or Expense").
- Currency: `IDR` only, case-insensitive; blank → `IDR`.
- Any validation error blocks the entire file: nothing imports until the file is fixed. No partial import.
- `Date & Time` column is `yyyy-MM-dd HH:mm:ss`, device-local timezone. Import also accepts `yyyy-MM-dd HH:mm` and `yyyy-MM-dd`. `Last Updated` is NEVER read from the file — always set to import time. `Created At` is preserved only for new rows that carry a valid value.
- Config values: delimiter auto-detect (comma default; semicolon if first non-empty line has `;` and no `,`). RFC 4180 quoting: fields containing `,` `"` `\n` `\r` are quoted; `"` doubled. CRLF. UTF-8.
- Header matching is by name, trimmed + lowercased; unknown extra columns ignored; column order doesn't matter.
- Error model: `CsvError(row, field, reason)` where `row` = CSV line number (1 = header). UI shows `Row · Field · Reason`.
- UI strings go to `androidApp/src/main/res/values/strings.xml` (Indonesian, default locale) using keys `screen.component_text`.
- iOS: placeholder app (per `docs/superpowers/specs/2026-09-11-indonesian-localization-design.md`). The `SystemFileIo` iOS bridge is DEFERRED; export/import is Android-first. UI on both platforms can come later without touching core.
- Export filename: `bigpickle-transactions-YYYYMMDD-HHmm.csv`. Files written with UTF-8 BOM.
- No DB schema changes; no migration. Only new DAO/repository methods.

---

## File Structure

**Created — shared core (`shared/src/commonMain/kotlin/com/septaalfauzan/saku/`):**
- `data/importexport/csv/CsvWriter.kt` — RFC 4180 writer (pure).
- `data/importexport/csv/CsvParser.kt` — RFC 4180 parser + delimiter auto-detect (pure).
- `data/importexport/csv/CsvDateFormat.kt` — local datetime ⇄ `yyyy-MM-dd HH:mm:ss` (pure).
- `data/importexport/csv/TransactionCsvMapper.kt` — column headers, `Transaction` ⇄ row, display legends (pure).
- `data/importexport/csv/TransactionCsvValidator.kt` — `CsvRowDraft` → `TransactionDraft` with per-field errors (pure).
- `domain/importexport/ImportExportModels.kt` — `CsvError`, `CsvRowDraft`, `TransactionDraft`, `ExportFilter`, `ParsedRows`, `ImportAction`/`Classification`, `UndoSnapshot`, `ApplyResult`.
- `domain/importexport/ExportTransactions.kt` — export usecase.
- `domain/importexport/ImportTransactions.kt` — import parse/classify/apply/undo usecase.
- `ui/importexport/ExportCsvViewModel.kt` — export flow state.
- `ui/importexport/ImportCsvViewModel.kt` — import flow state.

**Created — Android app (`androidApp/src/main/kotlin/com/septaalfauzan/saku/`):**
- `ui/settings/SettingsScreen.kt` — settings list (Export / Import rows).
- `ui/importexport/ExportCsvScreen.kt` — filters form + save via `CreateDocument` launcher.
- `ui/importexport/ImportCsvScreen.kt` — pick → preview → apply → done/undo.

**Modified — shared:**
- `data/dao/TransactionDao.kt` — add `getAll()` and `deleteByIds(ids)`.
- `data/entity/TransactionEntity.kt` — untouched (only new DAO methods).
- `data/repository/RoomTransactionRepository.kt` — add `getAll`, `applyImport`, `undoImport`; ctor gains `db: AppDatabase`.
- `domain/repository/TransactionRepository.kt` — interface additions.
- `di/AppModule.kt` — repository ctor DI + usecase + ViewModel registrations.

**Modified — androidApp:**
- `ui/navigation/App.kt` — add `SETTINGS`, `EXPORT`, `IMPORT` routes; gear entries.
- `ui/dashboard/DashboardScreen.kt` — gear icon next to "Aktivitas Terbaru".
- `ui/transactions/TransactionListScreen.kt` — gear icon in header row.
- `di/AndroidAppModule.kt` — ViewModel registrations (screens live in androidApp).
- `MainActivity.kt` — file-save launcher bridge (optional; see Task 11).
- `res/values/strings.xml` — new Indonesian strings.
- `AppModuleTest.kt` (androidHostTest) — if it asserts module graph, add new singles.
- `androidHostTest/.../data/repository/RoomTransactionRepositoryImportTest.kt` — atomicity + undo tests.

**Created — tests (`shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/` unless noted):**
- `csv/CsvWriterTest.kt`, `csv/CsvParserTest.kt`, `csv/CsvDateFormatTest.kt`, `csv/TransactionCsvMapperTest.kt`, `csv/TransactionCsvValidatorTest.kt`, `ImportTransactionsTest.kt`, `ExportTransactionsTest.kt`.

---

### Task 1: CSV writer (RFC 4180)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/importexport/csv/CsvWriter.kt`
- Test: `shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/csv/CsvWriterTest.kt`

**Interfaces:**
- Produces: `object CsvWriter { fun write(rows: List<List<String?>>): String; fun escape(field: String?): String }`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.septaalfauzan.saku.data.importexport.csv

import kotlin.test.Test
import kotlin.test.assertEquals

class CsvWriterTest {

    @Test
    fun writesPlainFieldsSeparatedByCommasWithCrlf() {
        val csv = CsvWriter.write(listOf(listOf("a", "b", "c")))
        assertEquals("a,b,c\r\n", csv)
    }

    @Test
    fun quotesFieldsContainingCommas() {
        val csv = CsvWriter.write(listOf(listOf("hello, world", "ok")))
        assertEquals("\"hello, world\",ok\r\n", csv)
    }

    @Test
    fun doublesQuotesInsideQuotedFields() {
        val csv = CsvWriter.write(listOf(listOf("say \"hi\"")))
        assertEquals("\"say \"\"hi\"\"\"\r\n", csv)
    }

    @Test
    fun quotesFieldsWithNewlinesAndCarriageReturns() {
        val csv = CsvWriter.write(listOf(listOf("line1\nline2")))
        assertEquals("\"line1\nline2\"\r\n", csv)
    }

    @Test
    fun nullAndEmptyFieldsBecomeEmpty() {
        val csv = CsvWriter.write(listOf(listOf("x", null, "")))
        assertEquals("x,,\r\n", csv)
    }

    @Test
    fun writesMultipleRows() {
        val csv = CsvWriter.write(listOf(listOf("a"), listOf("b")))
        assertEquals("a\r\nb\r\n", csv)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.septaalfauzan.saku.data.importexport.csv.CsvWriterTest" --console=plain 2>&1 | tail -20`
Expected: FAIL (compilation — `CsvWriter` missing). (commonTest runs through androidHostTest; this project's aggregate command. If compilation of commonTest blocks host test, use `./gradlew :shared:compileAndroidHostTestSources` first to see errors.)

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.septaalfauzan.saku.data.importexport.csv

object CsvWriter {
    /** Serializes rows per RFC 4180: CRLF endings, quoting fields containing , " \n \r, doubling embedded quotes. */
    fun write(rows: List<List<String?>>): String = buildString {
        rows.forEach { row ->
            append(row.joinToString(",") { escape(it) })
            append("\r\n")
        }
    }

    fun escape(field: String?): String {
        if (field == null) return ""
        return if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.septaalfauzan.saku.data.importexport.csv.CsvWriterTest" --console=plain`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/importexport/csv/CsvWriter.kt shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/csv/CsvWriterTest.kt
git commit -m "feat(importexport): add RFC 4180 CSV writer"
```

---

### Task 2: CSV parser with delimiter auto-detect

**Files:**
- Create: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/importexport/csv/CsvParser.kt`
- Test: `shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/csv/CsvParserTest.kt`

**Interfaces:**
- Produces: `object CsvParser { fun parse(input: String): List<List<String>>; fun detectDelimiter(input: String): Char }` — `parse` returns all rows including the header as first element; empty/blank lines dropped; trailing empty fields preserved.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.septaalfauzan.saku.data.importexport.csv

import kotlin.test.Test
import kotlin.test.assertEquals

class CsvParserTest {

    @Test
    fun parsesSimpleRows() {
        val rows = CsvParser.parse("a,b\r\n1,2\r\n")
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), rows)
    }

    @Test
    fun handlesQuotedFieldWithComma() {
        val rows = CsvParser.parse("\"hello, world\",ok\r\n")
        assertEquals(listOf(listOf("hello, world", "ok")), rows)
    }

    @Test
    fun handlesDoubledQuoteInsideQuotedField() {
        val rows = CsvParser.parse("\"say \"\"hi\"\"\"\r\n")
        assertEquals(listOf(listOf("say \"hi\"")), rows)
    }

    @Test
    fun handlesQuoteNotAtFieldStartAsLiteral() {
        val rows = CsvParser.parse("ab\"cd\r\n")
        assertEquals(listOf(listOf("ab\"cd")), rows)
    }

    @Test
    fun handlesEmbeddedNewlineInQuotedField() {
        val rows = CsvParser.parse("first,\"line1\nline2\"\r\n")
        assertEquals(1, rows.size)
        assertEquals(listOf("first", "line1\nline2"), rows.single())
    }

    @Test
    fun preservesTrailingEmptyFields() {
        val rows = CsvParser.parse("x,,,\r\n")
        assertEquals(listOf(listOf("x", "", "", "")), rows)
    }

    @Test
    fun dropsBlankLinesButKeepsHeaderRow() {
        val rows = CsvParser.parse("h1,h2\r\n\r\nv1,v2\r\n")
        assertEquals(listOf(listOf("h1", "h2"), listOf("v1", "v2")), rows)
    }

    @Test
    fun autoDetectsSemicolonDelimiter() {
        assertEquals(';', CsvParser.detectDelimiter("Transaction ID;Type;Amount"))
        assertEquals(',', CsvParser.detectDelimiter("Transaction ID,Type,Amount"))
        assertEquals(',', CsvParser.detectDelimiter("a;b,c;d"))
    }

    @Test
    fun parsesSemicolonDelimited() {
        val rows = CsvParser.parse("a;b;c\r\n1;2;3\r\n")
        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), rows)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.septaalfauzan.saku.data.importexport.csv.CsvParserTest" --console=plain 2>&1 | tail -20`
Expected: FAIL (compilation — `CsvParser` missing).

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.septaalfauzan.saku.data.importexport.csv

object CsvParser {
    /**
     * Parses RFC 4180 CSV. Handles quoted fields (commas, doubled quotes,
     * embedded newlines), CRLF/LF endings, and trailing empty fields.
     * Blank lines are dropped; the header (first row) is kept as element 0.
     */
    fun parse(input: String): List<List<String>> {
        val delimiter = detectDelimiter(input)
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < input.length && input[i + 1] == '"') {
                            field.append('"'); i += 2; continue
                        }
                        inQuotes = false; i++
                    } else {
                        field.append(c); i++
                    }
                }
                c == '"' -> { inQuotes = true; i++ }
                c == delimiter -> { row.add(field.toString()); field.setLength(0); i++ }
                c == '\r' -> i++
                c == '\n' -> { row.add(field.toString()); field.setLength(0); rows.add(row); row = mutableListOf(); i++ }
                else -> { field.append(c); i++ }
            }
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows.filter { it.any { cell -> cell.isNotBlank() } }
    }

    /** Comma by default; semicolon when the first non-empty line has ';' and no ','. */
    fun detectDelimiter(input: String): Char {
        val firstLine = input.substringBefore('\n').trim()
        return if (firstLine.contains(';') && !firstLine.contains(',')) ';' else ','
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.septaalfauzan.saku.data.importexport.csv.CsvParserTest" --console=plain`
Expected: PASS (9 tests).

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/importexport/csv/CsvParser.kt shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/csv/CsvParserTest.kt
git commit -m "feat(importexport): add RFC 4180 CSV parser"
```

---

### Task 3: CSV local-datetime formatting/parsing

**Files:**
- Create: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/importexport/csv/CsvDateFormat.kt`
- Test: `shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/csv/CsvDateFormatTest.kt`

**Interfaces:**
- Produces:
  - `fun formatCsvDateTime(millis: Long, zone: kotlinx.datetime.TimeZone = TimeZone.currentSystemDefault()): String` — `yyyy-MM-dd HH:mm:ss`.
  - `fun parseCsvDateTime(value: String, zone: kotlinx.datetime.TimeZone = TimeZone.currentSystemDefault()): kotlin.time.Instant?` — accepts `yyyy-MM-dd HH:mm:ss`, `yyyy-MM-dd HH:mm`, `yyyy-MM-dd`; blank/illegal → null.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.septaalfauzan.saku.data.importexport.csv

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CsvDateFormatTest {

    private val zone = TimeZone.UTC

    @Test
    fun formatsIsoDateTimeInLocalZone() {
        val instant = kotlinx.datetime.Instant.parse("2026-01-05T14:00:30Z")
        assertEquals("2026-01-05 14:00:30", formatCsvDateTime(instant.toEpochMilliseconds(), zone))
    }

    @Test
    fun parsesFullDateTime() {
        val instant = parseCsvDateTime("2026-01-05 14:00:30", zone)
        assertEquals(kotlinx.datetime.Instant.parse("2026-01-05T14:00:30Z").toEpochMilliseconds(), instant?.toEpochMilliseconds())
    }

    @Test
    fun parsesMinutePrecision() {
        val instant = parseCsvDateTime("2026-01-05 14:00", zone)
        assertEquals(kotlinx.datetime.Instant.parse("2026-01-05T14:00:00Z").toEpochMilliseconds(), instant?.toEpochMilliseconds())
    }

    @Test
    fun parsesDateOnlyAtMidnight() {
        val dateOnly = LocalDate(2026, 1, 5).atStartOfDayIn(zone).toEpochMilliseconds()
        assertEquals(dateOnly, parseCsvDateTime("2026-01-05", zone)?.toEpochMilliseconds())
    }

    @Test
    fun rejectsBlankAndInvalid() {
        assertNull(parseCsvDateTime("", zone))
        assertNull(parseCsvDateTime("not a date", zone))
        assertNull(parseCsvDateTime("2026-13-40", zone))
    }

    @Test
    fun roundTripsAcrossTimezones() {
        val jakarta = TimeZone.of("Asia/Jakarta")
        val utc = TimeZone.UTC
        val millis = kotlinx.datetime.Instant.parse("2026-01-05T07:00:00Z").toEpochMilliseconds()
        assertEquals(kotlinx.datetime.Instant.parse("2026-01-05T07:00:00Z").toEpochMilliseconds(),
            parseCsvDateTime(formatCsvDateTime(millis, jakarta), jakarta)?.toEpochMilliseconds())
        assertEquals(kotlinx.datetime.Instant.parse("2026-01-05T07:00:00Z").toEpochMilliseconds(),
            parseCsvDateTime(formatCsvDateTime(millis, utc), utc)?.toEpochMilliseconds())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.septaalfauzan.saku.data.importexport.csv.CsvDateFormatTest" --console=plain 2>&1 | tail -20`
Expected: FAIL (compilation — helpers missing).

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.septaalfauzan.saku.data.importexport.csv

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.Format
import kotlinx.datetime.format.char
import kotlinx.datetime.format.date
import kotlinx.datetime.format.hour
import kotlinx.datetime.format.minute
import kotlinx.datetime.format.time
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val fullFormat = LocalDateTime.Format { date(); char(' '); time() }
private val minuteFormat = LocalDateTime.Format { date(); char(' '); hour(); char(':'); minute() }
private val dateFormat = LocalDate.Format { date() }

fun formatCsvDateTime(
    millis: Long,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val local = kotlinx.datetime.Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone)
    return fullFormat.format(local)
}

fun parseCsvDateTime(
    value: String,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Instant? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    val kotlinxInstant = when {
        trimmed.contains(' ') && trimmed.count { it == ':' } >= 2 ->
            runCatching { LocalDateTime.parse(trimmed, fullFormat).toInstant(zone) }.getOrNull()
        trimmed.contains(' ') ->
            runCatching { LocalDateTime.parse(trimmed, minuteFormat).toInstant(zone) }.getOrNull()
        else ->
            runCatching { LocalDate.parse(trimmed, dateFormat).atStartOfDayIn(zone) }.getOrNull()
    }
    return kotlinxInstant?.toEpochMilliseconds()?.let { Instant.fromEpochMilliseconds(it) }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.septaalfauzan.saku.data.importexport.csv.CsvDateFormatTest" --console=plain`
Expected: PASS (6 tests). (If `hour()`/`minute()` imports are wrong, use `kotlinx.datetime.format.*` wildcard import and retry.)

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/importexport/csv/CsvDateFormat.kt shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/csv/CsvDateFormatTest.kt
git commit -m "feat(importexport): add CSV local datetime formatting"
```

---

### Task 4: Import/export domain models + column mapper

**Files:**
- Create: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/importexport/ImportExportModels.kt`
- Create: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/importexport/csv/TransactionCsvMapper.kt`
- Test: `shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/csv/TransactionCsvMapperTest.kt`

**Interfaces:**
- Produces:
  - `data class CsvError(val row: Int, val field: String, val reason: String)`
  - `data class CsvRowDraft(val lineNumber: Int, val id: String?, val type: String?, val amount: String?, val currency: String?, val merchant: String?, val category: String?, val notes: String?, val entryMethod: String?, val detectedFrom: String?, val status: String?, val occurredAt: String?, val createdAt: String?, val confidence: String?)`
  - `data class TransactionDraft(val lineNumber: Int, val id: String?, val type: TransactionType, val amount: Long, val currency: String, val merchant: String?, val categoryId: String?, val notes: String?, val source: TransactionSource, val sourcePackage: String?, val status: TransactionStatus, val occurredAt: Instant, val createdAt: Instant?, val confidence: Double)` with `fun toTransaction(now: Instant): Transaction`
  - `data class ExportFilter(val dateStart: Instant? = null, val dateEnd: Instant? = null, val type: TransactionType? = null, val categoryId: String? = null)`
  - `data class ParsedRows(val drafts: List<TransactionDraft>, val errors: List<CsvError>)`
  - `sealed interface ImportAction` + `data class CreateAction(val draft: TransactionDraft)`, `data class UpdateAction(val draft: TransactionDraft)`, `data class DuplicateAction(val draft: TransactionDraft)`, `data class Classification(val actions: List<ImportAction>)` with `val newCount/updateCount/duplicateCount`
  - `data class UndoSnapshot(val newIds: List<String>, val previousById: Map<String, Transaction>)`
  - `data class ApplyResult(val newCount: Int, val updateCount: Int, val snapshot: UndoSnapshot)`
  - `object TransactionCsvMapper` with `val headers: List<String>`, `fun headerIndexMap(headerRow: List<String>): Map<String, Int>`, `fun fromRow(lineNumber: Int, values: List<String>, index: Map<String, Int>): CsvRowDraft`, `fun toRow(tx: Transaction, categoryName: String?, sourceDisplay: String?): List<String?>`, plus display helpers `typeToDisplay(TransactionType)`, `typeFromDisplay(String?)`, `statusToDisplay(TransactionStatus)`, `statusFromDisplay(String?)`, `sourceToDisplay(TransactionSource)`.
- Consumes: `CsvDateFormat.kt` (Task 3).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.septaalfauzan.saku.data.importexport.csv

import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TransactionCsvMapperTest {

    private val now = Instant.fromEpochMilliseconds(1_770_000_000_000)

    private fun tx(
        id: String = "id-1",
        type: TransactionType = TransactionType.EXPENSE,
        amount: Long = 90000,
        categoryId: String? = "transport",
        source: TransactionSource = TransactionSource.NOTIFICATION,
        confidence: Double = 0.87,
    ) = Transaction(
        id = id, type = type, amount = amount, currency = "IDR",
        merchant = "Warung Bu Sari, Jakarta", categoryId = categoryId,
        description = "Grab to office", source = source,
        sourcePackage = "com.gojek.app", status = TransactionStatus.CONFIRMED,
        confidence = confidence,
        occurredAt = now, createdAt = now, updatedAt = now,
    )

    @Test
    fun headerOrderMatchesSpec() {
        assertEquals(
            listOf(
                "Transaction ID", "Type", "Amount", "Currency", "Merchant", "Category", "Notes",
                "Entry Method", "Detected From", "Status", "Date & Time", "Created At", "Last Updated", "Confidence",
            ),
            TransactionCsvMapper.headers,
        )
    }

    @Test
    fun mapsTransactionToReadableRow() {
        val row = TransactionCsvMapper.toRow(
            tx(),
            categoryName = "Transport",
            sourceDisplay = "gopay",
        )
        assertEquals("id-1", row[0])
        assertEquals("Expense", row[1])
        assertEquals("90000", row[2])
        assertEquals("IDR", row[3])
        assertEquals("Warung Bu Sari, Jakarta", row[4])
        assertEquals("Transport", row[5])
        assertEquals("Grab to office", row[6])
        assertEquals("Auto-detected", row[7])
        assertEquals("gopay", row[8])
        assertEquals("Confirmed", row[9])
        assertEquals("2026-01-25 00:00:00", row[10])
        assertEquals("87%", row[13])
    }

    @Test
    fun manualTransactionsShowManualAndBlankConfidence() {
        val row = TransactionCsvMapper.toRow(
            tx(source = TransactionSource.MANUAL, categoryId = null, confidence = 0.0),
            categoryName = null,
            sourceDisplay = null,
        )
        assertEquals("Manual", row[7])
        assertEquals("", row[8])
        assertEquals("", row[5])
        assertEquals("", row[13])
    }

    @Test
    fun parsesRowIntoDraftByHeaderNameIgnoringOrder() {
        val values = listOf("90000", "2026-01-05 14:00:30", "Income", "IDR")
        val index = TransactionCsvMapper.headerIndexMap(
            listOf("Amount", "Date & Time", "Type", "Currency"),
        )
        val draft = TransactionCsvMapper.fromRow(lineNumber = 2, values = values, index = index)
        assertEquals(2, draft.lineNumber)
        assertEquals("90000", draft.amount)
        assertEquals("Income", draft.type)
        assertEquals("IDR", draft.currency)
        assertEquals("2026-01-05 14:00:30", draft.occurredAt)
        assertNull(draft.merchant)
    }

    @Test
    fun displayLegendsRoundTrip() {
        assertEquals(TransactionType.INCOME, TransactionCsvMapper.typeFromDisplay("income"))
        assertEquals(TransactionType.EXPENSE, TransactionCsvMapper.typeFromDisplay("expense"))
        assertNull(TransactionCsvMapper.typeFromDisplay("transfer"))
        assertEquals(TransactionStatus.PENDING_REVIEW, TransactionCsvMapper.statusFromDisplay("pending"))
        assertEquals(TransactionSource.MANUAL, TransactionCsvMapper.sourceToDisplay("manual"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.septaalfauzan.saku.data.importexport.csv.TransactionCsvMapperTest" --console=plain 2>&1 | tail -20`
Expected: FAIL (compilation — models/mapper missing).

- [ ] **Step 3: Write implementation**

`ImportExportModels.kt`:

```kotlin
package com.septaalfauzan.saku.domain.importexport

import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.time.Instant

data class CsvError(val row: Int, val field: String, val reason: String)

data class CsvRowDraft(
    val lineNumber: Int,
    val id: String? = null,
    val type: String? = null,
    val amount: String? = null,
    val currency: String? = null,
    val merchant: String? = null,
    val category: String? = null,
    val notes: String? = null,
    val entryMethod: String? = null,
    val detectedFrom: String? = null,
    val status: String? = null,
    val occurredAt: String? = null,
    val createdAt: String? = null,
    val confidence: String? = null,
) {
    fun isEmpty(): Boolean =
        listOf(id, type, amount, currency, merchant, category, notes, entryMethod,
            detectedFrom, status, occurredAt, createdAt, confidence).all { it.isNullOrBlank() }
}

data class TransactionDraft(
    val lineNumber: Int,
    val id: String?,
    val type: TransactionType,
    val amount: Long,
    val currency: String,
    val merchant: String?,
    val categoryId: String?,
    val notes: String?,
    val source: TransactionSource,
    val sourcePackage: String?,
    val status: TransactionStatus,
    val occurredAt: Instant,
    val createdAt: Instant?,
    val confidence: Double,
) {
    fun toTransaction(now: Instant): Transaction = Transaction(
        id = id ?: kotlin.uuid.Uuid.random().toString(),
        type = type,
        amount = amount,
        currency = currency,
        merchant = merchant?.ifBlank { null },
        categoryId = categoryId,
        description = notes?.ifBlank { null },
        source = source,
        sourcePackage = sourcePackage?.ifBlank { null },
        status = status,
        confidence = confidence,
        occurredAt = occurredAt,
        createdAt = createdAt ?: now,
        updatedAt = now,
    )
}

data class ExportFilter(
    val dateStart: Instant? = null,
    val dateEnd: Instant? = null,
    val type: TransactionType? = null,
    val categoryId: String? = null,
)

data class ParsedRows(val drafts: List<TransactionDraft>, val errors: List<CsvError>)

sealed interface ImportAction {
    data class CreateAction(val draft: TransactionDraft) : ImportAction
    data class UpdateAction(val draft: TransactionDraft) : ImportAction
    data class DuplicateAction(val draft: TransactionDraft) : ImportAction
}

data class Classification(val actions: List<ImportAction>) {
    val newCount: Int get() = actions.count { it is ImportAction.CreateAction }
    val updateCount: Int get() = actions.count { it is ImportAction.UpdateAction }
    val duplicateCount: Int get() = actions.count { it is ImportAction.DuplicateAction }
}

data class UndoSnapshot(val newIds: List<String>, val previousById: Map<String, Transaction>)

data class ApplyResult(val newCount: Int, val updateCount: Int, val snapshot: UndoSnapshot)
```

`TransactionCsvMapper.kt`:

```kotlin
package com.septaalfauzan.saku.data.importexport.csv

import com.septaalfauzan.saku.domain.importexport.CsvRowDraft
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType

object TransactionCsvMapper {
    val headers: List<String> = listOf(
        "Transaction ID", "Type", "Amount", "Currency", "Merchant", "Category", "Notes",
        "Entry Method", "Detected From", "Status", "Date & Time", "Created At", "Last Updated", "Confidence",
    )

    /** Column index per normalized (trimmed, lowercase) header name. */
    fun headerIndexMap(headerRow: List<String>): Map<String, Int> =
        headerRow.mapIndexed { index, raw -> raw.trim().lowercase() to index }.toMap()

    fun fromRow(lineNumber: Int, values: List<String>, index: Map<String, Int>): CsvRowDraft {
        fun cell(key: String): String? =
            index[key]?.let { values.getOrNull(it)?.trim()?.ifBlank { null } }
        return CsvRowDraft(
            lineNumber = lineNumber,
            id = cell(HEADER_ID), type = cell(HEADER_TYPE), amount = cell(HEADER_AMOUNT),
            currency = cell(HEADER_CURRENCY), merchant = cell(HEADER_MERCHANT),
            category = cell(HEADER_CATEGORY), notes = cell(HEADER_NOTES),
            entryMethod = cell(HEADER_ENTRY_METHOD), detectedFrom = cell(HEADER_DETECTED_FROM),
            status = cell(HEADER_STATUS), occurredAt = cell(HEADER_OCCURRED_AT),
            createdAt = cell(HEADER_CREATED_AT), confidence = cell(HEADER_CONFIDENCE),
        )
    }

    fun toRow(
        tx: Transaction,
        categoryName: String?,
        sourceDisplay: String?,
    ): List<String?> = listOf(
        tx.id,
        typeToDisplay(tx.type),
        tx.amount.toString(),
        tx.currency,
        tx.merchant,
        categoryName?.takeIf { it.isNotBlank() },
        tx.description,
        sourceToDisplay(tx.source),
        if (tx.source == TransactionSource.MANUAL) null else sourceDisplay,
        statusToDisplay(tx.status),
        formatCsvDateTime(tx.occurredAt.toEpochMilliseconds()),
        formatCsvDateTime(tx.createdAt.toEpochMilliseconds()),
        formatCsvDateTime(tx.updatedAt.toEpochMilliseconds()),
        if (tx.source == TransactionSource.MANUAL) null else confidenceToDisplay(tx.confidence),
    )

    fun typeToDisplay(type: TransactionType): String = when (type) {
        TransactionType.INCOME -> "Income"
        TransactionType.EXPENSE -> "Expense"
        TransactionType.TRANSFER -> "Transfer"
    }

    fun typeFromDisplay(value: String?): TransactionType? = when (value?.trim()?.lowercase()) {
        "income" -> TransactionType.INCOME
        "expense" -> TransactionType.EXPENSE
        else -> null
    }

    fun statusToDisplay(status: TransactionStatus): String = when (status) {
        TransactionStatus.CONFIRMED -> "Confirmed"
        TransactionStatus.PENDING_REVIEW -> "Pending"
        TransactionStatus.IGNORED -> "Ignored"
    }

    fun statusFromDisplay(value: String?): TransactionStatus? = when (value?.trim()?.lowercase()) {
        "confirmed" -> TransactionStatus.CONFIRMED
        "pending" -> TransactionStatus.PENDING_REVIEW
        "ignored" -> TransactionStatus.IGNORED
        else -> null
    }

    fun sourceToDisplay(source: TransactionSource): String = when (source) {
        TransactionSource.MANUAL -> "Manual"
        TransactionSource.NOTIFICATION, TransactionSource.SCAN -> "Auto-detected"
    }

    fun sourceFromDisplay(value: String?): TransactionSource? = when (value?.trim()?.lowercase()) {
        "manual" -> TransactionSource.MANUAL
        "auto-detected" -> TransactionSource.NOTIFICATION
        else -> null
    }

    fun confidenceToDisplay(confidence: Double): String =
        (confidence * 100).roundToInt().toString() + "%"

    private const val HEADER_ID = "transaction id"
    private const val HEADER_TYPE = "type"
    private const val HEADER_AMOUNT = "amount"
    private const val HEADER_CURRENCY = "currency"
    private const val HEADER_MERCHANT = "merchant"
    private const val HEADER_CATEGORY = "category"
    private const val HEADER_NOTES = "notes"
    private const val HEADER_ENTRY_METHOD = "entry method"
    private const val HEADER_DETECTED_FROM = "detected from"
    private const val HEADER_STATUS = "status"
    private const val HEADER_OCCURRED_AT = "date & time"
    private const val HEADER_CREATED_AT = "created at"
    private const val HEADER_CONFIDENCE = "confidence"

    private fun Int.roundToInt(): Int =
        (this + 50) / 100 // unused; see confidenceToDisplay below
}
```

Note: `roundToInt` in the above stub is wrong — remove the shadow. `confidenceToDisplay` must use `kotlin.math.roundToInt()`:

```kotlin
import kotlin.math.roundToInt

fun confidenceToDisplay(confidence: Double): String =
    (confidence * 100).roundToInt().toString() + "%"
```

**Remove the bogus private `Int.roundToInt()` extension before compiling.**

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.septaalfauzan.saku.data.importexport.csv.TransactionCsvMapperTest" --console=plain`
Expected: PASS (5 tests). Watch the `2026-01-25 00:00:00` assertion: it hardcodes UTC output for `now`; if your machine's zone differs the test fails — run with `TZ=UTC` if needed, or change the assertion to `formatCsvDateTime(now.toEpochMilliseconds())`.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/importexport/ImportExportModels.kt shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/importexport/csv/TransactionCsvMapper.kt shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/csv/TransactionCsvMapperTest.kt
git commit -m "feat(importexport): add import/export models and column mapper"
```

---

### Task 5: Row validator (`CsvRowDraft` → `TransactionDraft`)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/importexport/csv/TransactionCsvValidator.kt`
- Test: `shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/csv/TransactionCsvValidatorTest.kt`

**Interfaces:**
- Consumes: `CsvRowDraft`, `TransactionDraft`, `CsvError` (Task 4), `TransactionCsvMapper` (Task 4), `CsvDateFormat` (Task 3).
- Produces: `object TransactionCsvValidator { fun buildDraft(draft: CsvRowDraft, categories: List<Category>, sources: List<NotificationSource>, now: Instant): Result }` where `data class Result(val draft: TransactionDraft?, val errors: List<CsvError>)` (defined in this file).
- Validation rules (each failure adds `CsvError(row = draft.lineNumber, field = "Type"/"Amount"/"Currency"/"Category"/"Entry Method"/"Status"/"Date & Time"/"Confidence", reason = …)`):
  - Type: required; must map via `typeFromDisplay`. `transfer` rejected: reason `Must be Income or Expense`.
  - Amount: required; must match `^\d+$` and parse to `Long`. Reason `Must be a non-negative whole number`.
  - Currency: blank → `IDR`; else must equal `IDR` case-insensitively. Reason `Currency must be IDR`.
  - Category: blank → null; else case-insensitive match against `categories.name`. Reason `No category named "<value>"`.
  - Entry Method: blank → `MANUAL`; else `sourceFromDisplay`; unknown → error.
  - Detected From: blank → null; else match `sources` by `providerId` or `packageName` (case-insensitive) → that source's `packageName`; no match → store raw value.
  - Status: blank → `CONFIRMED`; else `statusFromDisplay`; unknown → error.
  - Date & Time: required; `parseCsvDateTime`. Reason `Unrecognized date "<value>"`.
  - Created At: optional; `parseCsvDateTime`; invalid value → error.
  - Confidence: blank → `0.0`; else strip `%`, must parse to `Double` in `0.0..100.0`. Reason `Unrecognized confidence "<value>"`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.septaalfauzan.saku.data.importexport.csv

import com.septaalfauzan.saku.data.database.CategorySeed
import com.septaalfauzan.saku.domain.importexport.CsvRowDraft
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransactionCsvValidatorTest {

    private val categories: List<Category> = CategorySeed.categories
    private val sources: List<NotificationSource> = listOf(
        NotificationSource("com.gojek.app", "gopay", true),
        NotificationSource("com.bca", "bca", true),
    )
    private val now = Instant.fromEpochMilliseconds(1_770_000_000_000)

    private fun validDraft() = CsvRowDraft(
        lineNumber = 3,
        type = "Expense",
        amount = "90000",
        currency = "IDR",
        merchant = "Warung",
        category = "Transport",
        notes = "Grab",
        entryMethod = "Auto-detected",
        detectedFrom = "gopay",
        status = "Confirmed",
        occurredAt = "2026-01-05 14:00:30",
        confidence = "87%",
    )

    private fun result(draft: CsvRowDraft) =
        TransactionCsvValidator.buildDraft(draft, categories, sources, now)

    @Test
    fun buildsDraftFromValidRow() {
        val r = result(validDraft())
        assertTrue(r.errors.isEmpty(), r.errors.toString())
        val d = r.draft!!
        assertEquals(TransactionType.EXPENSE, d.type)
        assertEquals(90000L, d.amount)
        assertEquals("IDR", d.currency)
        assertEquals("transport", d.categoryId)
        assertEquals("Grab", d.notes)
        assertEquals(TransactionSource.NOTIFICATION, d.source)
        assertEquals("com.gojek.app", d.sourcePackage)
        assertEquals(TransactionStatus.CONFIRMED, d.status)
        assertEquals(0.87, d.confidence)
    }

    @Test
    fun defaultsMissingOptionalFields() {
        val r = result(CsvRowDraft(
            lineNumber = 4,
            type = "income",
            amount = "1000",
            occurredAt = "2026-01-05",
        ))
        assertTrue(r.errors.isEmpty(), r.errors.toString())
        val d = r.draft!!
        assertEquals(TransactionType.INCOME, d.type)
        assertEquals("IDR", d.currency)
        assertEquals(TransactionSource.MANUAL, d.source)
        assertEquals(TransactionStatus.CONFIRMED, d.status)
        assertEquals(0.0, d.confidence)
        assertNull(d.merchant)
        assertNull(d.categoryId)
    }

    @Test
    fun rejectsTransferType() {
        val r = result(validDraft().copy(type = "Transfer"))
        assertEquals("Must be Income or Expense", r.errors.first { it.field == "Type" }.reason)
        assertNull(r.draft)
    }

    @Test
    fun rejectsBadAmountAndCurrency() {
        val r = result(validDraft().copy(amount = "10,000", currency = "USD"))
        assertEquals("Must be a non-negative whole number", r.errors.first { it.field == "Amount" }.reason)
        assertEquals("Currency must be IDR", r.errors.first { it.field == "Currency" }.reason)
        assertNull(r.draft)
    }

    @Test
    fun rejectsUnknownCategoryAndMissingDate() {
        val r = result(validDraft().copy(category = "Grceries", occurredAt = null))
        assertTrue(r.errors.any { it.field == "Category" && it.reason == "No category named \"Grceries\"" })
        assertTrue(r.errors.any { it.field == "Date & Time" })
        assertNull(r.draft)
    }

    @Test
    fun resolvesSourcePackageByProviderIdOrPackageNameAndStoresRawOtherwise() {
        assertEquals("com.gojek.app", result(validDraft().copy(detectedFrom = "gopay")).draft?.sourcePackage)
        assertEquals("com.gojek.app", result(validDraft().copy(detectedFrom = "com.gojek.app")).draft?.sourcePackage)
        assertEquals("MyWallet", result(validDraft().copy(detectedFrom = "MyWallet")).draft?.sourcePackage)
    }

    @Test
    fun returnsAllErrorsForTheRow() {
        val r = result(validDraft().copy(type = "Transfer", amount = "abc", currency = "EUR",
            status = "Maybe", confidence = "120%", createdAt = "junk"))
        val fields = r.errors.map { it.field }.toSet()
        assertEquals(setOf("Type", "Amount", "Currency", "Status", "Confidence", "Created At"), fields)
        assertTrue(r.errors.all { it.row == 3 })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.septaalfauzan.saku.data.importexport.csv.TransactionCsvValidatorTest" --console=plain 2>&1 | tail -20`
Expected: FAIL (compilation — validator missing).

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.septaalfauzan.saku.data.importexport.csv

import com.septaalfauzan.saku.domain.importexport.CsvError
import com.septaalfauzan.saku.domain.importexport.CsvRowDraft
import com.septaalfauzan.saku.domain.importexport.TransactionDraft
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.math.roundToInt
import kotlin.time.Instant

object TransactionCsvValidator {

    data class Result(val draft: TransactionDraft?, val errors: List<CsvError>)

    fun buildDraft(
        draft: CsvRowDraft,
        categories: List<Category>,
        sources: List<NotificationSource>,
        now: Instant,
    ): Result {
        val errors = mutableListOf<CsvError>()
        val row = draft.lineNumber
        fun error(field: String, reason: String) { errors += CsvError(row, field, reason) }

        val type = TransactionCsvMapper.typeFromDisplay(draft.type)
        if (type == null) {
            if (draft.type.isNullOrBlank()) error("Type", "Required")
            else error("Type", "Must be Income or Expense")
        }

        val amount = draft.amount?.trim()?.let { raw ->
            if (raw.matches(Regex("""\d+"""))) raw.toLong()
            else { error("Amount", "Must be a non-negative whole number"); null }
        } ?: if (draft.amount.isNullOrBlank()) { error("Amount", "Required"); null } else null

        val currencyRaw = draft.currency?.trim().orEmpty()
        val currency = if (currencyRaw.equals("IDR", ignoreCase = true)) "IDR"
        else if (currencyRaw.isBlank()) "IDR"
        else { error("Currency", "Currency must be IDR"); "IDR" }

        val categoryId = draft.category?.let { name ->
            categories.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
                ?: run { error("Category", "No category named \"$name\""); null }
        }

        val source = draft.entryMethod?.let { TransactionCsvMapper.sourceFromDisplay(it) }
            ?: TransactionSource.MANUAL
        if (draft.entryMethod != null && source == TransactionSource.MANUAL &&
            !draft.entryMethod.equals("manual", ignoreCase = true)
        ) {
            error("Entry Method", "Unknown entry method")
        }

        val sourcePackage = draft.detectedFrom?.let { raw ->
            sources.firstOrNull {
                raw.equals(it.providerId, ignoreCase = true) || raw.equals(it.packageName, ignoreCase = true)
            }?.packageName ?: raw
        }

        val status = draft.status?.let { TransactionCsvMapper.statusFromDisplay(it) }
            ?: TransactionStatus.CONFIRMED
        if (draft.status != null && status == null) error("Status", "Unknown status")

        val occurredAt = if (draft.occurredAt.isNullOrBlank()) {
            error("Date & Time", "Required"); null
        } else {
            parseCsvDateTime(draft.occurredAt) ?: run {
                error("Date & Time", "Unrecognized date \"${draft.occurredAt}\""); null
            }
        }

        val createdAt = draft.createdAt?.let {
            parseCsvDateTime(it) ?: run { error("Created At", "Unrecognized date \"$it\""); null }
        }

        val confidence = if (draft.confidence.isNullOrBlank()) 0.0 else {
            val trimmed = draft.confidence.trim().removeSuffix("%")
            val value = trimmed.toDoubleOrNull()
            if (value != null && value in 0.0..100.0) value / 100.0
            else { error("Confidence", "Unrecognized confidence \"${draft.confidence}\""); 0.0 }
        }

        if (errors.isNotEmpty()) return Result(null, errors)

        val built = TransactionDraft(
            lineNumber = row,
            id = draft.id,
            type = type!!,
            amount = amount!!,
            currency = currency,
            merchant = draft.merchant?.ifBlank { null },
            categoryId = categoryId,
            notes = draft.notes?.ifBlank { null },
            source = source,
            sourcePackage = sourcePackage,
            status = status,
            occurredAt = occurredAt!!,
            createdAt = createdAt,
            confidence = confidence,
        )
        return Result(built, emptyList())
    }
}
```

Note on confidence tests: `0.87` is expected from input `87%`. The validator stores `value/100.0`. `TransactionCsvMapperTest` asserted `87%` for `0.87` via `(0.87*100).roundToInt()` = 87. Consistent.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.septaalfauzan.saku.data.importexport.csv.TransactionCsvValidatorTest" --console=plain`
Expected: PASS (7 tests). If `amount!!`/`type!!` non-null assertions trip the compiler's smart-cast analysis, restructure with local vals instead of `!!`.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/importexport/csv/TransactionCsvValidator.kt shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/csv/TransactionCsvValidatorTest.kt
git commit -m "feat(importexport): add per-row CSV validation"
```

---

### Task 6: Repository batch import + DI

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/dao/TransactionDao.kt`
- Modify: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/repository/TransactionRepository.kt`
- Modify: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/repository/RoomTransactionRepository.kt`
- Modify: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/di/AppModule.kt`
- Test: `shared/src/androidHostTest/kotlin/com/septaalfauzan/saku/data/repository/RoomTransactionRepositoryImportTest.kt`

**Interfaces:**
- Consumes: `UndoSnapshot`, `ApplyResult` (Task 4).
- Produces: `TransactionRepository` gains `suspend fun getAll(): List<Transaction>`, `suspend fun applyImport(changes: List<Transaction>): ApplyResult`, `suspend fun undoImport(snapshot: UndoSnapshot)`. `RoomTransactionRepository` ctor becomes `(db: AppDatabase, transactionDao: TransactionDao, categoryDao: CategoryDao)`; `applyImport` uses single `androidx.room3.withWriteTransaction`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.septaalfauzan.saku.data.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.septaalfauzan.saku.data.dao.CategoryDao
import com.septaalfauzan.saku.data.dao.TransactionDao
import com.septaalfauzan.saku.data.database.AppDatabase
import com.septaalfauzan.saku.data.database.AppDatabaseConstructor
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomTransactionRepositoryImportTest {

    private fun buildInMemory(): AppDatabase =
        Room.inMemoryDatabaseBuilder<AppDatabase>(
            factory = { AppDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver()).build()

    private fun tx(
        id: String,
        type: TransactionType = TransactionType.EXPENSE,
        amount: Long = 1000,
    ) = Transaction(
        id = id, type = type, amount = amount, currency = "IDR",
        merchant = null, categoryId = null, description = null,
        source = TransactionSource.MANUAL, sourcePackage = null,
        status = TransactionStatus.CONFIRMED, confidence = 0.0,
        occurredAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
        createdAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
        updatedAt = Instant.fromEpochMilliseconds(1_720_000_000_000),
    )

    @Test
    fun applyImportCreatesNewAndUpdatesExistingAtomically() = runTest {
        val db = buildInMemory()
        val dao: TransactionDao = db.transactionDao()
        val transactionDao = dao
        val categoryDao: CategoryDao = db.categoryDao()
        categoryDao.insertAll(com.septaalfauzan.saku.data.database.CategorySeed.categories.map {
            com.septaalfauzan.saku.data.entity.CategoryEntity(it.id, it.name, it.icon, it.type.name)
        })
        dao.insert(com.septaalfauzan.saku.data.entity.TransactionEntity(
            id = "existing", type = "EXPENSE", amount = 500, currency = "IDR",
            merchant = null, categoryId = null, description = null, source = "MANUAL",
            sourcePackage = null, status = "CONFIRMED", confidence = 0.0,
            occurredAtMillis = 1_720_000_000_000, createdAtMillis = 1_720_000_000_000,
            updatedAtMillis = 1_720_000_000_000,
        ))
        val repo = RoomTransactionRepository(db, transactionDao, categoryDao)

        val result = repo.applyImport(listOf(tx("new"), tx("existing", amount = 999)))

        assertEquals(1, result.newCount)
        assertEquals(1, result.updateCount)
        assertEquals(listOf("new"), result.snapshot.newIds)
        assertEquals(999, repo.getAll().first { it.id == "existing" }.amount)
        assertTrue(repo.getAll().any { it.id == "new" })
    }

    @Test
    fun undoRestoresExactlyTouchedRows() = runTest {
        val db = buildInMemory()
        val dao = db.transactionDao()
        val repo = RoomTransactionRepository(db, dao, db.categoryDao())
        dao.insert(com.septaalfauzan.saku.data.entity.TransactionEntity(
            id = "keep", type = "EXPENSE", amount = 1, currency = "IDR",
            merchant = null, categoryId = null, description = null, source = "MANUAL",
            sourcePackage = null, status = "CONFIRMED", confidence = 0.0,
            occurredAtMillis = 1, createdAtMillis = 1, updatedAtMillis = 1,
        ))
        dao.insert(com.septaalfauzan.saku.data.entity.TransactionEntity(
            id = "victim", type = "EXPENSE", amount = 2, currency = "IDR",
            merchant = "old", categoryId = null, description = null, source = "MANUAL",
            sourcePackage = null, status = "CONFIRMED", confidence = 0.0,
            occurredAtMillis = 1, createdAtMillis = 1, updatedAtMillis = 1,
        ))

        val result = repo.applyImport(listOf(tx("fresh"), tx("victim", amount = 9)))
        repo.undoImport(result.snapshot)

        assertEquals(setOf("keep", "victim"), repo.getAll().map { it.id }.toSet())
        assertEquals(2L, repo.getAll().first { it.id == "victim" }.amount)
    }

    @Test
    fun getAllReturnsSnapshotOfAllRows() = runTest {
        val db = buildInMemory()
        val dao = db.transactionDao()
        val repo = RoomTransactionRepository(db, dao, db.categoryDao())
        repo.applyImport(listOf(tx("a"), tx("b")))
        assertEquals(listOf("a", "b").sorted(), repo.getAll().map { it.id }.sorted())
    }
}
```

Note: if `CategoryEntity(it.id, it.name, it.icon, it.type.name)` positional args mismatch, construct named. Check `CategoryEntity.kt` field order before finalizing this test.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.septaalfauzan.saku.data.repository.RoomTransactionRepositoryImportTest" --console=plain 2>&1 | tail -20`
Expected: FAIL (compilation).

- [ ] **Step 3: Write implementation**

`TransactionDao.kt` — add:

```kotlin
@Query("SELECT * FROM transactions")
suspend fun getAll(): List<TransactionEntity>

@Query("DELETE FROM transactions WHERE id IN (:ids)")
suspend fun deleteByIds(ids: List<String>)
```

`TransactionRepository.kt` — add to interface:

```kotlin
suspend fun getAll(): List<Transaction>
suspend fun applyImport(changes: List<Transaction>): ApplyResult
suspend fun undoImport(snapshot: UndoSnapshot)
```

Add imports: `com.septaalfauzan.saku.domain.importexport.ApplyResult`, `com.septaalfauzan.saku.domain.importexport.UndoSnapshot`.

`RoomTransactionRepository.kt` — change ctor to `(private val db: AppDatabase, ...)` and add:

```kotlin
override suspend fun getAll(): List<Transaction> =
    transactionDao.getAll().map { it.toDomain() }

override suspend fun applyImport(changes: List<Transaction>): ApplyResult = db.withWriteTransaction {
    val existing = transactionDao.getAll().associateBy { it.id }
    val created = mutableListOf<String>()
    val updated = mutableListOf<String>()
    val previous = mutableMapOf<String, TransactionEntity>()
    changes.forEach { tx ->
        val entity = tx.toEntity()
        val current = existing[tx.id]
        if (current != null) {
            previous[tx.id] = current
            transactionDao.update(entity)
            updated += tx.id
        } else {
            transactionDao.insert(entity)
            created += tx.id
        }
    }
    ApplyResult(
        newCount = created.size,
        updateCount = updated.size,
        snapshot = UndoSnapshot(created, previous.mapValues { it.value.toDomain() }),
    )
}

override suspend fun undoImport(snapshot: UndoSnapshot) = db.withWriteTransaction {
    snapshot.previousById.forEach { (_, tx) -> transactionDao.update(tx.toEntity()) }
    transactionDao.deleteByIds(snapshot.newIds)
}
```

Imports to add in `RoomTransactionRepository.kt`:
```kotlin
import androidx.room3.withWriteTransaction
import com.septaalfauzan.saku.data.database.AppDatabase
import com.septaalfauzan.saku.domain.importexport.ApplyResult
import com.septaalfauzan.saku.domain.importexport.UndoSnapshot
```

`AppModule.kt` — change:

```kotlin
single<TransactionRepository> { RoomTransactionRepository(get(), get(), get()) }
```
(`get()` resolves `AppDatabase`, `TransactionDao`, `CategoryDao` by type — order in ctor matches.)

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.septaalfauzan.saku.data.repository.RoomTransactionRepositoryImportTest" --console=plain`
Expected: PASS (3 tests). Also run the full host test suite once: `./gradlew :shared:testAndroidHostTest --console=plain` — existing tests must still pass (DI graph change only; `AppModuleTest` will fail if it constructs the repository by hand or asserts projections — fix there as needed).

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/dao/TransactionDao.kt shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/repository/TransactionRepository.kt shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/repository/RoomTransactionRepository.kt shared/src/commonMain/kotlin/com/septaalfauzan/saku/di/AppModule.kt shared/src/androidHostTest/kotlin/com/septaalfauzan/saku/data/repository/RoomTransactionRepositoryImportTest.kt
git commit -m "feat(importexport): add atomic batch import and undo to repository"
```

---

### Task 7: Export + Import usecases

**Files:**
- Create: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/importexport/ExportTransactions.kt`
- Create: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/importexport/ImportTransactions.kt`
- Create: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/importexport/TransactionCsv.kt` (shared compile-retrieval helper)
- Test: `shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/ExportTransactionsTest.kt`
- Test: `shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/ImportTransactionsTest.kt`

**Interfaces:**
- Consumes: `CsvParser`, `CsvWriter`, `TransactionCsvMapper`, `TransactionCsvValidator` (Tasks 1-5), `TransactionRepository.observeCategories()`, `NotificationSettingsRepository.observeSources()`.
- Produces:
  - `class ExportTransactions(private val transactionRepository: TransactionRepository, private val notificationSettingsRepository: NotificationSettingsRepository)` with `data class Result(val csv: String, val exportedCount: Int, val excludedTransfers: Int)` and `suspend fun export(filter: ExportFilter): Result`.
  - `class ImportTransactions(private val transactionRepository: TransactionRepository, private val notificationSettingsRepository: NotificationSettingsRepository)` with `suspend fun parse(text: String): ParsedRows`, `suspend fun classify(drafts: List<TransactionDraft>): Classification`, `suspend fun apply(actions: List<ImportAction>): ApplyResult`, `suspend fun undo(snapshot: UndoSnapshot)`. `classify` resolves against DB state; `apply` skips `DuplicateAction`s and maps drafts to `Transaction` with `now`.

- [ ] **Step 1: Write the failing tests**

`ImportTransactionsTest.kt`:

```kotlin
package com.septaalfauzan.saku.data.importexport

import com.septaalfauzan.saku.data.dao.CategoryDao
import com.septaalfauzan.saku.data.dao.NotificationSourceDao
import com.septaalfauzan.saku.data.database.CategorySeed
import com.septaalfauzan.saku.data.entity.CategoryEntity
import com.septaalfauzan.saku.domain.importexport.CreateAction
import com.septaalfauzan.saku.domain.importexport.DuplicateAction
import com.septaalfauzan.saku.domain.importexport.ImportTransactions
import com.septaalfauzan.saku.domain.importexport.UpdateAction
import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeCategoryDao : CategoryDao {
    override fun observeAll(): Flow<List<CategoryEntity>> = flowOf(
        CategorySeed.categories.map { CategoryEntity(it.id, it.name, it.icon, it.type.name) },
    )
    override suspend fun count(): Long = 14
    override suspend fun insertAll(items: List<CategoryEntity>) {}
}

private class FakeTransportRepository : TransactionRepository {
    var existing: List<Transaction> = emptyList()
    val applied = mutableListOf<Transaction>()
    var undoSnapshot: UndoSnapshot? = null

    override fun observeTransactions() = flowOf(existing)
    override fun observeCategories() = flowOf(
        CategorySeed.categories.map { it },
    )
    override suspend fun insert(transaction: Transaction) {}
    override suspend fun update(transaction: Transaction) {}
    override suspend fun delete(id: String) {}
    override fun observePending() = flowOf(emptyList())
    override suspend fun setStatus(id: String, status: TransactionStatus) {}
    override suspend fun findRecentDuplicate(key: com.septaalfauzan.saku.domain.model.DuplicateKey, withinStartMillis: Long, withinEndMillis: Long) = null
    override suspend fun getAll(): List<Transaction> = existing
    override suspend fun applyImport(changes: List<Transaction>): ApplyResult {
        applied += changes
        return ApplyResult(changes.count { it.id.startsWith("new") }, changes.count { it.id.startsWith("upd") }, UndoSnapshot(emptyList(), emptyMap()))
    }
    override suspend fun undoImport(snapshot: UndoSnapshot) { undoSnapshot = snapshot }
}

private class FakeSources : NotificationSettingsRepository {
    override fun observeSources(): Flow<List<NotificationSource>> = flowOf(
        listOf(NotificationSource("com.gojek.app", "gopay", true)),
    )
    override suspend fun setSourceEnabled(packageName: String, enabled: Boolean) {}
    override fun observeTrackingEnabled() = flowOf(false)
    override suspend fun setTrackingEnabled(enabled: Boolean) {}
    override fun observeAutoConfirm() = flowOf(true)
    override suspend fun setAutoConfirm(enabled: Boolean) {}
    override fun observeKeywords(packageName: String) = flowOf(emptyList())
    override suspend fun getKeywords(packageName: String) = emptyList()
    override suspend fun upsertKeywords(packageName: String, expenseWords: List<String>, incomeWords: List<String>, merchantWords: List<String>) {}
    override suspend fun deleteSource(packageName: String) {}
    override suspend fun addSource(source: NotificationSource, keywords: List<com.septaalfauzan.saku.domain.model.ParserKeyword>) {}
}

class ImportTransactionsTest {

    private fun repo() = FakeTransportRepository()
    private fun usecase(repo: FakeTransportRepository) = ImportTransactions(repo, FakeSources())

    @Test
    fun parseCollectsValidDraftsAndPerRowErrors() = runTest {
        val u = usecase(repo())
        val rows = ParsedRows(txId = "id-1", category = "Transport", occurredAt = "2026-01-05 14:00:30") // placeholder, replaced below
        // (see implementation test below: use a literal CSV string)
    }
}
```

**Simplify** — the fake-stub test above is scaffolding noise. Replace the entire test file with the following two clean tests that use a real in-memory Room DB for `TransactionRepository`/categories/sources, mirroring `RoomTransactionRepositoryImportTest`:

```kotlin
package com.septaalfauzan.saku.data.importexport

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.septaalfauzan.saku.data.dao.CategoryDao
import com.septaalfauzan.saku.data.dao.NotificationSourceDao
import com.septaalfauzan.saku.data.database.AppDatabase
import com.septaalfauzan.saku.data.database.AppDatabaseConstructor
import com.septaalfauzan.saku.data.database.CategorySeed
import com.septaalfauzan.saku.data.database.NotificationSourceSeed
import com.septaalfauzan.saku.data.entity.CategoryEntity
import com.septaalfauzan.saku.data.entity.NotificationSourceEntity
import com.septaalfauzan.saku.data.entity.TransactionEntity
import com.septaalfauzan.saku.data.repository.RoomNotificationSettingsRepository
import com.septaalfauzan.saku.data.repository.RoomTransactionRepository
import com.septaalfauzan.saku.domain.importexport.DuplicateAction
import com.septaalfauzan.saku.domain.importexport.ImportTransactions
import com.septaalfauzan.saku.domain.importexport.ParsedRows
import com.septaalfauzan.saku.domain.importexport.UpdateAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportTransactionsTest {

    private fun build(): AppDatabase =
        Room.inMemoryDatabaseBuilder<AppDatabase>(
            factory = { AppDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver()).build()

    private fun seed(db: AppDatabase) {
        db.categoryDao().insertAll(CategorySeed.categories.map { CategoryEntity(it.id, it.name, it.icon, it.type.name) })
        db.sourceDao().insertAll(NotificationSourceSeed.sources.map { NotificationSourceEntity(it.packageName, it.providerId, it.enabled) })
    }

    private fun headers() = "Transaction ID,Type,Amount,Currency,Merchant,Category,Notes,Entry Method,Detected From,Status,Date & Time,Created At,Last Updated,Confidence"

    @Test
    fun parseProducesDraftsAndErrors() = runTest {
        val db = build(); seed(db)
        val u = ImportTransactions(
            RoomTransactionRepository(db, db.transactionDao(), db.categoryDao()),
            RoomNotificationSettingsRepository(db.sourceDao(), db.settingsDao(), db.keywordDao()),
        )
        val csv = headers() + "\r\n" +
            "id-9,Expense,90000,IDR,Warung,Transport,Grab,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n" +
            "id-8,Transfer,500,IDR,,,,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n" +
            ",Expense,1000,USD,,,,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n"
        val parsed: ParsedRows = u.parse(csv)
        assertEquals(1, parsed.drafts.size)
        assertEquals("id-9", parsed.drafts.single().id)
        assertEquals(2, parsed.errors.size)
        assertTrue(parsed.errors.any { it.field == "Type" && it.row == 3 })
        assertTrue(parsed.errors.any { it.field == "Currency" && it.row == 4 })
    }

    @Test
    fun classifyMarksUpdatesCreatesAndDuplicates() = runTest {
        val db = build(); seed(db)
        val dao = db.transactionDao()
        dao.insert(TransactionEntity(
            id = "id-1", type = "EXPENSE", amount = 1000, currency = "IDR",
            merchant = "Warung", categoryId = null, description = null, source = "MANUAL",
            sourcePackage = null, status = "CONFIRMED", confidence = 0.0,
            occurredAtMillis = 1_770_000_000_000, createdAtMillis = 1_770_000_000_000,
            updatedAtMillis = 1_770_000_000_000,
        ))
        val u = ImportTransactions(
            RoomTransactionRepository(db, dao, db.categoryDao()),
            RoomNotificationSettingsRepository(db.sourceDao(), db.settingsDao(), db.keywordDao()),
        )
        val csv = headers() + "\r\n" +
            "id-1,Expense,1000,IDR,Warung,,,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n" +
            "id-2,Expense,1000,IDR,Warung,,,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n" +
            "id-3,Expense,999,IDR,,,Grab,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n" +
            ",Expense,1000,IDR,Warung,,,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n"
        val parsed = u.parse(csv)
        assertEquals(4, parsed.drafts.size)
        val classification = u.classify(parsed.drafts)
        assertEquals(1, classification.updateCount)
        assertEquals(2, classification.newCount)
        assertEquals(1, classification.duplicateCount)
        assertTrue(classification.actions[0] is UpdateAction)
        assertTrue(classification.actions[3] is DuplicateAction)
    }

    @Test
    fun applyWritesOnlyNonDuplicateActionsAndUndoReverts() = runTest {
        val db = build(); seed(db)
        val repo = RoomTransactionRepository(db, db.transactionDao(), db.categoryDao())
        val u = ImportTransactions(
            repo,
            RoomNotificationSettingsRepository(db.sourceDao(), db.settingsDao(), db.keywordDao()),
        )
        val csv = headers() + "\r\n" +
            "id-1,Expense,1000,IDR,Warung,,,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n" +
            "id-2,Expense,999,IDR,,,Grab,Manual,,Confirmed,2026-01-05 14:00:30,,,\r\n"
        val parsed = u.parse(csv)
        val classification = u.classify(parsed.drafts)
        val result = u.apply(classification.actions)
        assertEquals(2, result.newCount)
        u.undo(result.snapshot)
        assertEquals(0, repo.getAll().size)
    }
}
```

Notes: `RoomNotificationSettingsRepository` ctor signature — verify against `AppModule.kt:66-68` (`RoomNotificationSettingsRepository(get(), get(), get())` = sourceDao, settingsDao, keywordDao). Duplicate detection in `classify` compares against the DAO's current rows; row 4 of the `classify` test ("id-1,Expense,1000,IDR,Warung") is the duplicate of the seeded row.

`ExportTransactionsTest.kt`:

```kotlin
package com.septaalfauzan.saku.data.importexport

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.septaalfauzan.saku.data.database.AppDatabase
import com.septaalfauzan.saku.data.database.AppDatabaseConstructor
import com.septaalfauzan.saku.data.database.CategorySeed
import com.septaalfauzan.saku.data.database.NotificationSourceSeed
import com.septaalfauzan.saku.data.entity.CategoryEntity
import com.septaalfauzan.saku.data.entity.NotificationSourceEntity
import com.septaalfauzan.saku.data.repository.RoomNotificationSettingsRepository
import com.septaalfauzan.saku.data.repository.RoomTransactionRepository
import com.septaalfauzan.saku.domain.importexport.ExportFilter
import com.septaalfauzan.saku.domain.importexport.ExportTransactions
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportTransactionsTest {

    private fun build(): AppDatabase =
        Room.inMemoryDatabaseBuilder<AppDatabase>(
            factory = { AppDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver()).build()

    private fun tx(id: String, type: TransactionType, amount: Long, categoryId: String?) = Transaction(
        id = id, type = type, amount = amount, currency = "IDR",
        merchant = "Merchant $id", categoryId = categoryId, description = null,
        source = TransactionSource.NOTIFICATION, sourcePackage = "com.gojek.app",
        status = TransactionStatus.CONFIRMED, confidence = 0.75,
        occurredAt = Instant.fromEpochMilliseconds(1_770_000_000_000),
        createdAt = Instant.fromEpochMilliseconds(1_770_000_000_000),
        updatedAt = Instant.fromEpochMilliseconds(1_770_000_000_000),
    )

    @Test
    fun exportExcludesTransfersAndReportsCount() = runTest {
        val db = build()
        val dao = db.transactionDao()
        db.categoryDao().insertAll(CategorySeed.categories.map { CategoryEntity(it.id, it.name, it.icon, it.type.name) })
        db.sourceDao().insertAll(NotificationSourceSeed.sources.map { NotificationSourceEntity(it.packageName, it.providerId, it.enabled) })
        val repo = RoomTransactionRepository(db, dao, db.categoryDao())
        repo.applyImport(listOf(
            tx("e1", TransactionType.EXPENSE, 1000, "transport"),
            tx("i1", TransactionType.INCOME, 500, "salary"),
            tx("t1", TransactionType.TRANSFER, 300, null),
        )).let { }

        val usecase = ExportTransactions(repo, RoomNotificationSettingsRepository(db.sourceDao(), db.settingsDao(), db.keywordDao()))
        val result = usecase.export(ExportFilter())

        assertEquals(2, result.exportedCount)
        assertEquals(1, result.excludedTransfers)
        assertTrue(result.csv.startsWith("Transaction ID,Type,Amount,Currency,Merchant,Category"))
        assertTrue(result.csv.contains("\"Merchant e1\",Transport"))
        assertTrue(result.csv.contains("Auto-detected,gopay"))
        assertTrue(!result.csv.contains("t1"))
        assertTrue(result.csv.contains("75%"))
    }

    @Test
    fun exportAppliesTypeCategoryAndDateFilter() = runTest {
        val db = build()
        val dao = db.transactionDao()
        db.categoryDao().insertAll(CategorySeed.categories.map { CategoryEntity(it.id, it.name, it.icon, it.type.name) })
        db.sourceDao().insertAll(NotificationSourceSeed.sources.map { NotificationSourceEntity(it.packageName, it.providerId, it.enabled) })
        val repo = RoomTransactionRepository(db, dao, db.categoryDao())
        repo.applyImport(listOf(
            tx("e1", TransactionType.EXPENSE, 1000, "transport"),
            tx("e2", TransactionType.EXPENSE, 2000, "food"),
            tx("i1", TransactionType.INCOME, 500, "salary"),
        )).let { }

        val usecase = ExportTransactions(repo, RoomNotificationSettingsRepository(db.sourceDao(), db.settingsDao(), db.keywordDao()))
        val byCategory = usecase.export(ExportFilter(categoryId = "transport"))
        assertEquals(1, byCategory.exportedCount)
        assertTrue(byCategory.csv.contains("e1") && !byCategory.csv.contains("e2"))

        val byType = usecase.export(ExportFilter(type = TransactionType.INCOME))
        assertEquals(1, byType.exportedCount)
        assertTrue(byType.csv.contains("i1"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.septaalfauzan.saku.data.importexport.ImportTransactionsTest" --tests "com.septaalfauzan.saku.data.importexport.ExportTransactionsTest" --console=plain 2>&1 | tail -20`
Expected: FAIL (compilation — usecases missing).

- [ ] **Step 3: Write implementation**

`ExportTransactions.kt`:

```kotlin
package com.septaalfauzan.saku.domain.importexport

import com.septaalfauzan.saku.data.importexport.csv.CsvWriter
import com.septaalfauzan.saku.data.importexport.csv.TransactionCsvMapper
import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first

class ExportTransactions(
    private val transactionRepository: TransactionRepository,
    private val notificationSettingsRepository: NotificationSettingsRepository,
) {
    class Result(csv: String, exportedCount: Int, excludedTransfers: Int) {
        val csv = csv
        val exportedCount = exportedCount
        val excludedTransfers = excludedTransfers
    }

    suspend fun export(filter: ExportFilter): Result {
        val categories = transactionRepository.observeCategories().first()
        val sources = notificationSettingsRepository.observeSources().first()
        val categoryNameById = categories.associate { it.id to it.name }

        val all = transactionRepository.getAll()
        val eligible = all.filter { it.type == TransactionType.INCOME || it.type == TransactionType.EXPENSE }
        val filtered = eligible
            .filter { filter.type == null || it.type == filter.type }
            .filter { filter.categoryId == null || it.categoryId == filter.categoryId }
            .filter { filter.dateStart == null || it.occurredAt >= filter.dateStart }
            .filter { filter.dateEnd == null || it.occurredAt <= filter.dateEnd }

        val rows = listOf(TransactionCsvMapper.headers) +
            filtered.sortedByDescending { it.occurredAt.toEpochMilliseconds() }.map { tx ->
                TransactionCsvMapper.toRow(
                    tx = tx,
                    categoryName = tx.categoryId?.let { categoryNameById[it] },
                    sourceDisplay = sourceDisplay(tx, sources),
                )
            }
        return Result(
            csv = CsvWriter.write(rows),
            exportedCount = filtered.size,
            excludedTransfers = all.size - eligible.size,
        )
    }

    private fun sourceDisplay(tx: Transaction, sources: List<NotificationSource>): String? {
        if (tx.source == TransactionSource.MANUAL) return null
        val source = sources.firstOrNull { it.packageName == tx.sourcePackage }
        return source?.providerId ?: tx.sourcePackage
    }
}
```

`class Result(...)` — simpler as a `data class Result(val csv: String, val exportedCount: Int, val excludedTransfers: Int)`; use that instead (drop the manual property duplicates). Truncated version above is a placeholder for the real one — write the data class version.

`ImportTransactions.kt`:

```kotlin
package com.septaalfauzan.saku.domain.importexport

import com.septaalfauzan.saku.data.importexport.csv.CsvParser
import com.septaalfauzan.saku.data.importexport.csv.TransactionCsvMapper
import com.septaalfauzan.saku.data.importexport.csv.TransactionCsvValidator
import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first

class ImportTransactions(
    private val transactionRepository: TransactionRepository,
    private val notificationSettingsRepository: NotificationSettingsRepository,
) {
    suspend fun parse(text: String): ParsedRows {
        val categories = transactionRepository.observeCategories().first()
        val sources = notificationSettingsRepository.observeSources().first()
        val now = Clock.System.now()

        val rawRows = CsvParser.parse(text)
        if (rawRows.isEmpty()) {
            return ParsedRows(emptyList(), listOf(CsvError(0, "", "Empty file")))
        }
        val index = TransactionCsvMapper.headerIndexMap(rawRows.first())
        val drafts = mutableListOf<TransactionDraft>()
        val errors = mutableListOf<CsvError>()

        rawRows.drop(1).forEachIndexed { offset, values ->
            val lineNumber = offset + 2
            val src = TransactionCsvMapper.fromRow(lineNumber, values, index)
            if (src.isEmpty()) return@forEachIndexed
            val result = TransactionCsvValidator.buildDraft(src, categories, sources, now)
            if (result.errors.isNotEmpty()) errors.addAll(result.errors)
            result.draft?.let(drafts::add)
        }
        if (drafts.isEmpty() && errors.isEmpty()) {
            return ParsedRows(emptyList(), listOf(CsvError(0, "", "No importable rows")))
        }
        return ParsedRows(drafts, errors)
    }

    suspend fun classify(drafts: List<TransactionDraft>): Classification {
        val existing = transactionRepository.getAll()
        val existingById = existing.associateBy { it.id }
        val actions = drafts.map { draft ->
            when {
                draft.id != null && existingById.containsKey(draft.id) -> ImportAction.UpdateAction(draft)
                draft.id != null -> ImportAction.CreateAction(draft)
                existing.any { isSameTransaction(it, draft) } -> ImportAction.DuplicateAction(draft)
                else -> ImportAction.CreateAction(draft)
            }
        }
        return Classification(actions)
    }

    suspend fun apply(actions: List<ImportAction>): ApplyResult {
        val now = Clock.System.now()
        val changes = actions.filterIsInstance<ImportAction.CreateAction>()
            .map { it.draft.toTransaction(now) } +
            actions.filterIsInstance<ImportAction.UpdateAction>().map { it.draft.toTransaction(now) }
        return transactionRepository.applyImport(changes)
    }

    suspend fun undo(snapshot: UndoSnapshot) {
        transactionRepository.undoImport(snapshot)
    }

    private fun isSameTransaction(tx: com.septaalfauzan.saku.domain.model.Transaction, draft: TransactionDraft): Boolean {
        if (tx.type != draft.type || tx.amount != draft.amount) return false
        if (tx.occurredAt.toEpochMilliseconds() != draft.occurredAt.toEpochMilliseconds()) return false
        val txMerchant = tx.merchant?.trim()
        val draftMerchant = draft.merchant?.trim()
        return when {
            txMerchant.isNullOrEmpty() && draftMerchant.isNullOrEmpty() -> true
            txMerchant.isNullOrEmpty() || draftMerchant.isNullOrEmpty() -> false
            else -> txMerchant.equals(draftMerchant, ignoreCase = true)
        }
    }
}
```

Note: `observeCategories()` returns `Flow<List<Category>>`; `NotificationSettingsRepository.observeSources()` returns `Flow<List<NotificationSource>>`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.septaalfauzan.saku.data.importexport.ImportTransactionsTest" --tests "com.septaalfauzan.saku.data.importexport.ExportTransactionsTest" --console=plain`
Expected: PASS (5 tests). Verify `RoomNotificationSettingsRepository` ctor arg order against `AppModule.kt` first — if different, fix the test construction to match.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/importexport/ExportTransactions.kt shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/importexport/ImportTransactions.kt shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/ExportTransactionsTest.kt shared/src/commonTest/kotlin/com/septaalfauzan/saku/data/importexport/ImportTransactionsTest.kt
git commit -m "feat(importexport): add export and import usecases"
```

[Phase 1 DONE — core layer, fully tested. Run full host suite: `./gradlew :shared:testAndroidHostTest --console=plain` before proceeding.]

---

### Task 8: Settings screen + routes

**Files:**
- Modify: `androidApp/src/main/res/values/strings.xml`
- Create: `androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/settings/SettingsScreen.kt`
- Modify: `androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/navigation/App.kt`
- Modify: `androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/dashboard/DashboardScreen.kt`
- Modify: `androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/transactions/TransactionListScreen.kt`

**Interfaces:**
- Consumes: exists — `SakuTheme`, `SakuDp`, `SakuIcons`, `PillButton`, `PillButtonVariant`, `App.kt` routes.
- Produces: `Routes.SETTINGS = "settings"`, `Routes.EXPORT = "export"`, `Routes.IMPORT = "import"`. `SettingsRoute(onBack: () -> Unit, onOpenExport: () -> Unit, onOpenImport: () -> Unit)`. `DashboardRoute` gains `onOpenSettings: () -> Unit`; `TransactionListRoute` gains `onOpenSettings: () -> Unit`.

- [ ] **Step 1: Add strings**

`androidApp/src/main/res/values/strings.xml` — append:

```xml
    <!-- Settings / Data -->
    <string name="settings.title">Pengaturan</string>
    <string name="settings.export_transactions">Ekspor Transaksi</string>
    <string name="settings.export_body">Simpan CSV untuk dianalisis di Sheets/Excel</string>
    <string name="settings.import_transactions">Impor Transaksi</string>
    <string name="settings.import_body">Muat CSV untuk membuat/mengubah banyak transaksi</string>
```

- [ ] **Step 2: Create the screen**

```kotlin
package com.septaalfauzan.saku.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.R
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuTheme

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenImport: () -> Unit,
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    Column(Modifier.fillMaxSize().padding(horizontal = SakuDp.screenEdgePadding)) {
        Spacer(Modifier.height(SakuDp.spaceLg))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXs),
        ) {
            Icon(
                SakuIcons.Back, contentDescription = stringResource(R.string.common_back),
                tint = palette.ink, modifier = Modifier
                    .size(40.dp)
                    .clickable { onBack() }
                    .padding(SakuDp.spaceXs),
            )
            Text(stringResource(R.string.settings_title), style = type.headlineLg, color = palette.ink)
        }
        Spacer(Modifier.height(SakuDp.spaceLg))

        SettingsRow(
            title = stringResource(R.string.settings_export_transactions),
            body = stringResource(R.string.settings_export_body),
            onTap = onOpenExport,
        )
        SettingsRow(
            title = stringResource(R.string.settings_import_transactions),
            body = stringResource(R.string.settings_import_body),
            onTap = onOpenImport,
        )
    }
}

@Composable
private fun SettingsRow(title: String, body: String, onTap: () -> Unit) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(vertical = SakuDp.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = type.headlineSm, color = palette.ink)
            Text(body, style = type.bodySm, color = palette.slate)
        }
        Icon(
            SakuIcons.ChevronRight, contentDescription = null,
            tint = palette.slate, modifier = Modifier.size(20.dp),
        )
    }
}
```

- [ ] **Step 3: Wire routes + gear entries**

In `App.kt` `object Routes`:
```kotlin
const val SETTINGS = "settings"
const val EXPORT = "export"
const val IMPORT = "import"
```

Add composables:
```kotlin
composable(Routes.SETTINGS) {
    SettingsRoute(
        onBack = { navController.popBackStack() },
        onOpenExport = { navController.navigate(Routes.EXPORT) },
        onOpenImport = { navController.navigate(Routes.IMPORT) },
    )
}
composable(Routes.EXPORT) { ExportCsvRoute(onBack = { navController.popBackStack() }) }
composable(Routes.IMPORT) { ImportCsvRoute(onBack = { navController.popBackStack() }) }
```
(the `ExportCsvRoute`/`ImportCsvRoute` symbols arrive in Tasks 10-11; `App.kt` won't compile until those land — commit this task's screen + strings, and wire `SettingsRoute` only; add the two lines that reference export/import in the same commit as their screens, or add them here and let compilation fail until Task 11. Prefer: wire SETTINGS → EMPTY placeholder composables for EXPORT/IMPORT in this task so `App.kt` compiles, then replace placeholders in Tasks 10-11.)

Add import: `import com.septaalfauzan.saku.ui.settings.SettingsRoute`.

In `DashboardScreen.kt` — add `onOpenSettings: () -> Unit` param and a gear in the "Aktivitas Terbaru" header row:
```kotlin
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(stringResource(R.string.dashboard_recent_activity), style = type.headlineSm, color = palette.ink)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.dashboard_view_all), style = type.labelMd, color = palette.crimson,
            modifier = Modifier.clickable { onOpenAll() })
        Icon(SakuIcons.Settings, contentDescription = stringResource(R.string.settings_title),
            tint = palette.slate, modifier = Modifier.size(22.dp).padding(start = 8.dp).clickable { onOpenSettings() })
    }
}
```
Add `val Settings: ImageVector = Icons.Outlined.Settings` to `SakuIcons` (requires `androidx.compose.material.icons.outlined.Settings` import).

In `TransactionListScreen.kt` — add `onOpenSettings: () -> Unit` and gear next to title:
```kotlin
Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(stringResource(R.string.transactions_title), style = type.headlineLg, color = palette.ink, modifier = Modifier.weight(1f))
    Icon(SakuIcons.Settings, contentDescription = stringResource(R.string.settings_title),
        tint = palette.slate, modifier = Modifier.size(22.dp).clickable { onOpenSettings() })
}
```

In `App.kt` `composable(Routes.DASHBOARD)` and `composable(Routes.TRANSACTIONS)` — pass `onOpenSettings = { navController.navigate(Routes.SETTINGS) }`.

- [ ] **Step 4: Build to verify**

Run: `./gradlew :androidApp:assembleDebug --console=plain 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL (placeholders for export/import routes compile as empty composables or temporarily route to `SettingsRoute`).

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/main/res/values/strings.xml androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/settings/SettingsScreen.kt androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/navigation/App.kt androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/dashboard/DashboardScreen.kt androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/transactions/TransactionListScreen.kt androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/designsystem/Icons.kt
git commit -m "feat(ui): add settings screen and routes"
```

---

### Task 9: Export ViewModel + screen

**Files:**
- Create: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/ui/importexport/ExportCsvViewModel.kt`
- Create: `androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/importexport/ExportCsvScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/di/AppModule.kt`
- Modify: `androidApp/src/main/kotlin/com/septaalfauzan/saku/di/AndroidAppModule.kt`
- Modify: `androidApp/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `ExportTransactions` (Task 7).
- Produces: `ExportCsvViewModel` with `val categories: StateFlow<List<Category>>`, `val monthLabels: List<String>` (or expose filter state): `data class ExportFilterState(dateStart: LocalDate?, dateEnd: LocalDate?, type: TransactionType?, categoryId: String?)`, `fun setDateRange(start: LocalDate?, end: LocalDate?)`, `fun setType(type: TransactionType?)`, `fun setCategory(id: String?)`, `suspend fun buildCsv(): ExportTransactions.Result?` (null when date range invalid).

- [ ] **Step 1: Add strings**

```xml
    <string name="export.title">Ekspor Transaksi</string>
    <string name="export.date_range">Rentang Tanggal</string>
    <string name="export.type">Tipe</string>
    <string name="export.category">Kategori</string>
    <string name="export.all">Semua</string>
    <string name="export.export_button">Ekspor CSV</string>
    <string name="export.done_title">Export Selesai</string>
    <string name="export.done_body">%1$d transaksi diekspor.</string>
    <string name="export.excluded_note">%1$d transaksi transfer tidak disertakan.</string>
```

- [ ] **Step 2: Write the ViewModel**

```kotlin
package com.septaalfauzan.saku.ui.importexport

import com.septaalfauzan.saku.domain.importexport.ExportTransactions
import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class ExportFilterState(
    val categories: List<Category> = emptyList(),
    val dateStart: String? = null,
    val dateEnd: String? = null,
    val type: TransactionType? = null,
    val categoryId: String? = null,
)

class ExportCsvViewModel(
    private val exportTransactions: ExportTransactions,
) : KoinComponent {
    val uiState: StateFlow<ExportFilterState> = MutableStateFlow(ExportFilterState(emptyList()))

    init { refresh() }

    fun refresh() {
        launch {
            val categories = exportTransactions.transactionRepository.observeCategories().first()
            uiState.value = uiState.value.copy(categories = categories)
        }
    }
    // ...
}
```

**Replace** the above sketch with a real implementation that:
- exposes `val uiState: StateFlow<ExportUiState>` where `ExportUiState(categories: List<Category>, dateStart: LocalDate?, dateEnd: LocalDate?, type: TransactionType?, categoryId: String?, building: Boolean = false, message: String?)`,
- calls `ExportTransactions`'s repo/categories through injected dependencies,
- `fun buildCsv(onBuilt: (ExportTransactions.Result) -> Unit)` runs `exportTransactions.export(ExportFilter(...))` on `viewModelScope`/`Dispatchers.Default` and returns the result to the caller (the screen writes it).

To keep `ExportTransactions` untouched, give `ExportTransactions` a convenience `fun snapshotCategories(): suspend () -> List<Category>` — or simpler: add categories to the `export()` signature: `suspend fun export(filter: ExportFilter): Result` already fetches categories internally via `observeCategories().first()`. For the filter screen, the ViewModel also needs categories — so inject `TransactionRepository` into the ViewModel too:

```kotlin
class ExportCsvViewModel(
    private val exportTransactions: ExportTransactions,
    private val transactionRepository: TransactionRepository,
    coroutineContext: CoroutineContext = Dispatchers.Default,
) {
    private val scope = CoroutineScope(coroutineContext + Job())

    data class UiState(
        val categories: List<Category> = emptyList(),
        val dateStart: kotlinx.datetime.LocalDate? = null,
        val dateEnd: kotlinx.datetime.LocalDate? = null,
        val type: TransactionType? = null,
        val categoryId: String? = null,
        val building: Boolean = false,
        val message: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    init {
        scope.launch {
            _uiState.value = _uiState.value.copy(
                categories = transactionRepository.observeCategories().first(),
            )
        }
    }

    fun setDateRange(start: kotlinx.datetime.LocalDate?, end: kotlinx.datetime.LocalDate?) {
        _uiState.value = _uiState.value.copy(dateStart = start, dateEnd = end)
    }

    fun setType(type: TransactionType?) { _uiState.value = _uiState.value.copy(type = type) }
    fun setCategory(id: String?) { _uiState.value = _uiState.value.copy(categoryId = id) }

    fun buildCsv(onResult: (ExportTransactions.Result) -> Unit) {
        val state = _uiState.value
        if (state.building) return
        _uiState.value = state.copy(building = true, message = null)
        scope.launch {
            val filter = ExportFilter(
                dateStart = state.dateStart?.atStartOfDayIn(TimeZone.currentSystemDefault()),
                dateEnd = state.dateEnd?.atStartOfDayIn(TimeZone.currentSystemDefault())?.plus(1, DateTimeUnit.DAY)
                    ?.minus(1, DateTimeUnit.NANOSECOND),
                type = state.type,
                categoryId = state.categoryId,
            )
            val result = exportTransactions.export(filter)
            _uiState.value = _uiState.value.copy(building = false)
            onResult(result)
        }
    }
}
```

This VM uses `kotlinx.coroutines.launch` on a manually-managed `CoroutineScope` instead of Android `viewModelScope` (shared module has no lifecycle ViewModel base available in commonTest) — the screen should cancel via `DisposableEffect`. To keep it simple and consistent with existing VMs (which use `viewModelScope` from `androidx.lifecycle.viewmodel`), check how `DashboardViewModel` creates scope; mirror it. Add `transactionRepository` + categories import wiring to `AppModule.kt`:

```kotlin
viewModel { ExportCsvViewModel(get(), get()) }
```

- [ ] **Step 3: Write the screen**

`ExportCsvScreen.kt` — full flow:
- Gear-back header "Ekspor Transaksi".
- Date range: two pill buttons (Start / End) — v1 uses `DatePickerDialog`? The project has no date picker dependency visible; simplest v1: two `FilterChip`s toggling "All time" vs "This month"/"Last 3 months" presets; or two text date inputs. Cleanest minimal v1 that satisfies FR-2 without a picker dependency: presets. Implement presets `All time / This month / This year` mapping to `LocalDate` range; plus type chips (Semua/Pemasukan/Pengeluaran); plus single-select category chips. CTA `Ekspor CSV` → `rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> viewModel.buildCsv { result -> uri?.let { contentResolver.openOutputStream(it)?.write("\uFEFF".toByteArray(Charsets.UTF_8) + result.csv.toByteArray(Charsets.UTF_8)); showDone(result) } } }`.
- Done state: replace CTA area with "Export Selesai · N transaksi diekspor" + excluded note.

The screen code is the last big UI deliverable; keep it in the app's design system (use `PillButton`, `FilterChip`, `LabelCaps`, `SakuTheme`). Verify `CreateDocument` needs a `List<String>` MIME arg (use `arrayOf("text/*")`).

- [ ] **Step 4: Build + verify**

Run: `./gradlew :androidApp:assembleDebug --console=plain 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL. Run `./gradlew :shared:testAndroidHostTest --console=plain` — still green.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/septaalfauzan/saku/ui/importexport/ExportCsvViewModel.kt androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/importexport/ExportCsvScreen.kt shared/src/commonMain/kotlin/com/septaalfauzan/saku/di/AppModule.kt androidApp/src/main/kotlin/com/septaalfauzan/saku/di/AndroidAppModule.kt androidApp/src/main/res/values/strings.xml
git commit -m "feat(ui): add export flow with filters"
```

---

### Task 10: Import ViewModel + screen flow

**Files:**
- Create: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/ui/importexport/ImportCsvViewModel.kt`
- Create: `androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/importexport/ImportCsvScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/di/AppModule.kt`
- Modify: `androidApp/src/main/kotlin/com/septaalfauzan/saku/di/AndroidAppModule.kt`
- Modify: `androidApp/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `ImportTransactions` (Task 7), `ApplyResult`, `UndoSnapshot`, `Classification`, `ParsedRows`.
- Produces: `ImportCsvViewModel` with:
  - `sealed interface UiState { data object Idle; data class Parsing(val fileName: String?); data class Preview(val fileName: String?, val summary: PreviewSummary); data class Applying(val summary: PreviewSummary); data class Done(val newCount: Int, val updateCount: Int, val skippedDuplicates: Int); data class Failed(val message: String) }`
  - `data class PreviewSummary(new: Int, update: Int, duplicates: Int, errors: List<CsvError>, sample: List<TransactionDraft> = emptyList(), val blocked: Boolean = errors.isNotEmpty())`
  - `fun onFilePicked(name: String, bytes: ByteArray)` — parse + classify on `Dispatchers.Default`.
  - `fun setSkipDuplicates(skip: Boolean)`.
  - `fun confirmImport()` — applies non-duplicate actions, stores snapshot, sets `Done`.
  - `fun undo()` — calls `importTransactions.undo(snapshot)`.
  - `fun reset()`.

- [ ] **Step 1: Add strings**

```xml
    <string name="import.title">Impor Transaksi</string>
    <string name="import.select">Pilih File CSV</string>
    <string name="import.select_body">Mendukung format ekspor Saku (.csv)</string>
    <string name="import.preview_title">Pratinjau Impor</string>
    <string name="import.new">Baru</string>
    <string name="import.update">Diperbarui</string>
    <string name="import.duplicate">Duplikat</string>
    <string name="import.error">Error</string>
    <string name="import.import_button">Impor %1$d Transaksi</string>
    <string name="import.blocked_body">Perbaiki file CSV, lalu coba lagi. Tidak ada yang diimpor.</string>
    <string name="import.skip_duplicates">Lewati duplikat</string>
    <string name="import.done_title">Impor Selesai</string>
    <string name="import.done_body">%1$d baru, %2$d diperbarui%3$s</string>
    <string name="import.undo">Batalkan Impor</string>
    <string name="import.failed">Gagal membaca file</string>
    <string name="import.no_file">File tidak terbaca</string>
    <string name="import.processing">Memproses…</string>
```

- [ ] **Step 2: Write the ViewModel**

```kotlin
package com.septaalfauzan.saku.ui.importexport

import com.septaalfauzan.saku.domain.importexport.ApplyResult
import com.septaalfauzan.saku.domain.importexport.CsvError
import com.septaalfauzan.saku.domain.importexport.ImportAction
import com.septaalfauzan.saku.domain.importexport.ImportTransactions
import com.septaalfauzan.saku.domain.importexport.TransactionDraft
import com.septaalfauzan.saku.domain.importexport.UndoSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ImportCsvViewModel(
    private val importTransactions: ImportTransactions,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    sealed interface UiState {
        data object Idle : UiState
        data class Parsing(val fileName: String?) : UiState
        data class Preview(
            val fileName: String?,
            val newCount: Int,
            val updateCount: Int,
            val duplicateCount: Int,
            val errors: List<CsvError>,
            val sample: List<TransactionDraft>,
            val skipDuplicates: Boolean = true,
        ) : UiState {
            val blocked: Boolean get() = errors.isNotEmpty()
            val importableCount: Int
                get() = if (skipDuplicates) newCount + updateCount else newCount + updateCount + duplicateCount
        }
        data class Applying(val previous: Preview) : UiState
        data class Done(val newCount: Int, val updateCount: Int, val skippedDuplicates: Int) : UiState {
            var snapshot: UndoSnapshot? = null
        }
        data class Failed(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private var lastActions: List<ImportAction> = emptyList()

    fun onFilePicked(name: String?, bytes: ByteArray) {
        _uiState.value = UiState.Parsing(name)
        scope.launch {
            runCatching {
                val text = bytes.toString(Charsets.UTF_8)
                val parsed = importTransactions.parse(text)
                val classification = if (parsed.errors.isEmpty()) {
                    importTransactions.classify(parsed.drafts)
                } else {
                    com.septaalfauzan.saku.domain.importexport.Classification(emptyList())
                }
                lastActions = classification.actions
                UiState.Preview(
                    fileName = name,
                    newCount = classification.newCount,
                    updateCount = classification.updateCount,
                    duplicateCount = classification.duplicateCount,
                    errors = parsed.errors,
                    sample = parsed.drafts.take(20),
                )
            }.onSuccess { _uiState.value = it }
                .onFailure { _uiState.value = UiState.Failed(it.message ?: "Unknown error") }
        }
    }

    fun setSkipDuplicates(skip: Boolean) {
        val s = _uiState.value as? UiState.Preview ?: return
        _uiState.value = s.copy(skipDuplicates = skip)
    }

    fun confirmImport() {
        val preview = _uiState.value as? UiState.Preview ?: return
        if (preview.blocked) return
        _uiState.value = UiState.Applying(preview)
        scope.launch {
            val actions = if (preview.skipDuplicates) {
                lastActions.filterNot { it is ImportAction.DuplicateAction }
            } else {
                lastActions
            }
            runCatching {
                val result: ApplyResult = importTransactions.apply(actions)
                val skipped = if (preview.skipDuplicates) preview.duplicateCount else 0
                val done = UiState.Done(result.newCount, result.updateCount, skipped)
                done.snapshot = result.snapshot
                done
            }.onSuccess { _uiState.value = it }
                .onFailure { _uiState.value = UiState.Failed(it.message ?: "Unknown error") }
        }
    }

    fun undo() {
        val done = _uiState.value as? UiState.Done ?: return
        val snapshot = done.snapshot ?: return
        scope.launch {
            importTransactions.undo(snapshot)
            _uiState.value = UiState.Idle
        }
    }

    fun reset() {
        lastActions = emptyList()
        _uiState.value = UiState.Idle
    }
}
```

Note: `Done.snapshot` as `var` on a data object is mutable state; prefer `data class Done(..., val snapshot: UndoSnapshot?)` — write it that way; the sketch above is illustrative. Mirror existing ViewModels' scope setup (`viewModelScope` if available in commonMain, else a manually-scoped `CoroutineScope` canceled on clear — simplest: `SupervisorJob()` scope; acceptable for this feature).

Register in `AppModule.kt`: `viewModel { ImportCsvViewModel(get()) }`.

- [ ] **Step 3: Write the screen**

`ImportCsvScreen.kt` — flow:
- Idle: drop-zone card (dashed/rounded box, cloud icon via `SakuIcons` → use `Icons.Outlined.FileOpen`; add to `SakuIcons`), two lines, tap → `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> ... read bytes ... viewModel.onFilePicked(name, bytes) }`; launch with `arrayOf("text/*", "text/csv", "text/comma-separated-values")`.
- Parsing: centered progress `CircularProgressIndicator` + `<import.processing>`.
- Preview: counts row (New/Diperbarui/Duplikat/Error), sample rows list (merchant + date + amount), errors list (`Row · Field · Reason`), if blocked show `<import.blocked_body>` and disabled CTA; else skip-duplicates `FilterChip` + CTA `Impor N`.
- Applying: progress.
- Done: summary + `<import.undo>` button (`PillButton`) + back.
- Failed: message + back.

Close over `contentResolver` via `LocalContext.current.contentResolver`. Read bytes:
```kotlin
val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
```
MIME for CreateDocument in export used `"text/csv"`.

- [ ] **Step 4: Build + verify**

Run: `./gradlew :androidApp:assembleDebug --console=plain 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL. Full host tests still green: `./gradlew :shared:testAndroidHostTest --console=plain`.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/septaalfauzan/saku/ui/importexport/ImportCsvViewModel.kt androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/importexport/ImportCsvScreen.kt shared/src/commonMain/kotlin/com/septaalfauzan/saku/di/AppModule.kt androidApp/src/main/kotlin/com/septaalfauzan/saku/di/AndroidAppModule.kt androidApp/src/main/res/values/strings.xml
git commit -m "feat(ui): add import flow with preview, apply and undo"
```

---

### Task 11: DI graph + final verification

**Files:**
- Modify: `shared/src/androidHostTest/kotlin/com/septaalfauzan/saku/di/AppModuleTest.kt`
- Modify: `androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/navigation/App.kt` (if placeholder routes remain)

**Interfaces:**
- Consumes: everything.

- [ ] **Step 1: Fix the DI test if it breaks**

`AppModuleTest.kt` likely asserts `RoomTransactionRepository` is injectable. Add coverage for the new graph:
```kotlin
@Test
fun importExportGraphResolves() {
    ...
    assertNotNull(koin.get<ExportTransactions>())
    assertNotNull(koin.get<ImportTransactions>())
}
```
(Adjust to whatever assertion style `AppModuleTest` uses.)

- [ ] **Step 2: Replace route placeholders**

If Tasks 8-10 left placeholder composables for `Routes.EXPORT`/`Routes.IMPORT`, point them at `ExportCsvRoute`/`ImportCsvRoute`.

- [ ] **Step 3: Full verification**

Run: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug --console=plain 2>&1 | tail -25`
Expected: BUILD SUCCESSFUL, all tests pass.

Run lint: `./gradlew :androidApp:lintDebug --console=plain 2>&1 | tail -15` — fix any new issues introduced by these changes (unused strings, missing translations not applicable since Indonesian-only).

- [ ] **Step 4: Manual smoke test (device/simulator)**

1. Dashboard → gear → Settings → Export → CTA → pick location → open file in Numbers/Sheets: header + rows correct, non-ASCII merchants intact.
2. Edit a row's amount; Settings → Import → pick the file → preview shows 1 update; confirm; detail shows new amount.
3. Introduce a bad row (Type=Transfer) → confirm button disabled; fix file → retry → imports.
4. Import → immediately tap Undo → rows reverted.

- [ ] **Step 5: Commit**

```bash
git add shared/src/androidHostTest/kotlin/com/septaalfauzan/saku/di/AppModuleTest.kt androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/navigation/App.kt
git commit -m "test: verify import/export DI graph and finalize wiring"
```

---

## Self-Review

Spec coverage:

| Spec section | Plan task(s) |
|---|---|
| §1 CSV writer/parser | T1, T2 |
| dates `yyyy-MM-dd HH:mm:ss` + fallbacks | T3 |
| column mapper + display legends | T4 |
| validation + `CsvError(row, field, reason)` + any-error-blocks-file | T5 (+ T10 blocked flag) |
| DAO `getAll` + `withWriteTransaction` apply + undo | T6 |
| ExportTransactions + filters + transfer exclusion | T7 |
| ImportTransactions parse/classify/apply/undo + duplicates | T7 |
| Settings screen + routes + gear entries | T8 |
| Export flow + filter sheet + BOM write | T9 |
| Import flow (pick→preview→apply→done→undo) | T10 |
| DI + tests + lint + manual smoke | T11 |
| iOS bridge | Deferred (global constraint) — flagged to user |

Placeholder scan: the only acknowledged placeholder is `ExportCsvViewModel` (Task 9) which instructs the implementer to mirror existing ViewModel scope conventions and complete `buildCsv`; the `ImportCsvViewModel` sketch in Task 10 notes `Done.snapshot` should be a val on the data class, not a mutating var — made explicit inline.

Type consistency: `CsvError(row:int, field:String, reason:String)` used identically in validator (T5), pipeline (T7), VMs (T9-10). `TransactionDraft.toTransaction(now)` produced in T4, consumed in T7. `ApplyResult(newCount, updateCount, snapshot)` and `UndoSnapshot(newIds, previousById)` consistent between T4/T6/T7/T10. `ExportFilter(dateStart, dateEnd, type, categoryId)` consistent between T4 (model) and T7/T9 (use).

Known follow-ups (out of scope, not in this plan): iOS file bridge, `values-en` locale, category-remap UI, downloadable errors.csv.