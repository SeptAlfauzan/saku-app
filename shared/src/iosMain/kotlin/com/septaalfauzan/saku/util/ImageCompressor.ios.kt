package com.septaalfauzan.saku.util

import com.septaalfauzan.saku.domain.model.CompressedImage

actual object ImageCompressor {
    actual fun compress(
        image: ByteArray,
        maxDimension: Int,
        quality: Int
    ): CompressedImage {
        TODO("Not yet implemented")
    }
}