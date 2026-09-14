package com.septaalfauzan.saku.domain.model

data class CompressedImage(
    val bytes: ByteArray,
    val mimeType: String
)