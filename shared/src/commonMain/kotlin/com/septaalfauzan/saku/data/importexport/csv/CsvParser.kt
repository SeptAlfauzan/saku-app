package com.septaalfauzan.saku.data.importexport.csv

object CsvParser {
    /**
     * Parses RFC 4180 CSV. Handles quoted fields (commas, doubled quotes,
     * embedded newlines), CRLF/LF endings, and trailing empty fields.
     * Blank lines are dropped; the header (first row) is kept as element 0.
     */
    fun parse(input: String): List<List<String>> {
        val text = if (input.startsWith('\uFEFF')) input.removePrefix("\uFEFF") else input
        val delimiter = detectDelimiter(text)
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < input.length && input[i + 1] == '"') {
                            field.append('"'); i += 2; continue
                        }
                        inQuotes = false; i++
                    } else {
                        field.append(c); i++
                    }
                }
                c == '"' -> {
                    if (field.isEmpty()) { inQuotes = true; i++ } else { field.append(c); i++ }
                }
                c == delimiter -> { row.add(field.toString()); field.setLength(0); i++ }
                c == '\r' -> i++
                c == '\n' -> { row.add(field.toString()); field.setLength(0); rows.add(row); row = mutableListOf(); i++ }
                else -> { field.append(c); i++ }
            }
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows.filter { it.any { cell -> cell.isNotBlank() } }
    }

    /** Comma by default; semicolon when the first non-empty line has ';' and no ','. */
    fun detectDelimiter(input: String): Char {
        val firstLine = input.substringBefore('\n').trim()
        return if (firstLine.contains(';') && !firstLine.contains(',')) ';' else ','
    }
}
