package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository

class UpdateParserKeywords(private val repository: NotificationSettingsRepository) {
    suspend operator fun invoke(
        packageName: String,
        expenseWords: List<String>,
        incomeWords: List<String>,
        merchantWords: List<String>,
    ) = repository.upsertKeywords(packageName, expenseWords, incomeWords, merchantWords)
}