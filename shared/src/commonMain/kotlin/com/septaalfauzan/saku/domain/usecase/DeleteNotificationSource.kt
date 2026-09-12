package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository

class DeleteNotificationSource(private val repository: NotificationSettingsRepository) {
    suspend operator fun invoke(packageName: String) = repository.deleteSource(packageName)
}