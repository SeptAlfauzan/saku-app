package com.septaalfauzan.saku.notification.provider

import com.septaalfauzan.saku.domain.model.KeywordType
import com.septaalfauzan.saku.domain.model.NotificationSource
import com.septaalfauzan.saku.domain.model.ParserKeyword
import com.septaalfauzan.saku.notification.generic.GenericNotificationParser
import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.notification.model.ParsedTransaction

class ParserRegistry(
    private val fallback: NotificationParser = GenericNotificationParser(),
) {
    private var parsers: Map<String, ConfigurableParser> = emptyMap()

    val size: Int get() = parsers.size

    fun rebuild(
        sources: List<NotificationSource>,
        keywordMap: Map<String, List<ParserKeyword>>,
    ) {
        parsers = sources.filter { it.enabled }.associate { source ->
            val keywords = keywordMap[source.packageName] ?: emptyList()
            source.packageName to ConfigurableParser(
                packageName = source.packageName,
                expenseWords = keywords.filter { it.keywordType == KeywordType.EXPENSE }.map { it.keyword },
                incomeWords = keywords.filter { it.keywordType == KeywordType.INCOME }.map { it.keyword },
                merchantWords = keywords.filter { it.keywordType == KeywordType.MERCHANT }.map { it.keyword },
            )
        }
    }

    fun parse(data: NotificationData): ParsedTransaction? =
        parsers[data.packageName]?.parse(data) ?: fallback.parse(data)
}
