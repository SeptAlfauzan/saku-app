package com.septaalfauzan.saku.util

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatTest {

    // 2026-09-06T00:00:00Z
    private val sep06 = 1_788_652_800_000L

    @Test
    fun formatsIndonesianShortDate() {
        assertEquals("6 Sep 2026", formatShortDate(sep06, TimeZone.UTC))
    }

    @Test
    fun respectsTimeZoneDateShift() {
        // 2026-09-06T23:30:00Z is 2026-09-07 in UTC+7
        val late = 1_788_737_400_000L
        assertEquals("7 Sep 2026", formatShortDate(late, TimeZone.of("Asia/Jakarta")))
        assertEquals("6 Sep 2026", formatShortDate(late, TimeZone.UTC))
    }
}
