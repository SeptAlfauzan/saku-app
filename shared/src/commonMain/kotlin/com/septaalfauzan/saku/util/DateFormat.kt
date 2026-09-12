package com.septaalfauzan.saku.util

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

private val indonesianMonthAbbr = mapOf(
    1 to "Jan", 2 to "Feb", 3 to "Mar", 4 to "Apr", 5 to "Mei", 6 to "Jun",
    7 to "Jul", 8 to "Agu", 9 to "Sep", 10 to "Okt", 11 to "Nov", 12 to "Des",
)

private val englishMonthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

private val monthAbbr3 = mapOf(
    "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
    "may" to 5, "mei" to 5, "jun" to 6, "jul" to 7,
    "aug" to 8, "agu" to 8, "sep" to 9, "oct" to 10, "okt" to 10,
    "nov" to 11, "dec" to 12, "des" to 12,
)

fun formatShortDate(millis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone)
    val month = indonesianMonthAbbr.getValue(local.month.number)
    return "${local.day} $month ${local.year}"
}

fun monthLabel(year: Int, monthNumber: Int): String =
    "${englishMonthNames[monthNumber - 1]} $year"

fun parseReceiptDate(
    dateText: String,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Instant? {
    val parts = dateText.trim()
        .split(' ', '-', '/')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (parts.size != 3) return null

    fun toInstant(day: Int, month: Int, year: Int): Instant? = runCatching {
        LocalDate(year, month, day).atStartOfDayIn(zone).toEpochMilliseconds()
    }.getOrNull()?.let { Instant.fromEpochMilliseconds(it) }

    val (a, b, c) = parts

    if (parts.any { it.any { ch -> ch.isLetter() } }) {
        val monthWord = b.lowercase().take(3)
        val month = monthAbbr3[monthWord] ?: return null
        val day = a.toIntOrNull() ?: c.toIntOrNull() ?: return null
        val year = c.toIntOrNull() ?: a.toIntOrNull() ?: return null
        return toInstant(day, month, year)
    }

    val numericA = a.toIntOrNull()
    if (numericA == null) return null

    val separator = dateText.firstOrNull { it == '-' || it == '/' } ?: ' '
    val yearFirst = when (separator) {
        '-' -> dateText.indexOf('-') > 2
        '/' -> dateText.indexOf('/') > 2
        else -> false
    }
    if (yearFirst) {
        val year = a.toIntOrNull() ?: return null
        val month = b.toIntOrNull() ?: return null
        val day = c.toIntOrNull() ?: return null
        return toInstant(day, month, year)
    }
    val day = a.toIntOrNull() ?: return null
    val month = b.toIntOrNull() ?: return null
    val year = c.toIntOrNull() ?: return null
    return toInstant(day, month, year)
}
