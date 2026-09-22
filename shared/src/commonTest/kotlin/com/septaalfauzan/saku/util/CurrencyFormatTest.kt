package com.septaalfauzan.saku.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyFormatTest {

    @Test
    fun formatsPlainThousandsUsingIdDots() {
        assertEquals("Rp1.250.000", formatRupiah(1_250_000))
    }

    @Test
    fun formatsSmallAmounts() {
        assertEquals("Rp54.990", formatRupiah(54_990))
        assertEquals("Rp0", formatRupiah(0))
        assertEquals("Rp5", formatRupiah(5))
    }

    @Test
    fun groupsEveryThreeDigits() {
        assertEquals("Rp1", formatRupiah(1))
        assertEquals("Rp100", formatRupiah(100))
        assertEquals("Rp1.000", formatRupiah(1_000))
        assertEquals("Rp123.456.789", formatRupiah(123_456_789))
    }

    @Test
    fun hidesNegativeSignByDefault() {
        assertEquals("Rp1.000", formatRupiah(-1_000))
        assertEquals("Rp0", formatRupiah(-0))
    }

    @Test
    fun showsNegativeSignWhenAbsoluteValueDisabled() {
        assertEquals("-Rp1.000", formatRupiah(-1_000, absoluteValue = false))
        assertEquals("-Rp123.456.789", formatRupiah(-123_456_789, absoluteValue = false))
    }
}
