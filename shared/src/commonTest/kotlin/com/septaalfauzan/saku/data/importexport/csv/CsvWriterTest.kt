package com.septaalfauzan.saku.data.importexport.csv

import kotlin.test.Test
import kotlin.test.assertEquals

class CsvWriterTest {

    @Test
    fun writesPlainFieldsSeparatedByCommasWithCrlf() {
        val csv = CsvWriter.write(listOf(listOf("a", "b", "c")))
        assertEquals("a,b,c\r\n", csv)
    }

    @Test
    fun quotesFieldsContainingCommas() {
        val csv = CsvWriter.write(listOf(listOf("hello, world", "ok")))
        assertEquals("\"hello, world\",ok\r\n", csv)
    }

    @Test
    fun doublesQuotesInsideQuotedFields() {
        val csv = CsvWriter.write(listOf(listOf("say \"hi\"")))
        assertEquals("\"say \"\"hi\"\"\"\r\n", csv)
    }

    @Test
    fun quotesFieldsWithNewlinesAndCarriageReturns() {
        val csv = CsvWriter.write(listOf(listOf("line1\nline2")))
        assertEquals("\"line1\nline2\"\r\n", csv)
    }

    @Test
    fun nullAndEmptyFieldsBecomeEmpty() {
        val csv = CsvWriter.write(listOf(listOf("x", null, "")))
        assertEquals("x,,\r\n", csv)
    }

    @Test
    fun writesMultipleRows() {
        val csv = CsvWriter.write(listOf(listOf("a"), listOf("b")))
        assertEquals("a\r\nb\r\n", csv)
    }
}