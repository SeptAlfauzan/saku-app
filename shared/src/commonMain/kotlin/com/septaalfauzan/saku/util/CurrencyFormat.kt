package com.septaalfauzan.saku.util

fun formatRupiah(amount: Long, absoluteValue: Boolean = true): String {
    val abs = kotlin.math.abs(amount)
    val negativeNumber = amount < 0
    val grouped = abs.toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
    return "${if (!absoluteValue && negativeNumber) "-" else ""}Rp$grouped"
}
