package com.septaalfauzan.saku.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType { INCOME, EXPENSE, TRANSFER }
