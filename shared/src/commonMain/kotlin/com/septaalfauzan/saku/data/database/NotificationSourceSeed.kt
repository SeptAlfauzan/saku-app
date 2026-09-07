package com.septaalfauzan.saku.data.database

import com.septaalfauzan.saku.domain.model.NotificationSource

object NotificationSourceSeed {
    const val KEY_TRACKING_ENABLED = "notification_tracking_enabled"
    const val KEY_AUTO_CONFIRM = "auto_confirm"

    val sources: List<NotificationSource> = listOf(
        NotificationSource(packageName = "com.bca", providerId = "bca", enabled = true),
        NotificationSource(packageName = "com.gojek.app", providerId = "gopay", enabled = true),
        NotificationSource(packageName = "com.ovo.id", providerId = "ovo", enabled = true),
        NotificationSource(packageName = "id.dana", providerId = "dana", enabled = true),
    )
}
