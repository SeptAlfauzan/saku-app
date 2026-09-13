package com.septaalfauzan.saku.data.importexport.csv

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CsvDateFormatTest {

    private val zone = TimeZone.UTC

    @Test
    fun formatsIsoDateTimeInLocalZone() {
        val instant = kotlinx.datetime.Instant.parse("2026-01-05T14:00:30Z")
        assertEquals("2026-01-05 14:00:30", formatCsvDateTime(instant.toEpochMilliseconds(), zone))
    }

    @Test
    fun parsesFullDateTime() {
        val instant = parseCsvDateTime("2026-01-05 14:00:30", zone)
        assertEquals(kotlinx.datetime.Instant.parse("2026-01-05T14:00:30Z").toEpochMilliseconds(), instant?.toEpochMilliseconds())
    }

    @Test
    fun parsesMinutePrecision() {
        val instant = parseCsvDateTime("2026-01-05 14:00", zone)
        assertEquals(kotlinx.datetime.Instant.parse("2026-01-05T14:00:00Z").toEpochMilliseconds(), instant?.toEpochMilliseconds())
    }

    @Test
    fun parsesDateOnlyAtMidnight() {
        val dateOnly = LocalDate(2026, 1, 5).atStartOfDayIn(zone).toEpochMilliseconds()
        assertEquals(dateOnly, parseCsvDateTime("2026-01-05", zone)?.toEpochMilliseconds())
    }

    @Test
    fun rejectsBlankAndInvalid() {
        assertNull(parseCsvDateTime("", zone))
        assertNull(parseCsvDateTime("not a date", zone))
        assertNull(parseCsvDateTime("2026-13-40", zone))
    }

    @Test
    fun roundTripsAcrossTimezones() {
        val jakarta = TimeZone.of("Asia/Jakarta")
        val utc = TimeZone.UTC
        val millis = kotlinx.datetime.Instant.parse("2026-01-05T07:00:00Z").toEpochMilliseconds()
        assertEquals(kotlinx.datetime.Instant.parse("2026-01-05T07:00:00Z").toEpochMilliseconds(),
            parseCsvDateTime(formatCsvDateTime(millis, jakarta), jakarta)?.toEpochMilliseconds())
        assertEquals(kotlinx.datetime.Instant.parse("2026-01-05T07:00:00Z").toEpochMilliseconds(),
            parseCsvDateTime(formatCsvDateTime(millis, utc), utc)?.toEpochMilliseconds())
    }
}
