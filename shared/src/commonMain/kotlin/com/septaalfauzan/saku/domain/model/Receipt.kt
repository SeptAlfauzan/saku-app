package com.septaalfauzan.saku.domain.model

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
    val items: List<ReceiptItem>
)

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Long,
    val totalPrice: Long
)

fun Receipt.toItemsNote(): String =
    items.joinToString("\n") { item ->
        if (item.quantity > 1) "${item.quantity}x ${item.name}" else item.name
    }