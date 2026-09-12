# Receipt Scanner Integration Design

Date: 2026-09-11

## Goal

Wire the receipt scanner OCR results into the transaction flow:

1. **Approve & Log** saves a transaction directly from the parsed receipt (no review screen).
2. **Edit** opens the Add/Edit screen pre-filled with the parsed receipt data, where the user can review, change the type (default Expense → Income), and save.
3. Fix the captured receipt preview in the Scan Review screen being rotated 90° counter-clockwise.

## Context

- KMP project: `androidApp/` (Compose UI) + `shared/` (domain/data).
- `ScannerViewModel` (androidApp) orchestrates capture → OCR via `GetReceiptValue` → `Receipt` domain model. The Scan Review screen displays the result. `approve()` and `edit()` are currently "Mock only" stubs.
- `AddEditTransactionViewModel` (shared) manages the add/edit form. Its route (`Routes.ADD`, `Routes.EDIT/{id}`) accepts no OCR-derived data, so scanner and form are fully disconnected.
- `BitmapFactory.decodeFile` in the Scan Review ignores JPEG EXIF orientation, producing the 90°-rotated preview.

## Field Mapping (Receipt → Add/Edit form)

| Receipt field | Add/Edit field |
|---|---|
| `total` (Long) | `amountInput` |
| `items` | `note` (see format below) |
| `merchantName` | `merchant` |
| `transactionDate` (String) | `occurredAtMillis` (parsed; fallback now) |
| — (always expense) | `type` = `EXPENSE` when saved directly; user may switch to `INCOME` in the form |

### Item note format

`Receipt.toItemsNote()` (new shared helper) renders items as a newline-separated list:

```
2x Nasi Goreng
Ayam Bakar
```

- Quantity prefix (`Nx`) shown only when quantity > 1.
- Empty receipt → empty note.

### Date parsing

New shared parser `parseReceiptDate(dateText: String): Instant?` in `util/DateFormat.kt`. Supports, in order:

- ISO `yyyy-MM-dd`
- `yyyy/MM/dd`
- `dd/MM/yyyy`
- `dd-MM-yyyy`
- `dd MMMM yyyy` and `dd MMM yyyy` with English month names
- `dd MMM yyyy` with Indonesian abbreviations (Jan, Feb, Mar, Apr, Mei, Jun, Jul, Agu, Sep, Okt, Nov, Des)

On failure, callers fall back to the current time (Add/Edit already defaults `occurredAtMillis` to now; direct save uses now).

## Behavior

### Approve & Log → direct save

`ScannerViewModel` gains dependencies `AddTransaction` and `ObserveCategories` (already registered in shared `AppModule`).

On `approve()`:

1. Take the current `StateUi.Success<Receipt>`; ignore if not in SUCCESS state.
2. Build and store a transaction:
   - `type` = `EXPENSE`
   - `amount` = `receipt.total`
   - `merchant` = `receipt.merchantName` (blank → null)
   - `categoryId` = first category with `id == "other_expense"`, else first EXPENSE category, else null (Room column is nullable; null is acceptable)
   - `description` = `receipt.toItemsNote()` (blank → null)
   - `occurredAt` = parsed date or now
   - `source` = `MANUAL`, `status` = `CONFIRMED`, `confidence` = 0.0
3. Emit `Saved` event through a new `ScannerEvent` state flow.

`ScannerScreen` observes the event and invokes `onBack()`, popping back to the dashboard.

Failure sets a message on `ScannerUiState.message` (existing toast/pill UI) and does not navigate.

### Edit → pre-filled Add/Edit

New serializable shared model `ScanPrefill` (`shared/.../ui/addedit/ScanPrefill.kt`):

```kotlin
@Serializable
data class ScanPrefill(
    val amount: Long,
    val merchant: String,
    val note: String,
    val occurredAtMillis: Long,
)
```

