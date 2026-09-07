package com.septaalfauzan.saku.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyFormatTest {

    @Test
    fun formatsPlainThousandsUsingIdDots() {
        assertEquals("Rp. 1.250.000", formatRupiah(1_250_000))
    }

    @Test
    fun formatsSmallAmounts() {
        assertEquals("Rp. 54.990", formatRupiah(54_990))
        assertEquals("Rp. 0", formatRupiah(0))
        assertEquals("Rp. 5", formatRupiah(5))
    }

    @Test
    fun groupsEveryThreeDigits() {
        assertEquals("Rp. 1", formatRupiah(1))
        assertEquals("Rp. 100", formatRupiah(100))
        assertEquals("Rp. 1.000", formatRupiah(1_000))
        assertEquals("Rp. 123.456.789", formatRupiah(123_456_789))
    }
}
