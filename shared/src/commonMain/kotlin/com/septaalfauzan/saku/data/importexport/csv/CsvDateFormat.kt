package com.septaalfauzan.saku.data.importexport.csv

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.*
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val fullFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    time(LocalTime.Formats.ISO)
}
private val minuteFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    hour()
    char(':')
    minute()
}
private val dateFormat = LocalDate.Format { date(LocalDate.Formats.ISO) }

fun formatCsvDateTime(
    millis: Long,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val local = kotlinx.datetime.Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone)
    return fullFormat.format(local)
}

fun parseCsvDateTime(
    value: String,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Instant? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    val kotlinxInstant = when {
        trimmed.contains(' ') && trimmed.count { it == ':' } >= 2 ->
            runCatching { LocalDateTime.parse(trimmed, fullFormat).toInstant(zone) }.getOrNull()
        trimmed.contains(' ') ->
            runCatching { LocalDateTime.parse(trimmed, minuteFormat).toInstant(zone) }.getOrNull()
        else ->
            runCatching { LocalDate.parse(trimmed, dateFormat).atStartOfDayIn(zone) }.getOrNull()
    }
    return kotlinxInstant?.toEpochMilliseconds()?.let { Instant.fromEpochMilliseconds(it) }
}
