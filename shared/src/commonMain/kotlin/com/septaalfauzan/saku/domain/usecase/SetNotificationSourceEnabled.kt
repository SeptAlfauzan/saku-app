package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository

class SetNotificationSourceEnabled(private val repository: NotificationSettingsRepository) {
    suspend operator fun invoke(packageName: String, enabled: Boolean) = repository.setSourceEnabled(packageName, enabled)
}
