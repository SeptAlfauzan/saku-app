package com.septaalfauzan.saku.notification

import android.app.Notification
import android.service.notification.StatusBarNotification
import android.util.Log
import com.septaalfauzan.saku.notification.model.NotificationData
import kotlin.time.Instant

object NotificationMapper {
    fun map(sbn: StatusBarNotification): NotificationData {
        val extras = sbn.notification?.extras
        val textLines = extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        val body = buildString {
            extras?.getCharSequence(Notification.EXTRA_TEXT)?.let { append(it).append('\n') }
            extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let {
                if (isNotEmpty()) append('\n')
                append(it)
            }
            if (textLines != null) {
                for (line in textLines) append(line.toString()).append('\n')
            }
        }.trim().ifBlank { null }
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val notificationData = NotificationData(
            packageName = sbn.packageName,
            title = title,
            body = body,
            postedAt = Instant.fromEpochMilliseconds(sbn.postTime),
            notificationId = sbn.id,
        )

        return notificationData
    }
}