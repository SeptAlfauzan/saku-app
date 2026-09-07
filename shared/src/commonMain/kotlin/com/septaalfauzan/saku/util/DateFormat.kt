package com.septaalfauzan.saku.util

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

private val indonesianMonthAbbr = mapOf(
    1 to "Jan", 2 to "Feb", 3 to "Mar", 4 to "Apr", 5 to "Mei", 6 to "Jun",
    7 to "Jul", 8 to "Agu", 9 to "Sep", 10 to "Okt", 11 to "Nov", 12 to "Des",
)

fun formatShortDate(millis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone)
    val month = indonesianMonthAbbr.getValue(local.month.number)
    return "${local.day} $month ${local.year}"
}
