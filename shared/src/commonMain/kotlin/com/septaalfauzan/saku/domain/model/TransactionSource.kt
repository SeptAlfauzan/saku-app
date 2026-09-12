package com.septaalfauzan.saku.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionSource { MANUAL, NOTIFICATION, SCAN }
