package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveAutoConfirm(private val repository: NotificationSettingsRepository) {
    operator fun invoke(): Flow<Boolean> = repository.observeAutoConfirm()
}
