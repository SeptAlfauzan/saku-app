package com.septaalfauzan.saku.notification.normalizer

object AmountNormalizer {
    private val currencyPrefix = Regex("""(?i)^\s*(Rp|IDR)\s*""")
    private val tokenRegex = Regex("""(?i)(?:Rp\s*|IDR\s*)?\d[\d.,]*""")

    fun firstToken(text: String): String? = tokenRegex.find(text)?.value

    fun normalize(raw: String): Long? {
        var s = raw.replace(currencyPrefix, "").trim()
        if (s.isEmpty()) return null
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        when {
            lastDot > lastComma -> {
                val after = s.length - lastDot - 1
                if (after in 1..2) {
                    s = s.substring(0, lastDot).replace(",", "")
                } else {
                    s = s.replace(".", "")
                }
            }
            lastComma > lastDot -> {
                val after = s.length - lastComma - 1
                if (after in 1..2) {
                    s = s.substring(0, lastComma).replace(".", "")
                } else {
                    s = s.replace(".", "").replace(",", "")
                }
            }
            else -> {
                s = s.replace(".", "")
            }
        }
        return s.trim().toLongOrNull()
    }
}
