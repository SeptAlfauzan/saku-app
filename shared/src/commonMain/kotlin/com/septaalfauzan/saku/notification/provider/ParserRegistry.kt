package com.septaalfauzan.saku.notification.provider

import com.septaalfauzan.saku.notification.generic.GenericNotificationParser
import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.notification.model.ParsedTransaction

class ParserRegistry(
    private val providerParsers: List<NotificationParser>,
    private val fallback: NotificationParser = GenericNotificationParser(),
) {
    fun parse(data: NotificationData): ParsedTransaction? =
        providerParsers.firstOrNull { it.canParse(data) }?.parse(data) ?: fallback.parse(data)
}
