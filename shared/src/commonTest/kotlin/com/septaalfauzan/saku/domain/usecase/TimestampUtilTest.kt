package com.septaalfauzan.saku.domain.usecase

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class TimestampUtilTest {

    @Test
    fun startOfMonthEpochIsFirstDayMidnight() {
        val start = startOfMonthMillis(2026, 9, TimeZone.UTC)
        // 2026-09-01T00:00:00Z
        assertEquals(1_788_220_800_000L, start)
    }

    @Test
    fun endOfMonthExcludesFirstOfNextMonth() {
        val start = startOfMonthMillis(2026, 9, TimeZone.UTC)
        val end = endOfMonthMillis(2026, 9, TimeZone.UTC)
        assertEquals(30, ((end - start) / 86_400_000L) + 1)
    }
}