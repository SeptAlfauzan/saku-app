package com.septaalfauzan.saku.data.remote.response

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String
)