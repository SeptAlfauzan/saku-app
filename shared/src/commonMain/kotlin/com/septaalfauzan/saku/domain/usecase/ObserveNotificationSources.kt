package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveNotificationSources(private val repository: NotificationSettingsRepository) {
    operator fun invoke(): Flow<List<NotificationSource>> = repository.observeSources()
}
