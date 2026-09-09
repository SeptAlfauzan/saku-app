package com.septaalfauzan.saku.notification.provider

import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.notification.model.ParsedTransaction

interface NotificationParser {
    fun canParse(data: NotificationData): Boolean
    fun parse(data: NotificationData): ParsedTransaction?
}
