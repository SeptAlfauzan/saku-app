package com.septaalfauzan.saku.data.importexport.csv

object CsvWriter {
    /** Serializes rows per RFC 4180: CRLF endings, quoting fields containing , " \n \r, doubling embedded quotes. */
    fun write(rows: List<List<String?>>): String = buildString {
        rows.forEach { row ->
            append(row.joinToString(",") { escape(it) })
            append("\r\n")
        }
    }

    fun escape(field: String?): String {
        if (field == null) return ""
        return if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }
}
