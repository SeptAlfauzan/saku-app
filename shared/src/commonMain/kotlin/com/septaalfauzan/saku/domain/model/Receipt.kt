package com.septaalfauzan.saku.domain.model

import com.septaalfauzan.saku.util.formatRupiah

data class Receipt(
    val merchantName: String,
    val transactionDate: String,
    val transactionTime: String,
    val currency: String,
    val subtotal: Long?,
    val tax: Long,
    val discount: Long,
    val total: Long,
    val paymentMethod: String,
    val items: List<ReceiptItem>,
    val categories: List<Category> = listOf(),
    val categoryId: String? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val note: String? = null,
)

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Long,
    val totalPrice: Long
)

fun Receipt.toItemsNote(): String = note ?:
    items.map {
        "${it.quantity}x ${it.name} (${formatRupiah(it.totalPrice)})"
    }.joinToString(", ")
