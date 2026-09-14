package com.septaalfauzan.saku.data.remote.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OcrRequest(
    val image: String,
    @SerialName("mime_type")  val mimeType: String
)