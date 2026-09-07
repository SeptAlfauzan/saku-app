package com.septaalfauzan.saku.notification.normalizer

object AmountNormalizer {
    private val currencyPrefix = Regex("""(?i)^\s*(Rp|IDR)\s*""")
    private val tokenRegex = Regex("""(?i)(?:Rp\s*|IDR\s*)?\d[\d.,]*""")

    fun firstToken(text: String): String? = tokenRegex.find(text)?.value

    fun normalize(raw: String): Long? {
        var s = raw.replace(currencyPrefix, "")
        s = s.replace(",", "")
        s = s.replace(".", "")
        return s.trim().toLongOrNull()
    }
}
