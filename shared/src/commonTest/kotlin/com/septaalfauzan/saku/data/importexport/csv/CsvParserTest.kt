package com.septaalfauzan.saku.data.importexport.csv

import kotlin.test.Test
import kotlin.test.assertEquals

class CsvParserTest {

    @Test
    fun parsesSimpleRows() {
        val rows = CsvParser.parse("a,b\r\n1,2\r\n")
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), rows)
    }

    @Test
    fun handlesQuotedFieldWithComma() {
        val rows = CsvParser.parse("\"hello, world\",ok\r\n")
        assertEquals(listOf(listOf("hello, world", "ok")), rows)
    }

    @Test
    fun handlesDoubledQuoteInsideQuotedField() {
        val rows = CsvParser.parse("\"say \"\"hi\"\"\"\r\n")
        assertEquals(listOf(listOf("say \"hi\"")), rows)
    }

    @Test
    fun handlesQuoteNotAtFieldStartAsLiteral() {
        val rows = CsvParser.parse("ab\"cd\r\n")
        assertEquals(listOf(listOf("ab\"cd")), rows)
    }

    @Test
    fun handlesEmbeddedNewlineInQuotedField() {
        val rows = CsvParser.parse("first,\"line1\nline2\"\r\n")
        assertEquals(1, rows.size)
        assertEquals(listOf("first", "line1\nline2"), rows.single())
    }

    @Test
    fun preservesTrailingEmptyFields() {
        val rows = CsvParser.parse("x,,,\r\n")
        assertEquals(listOf(listOf("x", "", "", "")), rows)
    }

    @Test
    fun dropsBlankLinesButKeepsHeaderRow() {
        val rows = CsvParser.parse("h1,h2\r\n\r\nv1,v2\r\n")
        assertEquals(listOf(listOf("h1", "h2"), listOf("v1", "v2")), rows)
    }

    @Test
    fun autoDetectsSemicolonDelimiter() {
        assertEquals(';', CsvParser.detectDelimiter("Transaction ID;Type;Amount"))
        assertEquals(',', CsvParser.detectDelimiter("Transaction ID,Type,Amount"))
        assertEquals(',', CsvParser.detectDelimiter("a;b,c;d"))
    }

    @Test
    fun parsesSemicolonDelimited() {
        val rows = CsvParser.parse("a;b;c\r\n1;2;3\r\n")
        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "2", "3")), rows)
    }
}
