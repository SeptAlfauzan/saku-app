package com.septaalfauzan.saku.domain.importexport

import com.septaalfauzan.saku.data.importexport.csv.CsvParser
import com.septaalfauzan.saku.data.importexport.csv.TransactionCsvMapper
import com.septaalfauzan.saku.data.importexport.csv.TransactionCsvValidator
import com.septaalfauzan.saku.domain.model.Transaction
import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import kotlin.time.Clock
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

    private fun isSameTransaction(tx: Transaction, draft: TransactionDraft): Boolean {
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
