package com.septaalfauzan.saku.data.remote.response

import com.septaalfauzan.saku.domain.model.Receipt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceiptResponse(
    val data: ReceiptData
)

@kotlinx.serialization.Serializable
data class ReceiptData(
    @SerialName("merchant_name")
    val merchantName: String,

    @SerialName("transaction_date")
    val transactionDate: String,

    @SerialName("transaction_time")
    val transactionTime: String,

    val currency: String,

    val subtotal: Long?,

    val tax: Long,

    val discount: Long,

    val total: Long,

    @SerialName("payment_method")
    val paymentMethod: String,

    val items: List<ReceiptItem>
)

@Serializable
data class ReceiptItem(
    val name: String,
    val quantity: Int,

    @SerialName("unit_price")
    val unitPrice: Long,

    @SerialName("total_price")
    val totalPrice: Long
)

fun ReceiptData.toDomain(): Receipt = Receipt(
    this.merchantName,
    this.transactionDate,
    this.transactionTime,
    this.currency,
    this.subtotal,
    this.tax,
    this.discount,
    this.total,
    this.paymentMethod,
    items = this.items.map {
        com.septaalfauzan.saku.domain.model.ReceiptItem(
            it.name,
            it.quantity,
            it.unitPrice,
            it.totalPrice,
        )
    }
)