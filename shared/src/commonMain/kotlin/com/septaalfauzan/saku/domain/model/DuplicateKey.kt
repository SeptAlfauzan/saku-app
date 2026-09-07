package com.septaalfauzan.saku.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DuplicateKey(
    val sourcePackage: String,
    val type: TransactionType,
    val amount: Long,
)
