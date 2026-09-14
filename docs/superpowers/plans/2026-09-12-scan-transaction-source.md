# Scan Transaction Source Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Track scanned-receipt transactions under a distinct `SCAN` source so they are distinguishable from `MANUAL` and `NOTIFICATION` transactions in the DB.

**Architecture:** Add a `SCAN` value to the `TransactionSource` enum (plain TEXT column → no migration), let `AddTransaction` accept an optional caller-provided source/sourcePackage (defaulting to today's `MANUAL`/null), and have `ScannerViewModel.approve()` pass `SCAN`.

**Tech Stack:** Kotlin Multiplatform, androidx Room 3, Koin, kotlinx.serialization, kotlin.test (run via `./gradlew :shared:testAndroidHostTest`).

## Global Constraints

- `source` persists as plain TEXT (`TransactionEntity.kt:20`) — no DB schema/migration changes anywhere.
- Defaults of `AddTransaction.invoke` must stay `source = TransactionSource.MANUAL`, `sourcePackage = null` so existing callers are unchanged.
- Scanner-approved transactions stay `type = EXPENSE`, `status = CONFIRMED`, confidence `0.0` — do not change.
- Design spec: `docs/superpowers/specs/2026-09-12-scan-transaction-source-design.md`.
- Test command: `./gradlew :shared:testAndroidHostTest`.
- Commit convention: Conventional Commits, imperative subject, no AI attribution.

---

### Task 1: Add `SCAN` to `TransactionSource`

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/model/TransactionSource.kt:6`
- Test: `shared/src/commonTest/kotlin/com/septaalfauzan/saku/domain/model/ModelSerializationTest.kt`

**Interfaces:**
- Consumes: nothing (existing helper `sampleTransaction()` at `ModelSerializationTest.kt:12`).
- Produces: enum value `TransactionSource.SCAN` (used by Task 2/3).

- [ ] **Step 1: Write the failing test**

Append to `ModelSerializationTest.kt` (after the existing `notificationTransactionSerializesWithStatusAndConfidence` test):

```kotlin
@Test
fun scanTransactionSerializes() {
    val tx = sampleTransaction().copy(source = TransactionSource.SCAN)
    val decoded = json.decodeFromString(Transaction.serializer(), json.encodeToString(Transaction.serializer(), tx))
    assertEquals(tx, decoded)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest`
Expected: FAIL to compile — `Unresolved reference 'SCAN'` for `TransactionSource.SCAN`.

- [ ] **Step 3: Write minimal implementation**

`TransactionSource.kt`:

```kotlin
enum class TransactionSource { MANUAL, NOTIFICATION, SCAN }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest`
Expected: PASS — all tests green.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/model/TransactionSource.kt shared/src/commonTest/kotlin/com/septaalfauzan/saku/domain/model/ModelSerializationTest.kt
git commit -m "feat: add SCAN transaction source"
```

---

### Task 2: Let `AddTransaction` accept caller-chosen source

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/usecase/AddTransaction.kt:14-41`
- Create: `shared/src/commonTest/kotlin/com/septaalfauzan/saku/domain/usecase/AddTransactionTest.kt`

**Interfaces:**
- Consumes: `TransactionRepository` (for the stub), `Transaction`, `TransactionSource`, `TransactionStatus`, `TransactionType` from `domain.model`; `kotlinx.coroutines.flow.flowOf`.
- Produces: `AddTransaction.invoke` now has two extra trailing params `source: TransactionSource = TransactionSource.MANUAL`, `sourcePackage: String? = null`. Note: existing params are non-defaulted (`merchant`, `categoryId`, `description` are required; `currency` and `occurredAt` have defaults), so new params go last after `occurredAt`.

- [ ] **Step 1: Write the failing test**

Create `shared/src/commonTest/kotlin/com/septaalfauzan/saku/domain/usecase/AddTransactionTest.kt`:

```kotlin
package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.model.Category
import com.septaalfauzan.saku.domain.model.DuplicateKey
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.model.TransactionSource
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FakeTransactionRepository : TransactionRepository {
    override fun observeTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
    override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())
    override suspend fun insert(transaction: Transaction) = Unit
    override suspend fun update(transaction: Transaction) = Unit
    override suspend fun delete(id: String) = Unit
    override fun observePending(): Flow<List<Transaction>> = flowOf(emptyList())
    override suspend fun setStatus(id: String, status: TransactionStatus) = Unit
    override suspend fun findRecentDuplicate(
        key: DuplicateKey,
        withinStartMillis: Long,
        withinEndMillis: Long,
    ): Transaction? = null
}

class AddTransactionTest {

    private fun useCase() = AddTransaction(FakeTransactionRepository())

    @Test
    fun defaultSourceIsManual() {
        val tx = useCase().invoke(
            type = TransactionType.EXPENSE,
            amount = 120_000,
            merchant = "Alfamart",
            categoryId = null,
            description = null,
            occurredAt = Clock.System.now(),
        )
        assertEquals(TransactionSource.MANUAL, tx.source)
        assertNull(tx.sourcePackage)
    }

    @Test
    fun passesThroughCallerSourceAndPackage() {
        val tx = useCase().invoke(
            type = TransactionType.EXPENSE,
            amount = 120_000,
            merchant = "Alfamart",
            categoryId = null,
            description = null,
            occurredAt = Clock.System.now(),
            source = TransactionSource.SCAN,
            sourcePackage = "receipt_scan",
        )
        assertEquals(TransactionSource.SCAN, tx.source)
        assertEquals("receipt_scan", tx.sourcePackage)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest`
Expected: FAIL to compile — named args `source`/`sourcePackage` not found on `AddTransaction.invoke`.

- [ ] **Step 3: Write minimal implementation**

`AddTransaction.kt` — change the signature and the `source`/`sourcePackage` assignments:

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
    ): Transaction {
        val now = Clock.System.now()
        val tx = Transaction(
            id = Uuid.random().toString(),
            type = type,
            amount = amount,
            currency = currency,
            merchant = merchant,
            categoryId = categoryId,
            description = description,
            source = source,
            sourcePackage = sourcePackage,
            status = TransactionStatus.CONFIRMED,
            confidence = 0.0,
            occurredAt = occurredAt,
            createdAt = now,
            updatedAt = now,
        )
        return tx
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/septaalfauzan/saku/domain/usecase/AddTransaction.kt shared/src/commonTest/kotlin/com/septaalfauzan/saku/domain/usecase/AddTransactionTest.kt
git commit -m "feat: allow caller to set transaction source in AddTransaction"
```

---

### Task 3: Mark scanner-created transactions as `SCAN`

**Files:**
- Modify: `androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerViewModel.kt:73-80`

**Interfaces:**
- Consumes: `TransactionSource` (Task 1), `AddTransaction` source param (Task 2).
- Produces: no new public API — behavior change only; `approve()` continues to store an EXPENSE/CONFIRMED transaction, now with `source = TransactionSource.SCAN`.

- [ ] **Step 1: Make the edit**

In `ScannerViewModel.kt`, add the import and the `source` argument on the existing `addTransaction(...)` call in `approve()`:

```kotlin
import com.septaalfauzan.saku.domain.model.TransactionSource
```

and inside `approve()`:

```kotlin
                val tx = addTransaction(
                    type = TransactionType.EXPENSE,
                    amount = receipt.total,
                    merchant = receipt.merchantName.ifBlank { null },
                    categoryId = categories.defaultExpenseId(),
                    description = receipt.toItemsNote().ifBlank { null },
                    occurredAt = parseReceiptDate(receipt.transactionDate) ?: Clock.System.now(),
                    source = TransactionSource.SCAN,
                )
```

Leave `sourcePackage` unset (stays `null`). Do not change type, status, or confidence assignment in `AddTransaction`.

- [ ] **Step 2: Verify shared tests still pass**

Run: `./gradlew :shared:testAndroidHostTest`
Expected: PASS — all tests green, no behavior regression.

- [ ] **Step 3: Verify the app still compiles**

Run: `./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/septaalfauzan/saku/ui/scanner/ScannerViewModel.kt
git commit -m "feat: tag scanned receipts with SCAN source"
```

---

## Verification

After all tasks: `./gradlew :shared:testAndroidHostTest` and `./gradlew :androidApp:compileDebugKotlin` both succeed.