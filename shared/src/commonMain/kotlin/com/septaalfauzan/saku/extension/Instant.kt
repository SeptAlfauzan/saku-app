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