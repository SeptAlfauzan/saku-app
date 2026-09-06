package com.septaalfauzan.saku.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionStatus { CONFIRMED, PENDING_REVIEW, IGNORED }
