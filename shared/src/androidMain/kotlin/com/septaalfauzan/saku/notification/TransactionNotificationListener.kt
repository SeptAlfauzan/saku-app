package com.septaalfauzan.saku.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.septaalfauzan.saku.notification.usecase.ProcessNotificationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TransactionNotificationListener : NotificationListenerService(), KoinComponent {

    private val processNotification: ProcessNotificationUseCase by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val data = NotificationMapper.map(sbn)
        scope.launch {
            processNotification(data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}