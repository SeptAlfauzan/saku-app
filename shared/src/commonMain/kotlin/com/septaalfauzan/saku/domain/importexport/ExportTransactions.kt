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
    data class Result(val csv: String, val exportedCount: Int, val excludedTransfers: Int)

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