- `Routes.ADD` changes to `"add?prefill={prefill}"`, optional `String` nav arg with default empty string. `Routes.addScan(receipt)` helper in App.kt builds `ScanPrefill` from the receipt and navigates with URL-encoded JSON (`Uri.encode`).
- `ScannerScreen` signature gains `onEdit: (Receipt) -> Unit`. The **Edit** pill calls it with the successful scan result; App.kt encodes and navigates.
- `AddEditRoute` gains `prefillJson: String?` and passes it to the ViewModel via `parametersOf(transactionId, prefillJson)`.
- `AddEditTransactionViewModel` constructor gains `prefillJson: String? = null`. In `init` (when not editing an existing transaction), decode and seed `fieldState`:
  - `amountInput` = `prefill.amount.toString()`
  - `merchant` = `prefill.merchant`
  - `note` = `prefill.note`
  - `occurredAtMillis` = `prefill.occurredAtMillis`
  - `type` stays default `EXPENSE`; the existing segmented Expense/Income selector lets the user switch.
  - `categoryId` = null (user picks a category).
- Koin `AddEditTransactionViewModel` factory updated to resolve both params positionally (`params.getOrNull(0)`, `params.getOrNull(1)`), since two `String?` params are no longer unambiguous.

The existing edit flow (`Routes.EDIT/{id}`) is untouched — it still loads an existing transaction by id.

### Rotated image fix

In `ScannerScreen`, replace `BitmapFactory.decodeFile(path)` (Scan Review phase) with `decodeWithExifRotation(path)`:

1. `BitmapFactory.decodeFile` → original bitmap.
2. Read EXIF orientation via `androidx.exifinterface.media.ExifInterface`.
3. Map `ORIENTATION_ROTATE_90/180/270` (and transpose variants) to degrees.
4. Rotate with `android.graphics.Matrix` + `Bitmap.createBitmap`; recycle the original when a new bitmap is produced.
5. Unrotated images return the original bitmap unchanged.

Add dependency `androidx.exifinterface:exifinterface` to the version catalog and `androidApp/build.gradle.kts`.

## Non-Goals

- No automatic income detection from receipts; everything defaults to Expense.
- No payment-method → category inference.
- No confidence-score mapping from OCR.
- No changes to the review queue, notification pipeline, or manual entry path.

## Files Touched

| File | Change |
|---|---|
| `shared/.../domain/model/Receipt.kt` | Add `toItemsNote()` extension/helper |
| `shared/.../util/DateFormat.kt` | Add `parseReceiptDate()` |
| `shared/.../ui/addedit/ScanPrefill.kt` | New `@Serializable ScanPrefill` |
| `shared/.../ui/addedit/AddEditTransactionViewModel.kt` | Add `prefillJson` constructor param, seed in `init` |
| `shared/.../di/AppModule.kt` | Positional Koin params for AddEditViewModel |
| `androidApp/.../ui/scanner/ScannerViewModel.kt` | Inject `AddTransaction`/`ObserveCategories`; real `approve()`; `ScannerEvent` flow |
| `androidApp/.../ui/scanner/ScannerScreen.kt` | Wire buttons, EXIF-rotated decode, event observation |
| `androidApp/.../ui/navigation/App.kt` | `ADD` route arg, `addScan` helper, `onEdit` wiring |
| `androidApp/.../ui/addedit/AddEditTransactionScreen.kt` | Pass `prefillJson` to ViewModel |
| `gradle/libs.versions.toml` | Add exifinterface version |
| `androidApp/build.gradle.kts` | Add exifinterface dependency |
| `androidApp/.../di/AndroidAppModule.kt` | ScannerViewModel new deps resolvable |

## Verification

- `./gradlew :androidApp:assembleDebug` builds clean.
- Manual: scan a receipt → Approve & Log → transaction appears in list/dashboard as EXPENSE with total, item note, merchant, date.
- Manual: scan → Edit → Add/Edit opens pre-filled; switch to Income and save → income transaction.
- Scan Review preview renders upright (not rotated).
- Process death while on Add/Edit after Edit-from-scanner retains pre-filled data (nav arg survives).