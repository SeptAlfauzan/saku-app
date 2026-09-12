package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.ParserKeyword
import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository

class AddNotificationSource(private val repository: NotificationSettingsRepository) {
    suspend operator fun invoke(
        packageName: String,
        providerId: String,
        keywords: List<ParserKeyword>,
    ) {
        val source = NotificationSource(
            packageName = packageName,
            providerId = providerId,
            enabled = true,
        )
        repository.addSource(source, keywords)
    }
}