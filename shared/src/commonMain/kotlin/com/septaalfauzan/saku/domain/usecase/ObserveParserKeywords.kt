package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.model.ParserKeyword
import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveParserKeywords(private val repository: NotificationSettingsRepository) {
    operator fun invoke(packageName: String): Flow<List<ParserKeyword>> =
        repository.observeKeywords(packageName)
}