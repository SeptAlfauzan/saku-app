package com.septaalfauzan.saku.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationSource(
    val packageName: String,
    val providerId: String,
    val enabled: Boolean,
)
