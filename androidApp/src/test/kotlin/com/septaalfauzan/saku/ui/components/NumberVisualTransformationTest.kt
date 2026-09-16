package com.septaalfauzan.saku.ui.components

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberVisualTransformationTest {

    private val sut = NumberVisualTransformation()

    private fun format(input: String): String =
        sut.filter(AnnotatedString(input)).text.text

    @Test
    fun groupsDigitsWithDotSeparator() {
        assertEquals("0", format("0"))
        assertEquals("123", format("123"))
        assertEquals("1.234", format("1234"))
        assertEquals("12.345", format("12345"))
        assertEquals("1.234.567", format("1234567"))
    }

    @Test
    fun stripsNonDigitCharacters() {
        assertEquals("1.234", format("12ab34"))
        assertEquals("1.234", format("1a2b3c4"))
    }

    @Test
    fun emptyInputProducesEmptyOutput() {
        assertEquals("", format(""))
    }

    @Test
    fun offsetMappingRoundTrips() {
        val result = sut.filter(AnnotatedString("1234"))

        assertEquals(5, result.offsetMapping.originalToTransformed(4))
        assertEquals(4, result.offsetMapping.transformedToOriginal(5))

        for (offset in 0..4) {
            val transformed = result.offsetMapping.originalToTransformed(offset)
            assertEquals(offset, result.offsetMapping.transformedToOriginal(transformed))
        }
    }
}