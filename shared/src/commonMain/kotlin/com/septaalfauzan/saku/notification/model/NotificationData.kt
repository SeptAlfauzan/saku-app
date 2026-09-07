package com.septaalfauzan.saku.notification.model

import kotlin.time.Instant

data class NotificationData(
    val packageName: String,
    val title: String?,
    val body: String?,
    val postedAt: Instant,
    val notificationId: Int?,
)
