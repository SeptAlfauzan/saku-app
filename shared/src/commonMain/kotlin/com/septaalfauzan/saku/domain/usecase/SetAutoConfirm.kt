package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository

class SetAutoConfirm(private val repository: NotificationSettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setAutoConfirm(enabled)
}
