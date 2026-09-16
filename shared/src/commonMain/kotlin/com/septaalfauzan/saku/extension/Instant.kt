package com.septaalfauzan.saku.extension

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun Instant.isToday(): Boolean {
    val timeZone = TimeZone.currentSystemDefault()

    val instantDate = this
        .toLocalDateTime(timeZone)
        .date

    val today = kotlin.time.Clock.System.now()
        .toLocalDateTime(timeZone)
        .date

    return instantDate == today
}

fun Instant.toDateString(): String {
    val dateTime = toLocalDateTime(TimeZone.currentSystemDefault())

    return "${dateTime.year}-" +
            "${(dateTime.month.ordinal + 1).toString().padStart(2, '0')}-" +
            dateTime.day.toString().padStart(2, '0')
}

fun Instant.toTimeString(): String {
    val dateTime = toLocalDateTime(TimeZone.currentSystemDefault())

    return "${dateTime.hour.toString().padStart(2, '0')}:" +
            dateTime.minute.toString().padStart(2, '0')
}