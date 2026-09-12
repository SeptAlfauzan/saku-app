# Design: Track transaction source for scanned receipts

Date: 2026-09-12

## Problem

Receipt scans create transactions, but the DB cannot distinguish them from
manually entered ones. `ScannerViewModel.approve()` builds its transaction
through `AddTransaction`, which hardcodes `source = TransactionSource.MANUAL`
(`shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/usecase/AddTransaction.kt:32`).

Notification-derived transactions already carry a tracked source
(`TransactionSource.NOTIFICATION`, set in
`shared/src/commonMain/kotlin/com/septaalfauzan/saku/notification/usecase/ProcessNotificationUseCase.kt:68`).
Scan receipts should do the same.

## Approach

Add a new `TransactionSource.SCAN` value and let the caller choose the source
when building a transaction via `AddTransaction`.

## Design

### 1. Model — new source value

`shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/model/TransactionSource.kt`:

```kotlin
enum class TransactionSource { MANUAL, NOTIFICATION, SCAN }
```

`source` is stored as a plain `TEXT` column
(`shared/src/commonMain/kotlin/com/septaalfauzan/saku/data/entity/TransactionEntity.kt:20`),
so adding an enum value requires no DB migration. Entity and migration files
remain untouched.

### 2. Usecase — caller chooses source

In `AddTransaction` (`AddTransaction.kt:14`), add optional parameters and use
them in place of the hardcoded values at lines 32-33:

```kotlin
operator fun invoke(
    type: TransactionType,
    amount: Long,
    currency: String = "IDR",
    merchant: String?,
    categoryId: String?,
    description: String?,
    occurredAt: Instant,
    source: TransactionSource = TransactionSource.MANUAL,
    sourcePackage: String? = null,
): Transaction
```

Defaults keep all existing callers (manual add, add/edit screen) unchanged.

### 3. Scanner — mark transactions as scanned

In `ScannerViewModel.approve()`
(`androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerViewModel.kt:73`),
pass `source = TransactionSource.SCAN` to `addTransaction`. `sourcePackage`
stays `null`. Type stays `EXPENSE`, status stays `CONFIRMED`; both unchanged
from today's behavior.

### 4. Edit path — no change

`AddEditTransactionViewModel` already preserves the existing source when
updating (`AddEditTransactionViewModel.kt:176`), so editing a scanned
transaction keeps `SCAN`.

## Error handling

None new. `approve()` already catches exceptions and surfaces a message on
`ScannerUiState.message` (`ScannerViewModel.kt:83-87`).

## Testing

- `ModelSerializationTest`: add a `SCAN` round-trip case.
- `AddTransaction` test: invoking with `source = TransactionSource.SCAN`
  produces a transaction whose `source == TransactionSource.SCAN` and
  `sourcePackage == null`.

## Out of scope

- Duplicate detection for scans
- Source-filter UI
- Income toggle on the scanner result screen

These can be separate follow-ups.