package com.septaalfauzan.saku.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DateFormatTest {

    private val sep06 = 1_788_652_800_000L

    @Test
    fun formatsIndonesianShortDate() {
        assertEquals("6 Sep 2026", formatShortDate(sep06, TimeZone.UTC))
    }

    @Test
    fun respectsTimeZoneDateShift() {
        val late = 1_788_737_400_000L
        assertEquals("7 Sep 2026", formatShortDate(late, TimeZone.of("Asia/Jakarta")))
        assertEquals("6 Sep 2026", formatShortDate(late, TimeZone.UTC))
    }

    @Test
    fun parsesIsoDate() {
        assertEquals(sep06, parseReceiptDate("2026-09-06", TimeZone.UTC)?.toEpochMilliseconds())
    }

    @Test
    fun parsesSlashedAndDashedDateOrder() {
        assertEquals(sep06, parseReceiptDate("06/09/2026", TimeZone.UTC)?.toEpochMilliseconds())
        assertEquals(sep06, parseReceiptDate("06-09-2026", TimeZone.UTC)?.toEpochMilliseconds())
        assertEquals(sep06, parseReceiptDate("2026/09/06", TimeZone.UTC)?.toEpochMilliseconds())
    }

    @Test
    fun parsesEnglishAndIndonesianMonthNames() {
        assertEquals(sep06, parseReceiptDate("6 Sep 2026", TimeZone.UTC)?.toEpochMilliseconds())
        assertEquals(sep06, parseReceiptDate("6 September 2026", TimeZone.UTC)?.toEpochMilliseconds())
        assertEquals(1_796_515_200_000L, parseReceiptDate("6 Des 2026", TimeZone.UTC)?.toEpochMilliseconds())
        val aug06 = LocalDate(2026, 8, 6).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        assertEquals(aug06, parseReceiptDate("6 Agu 2026", TimeZone.UTC)?.toEpochMilliseconds())
    }

    @Test
    fun rejectsUnparseableDates() {
        assertNull(parseReceiptDate("not a date", TimeZone.UTC))
        assertNull(parseReceiptDate("", TimeZone.UTC))
        assertNull(parseReceiptDate("2026-13-40", TimeZone.UTC))
    }
}
