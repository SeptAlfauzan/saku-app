package com.septaalfauzan.saku.util

fun formatRupiah(amount: Long): String {
    val abs = kotlin.math.abs(amount)
    val grouped = abs.toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
    return "Rp. $grouped"
}
