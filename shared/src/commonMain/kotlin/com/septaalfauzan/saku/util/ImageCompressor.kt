package com.septaalfauzan.saku.util

import com.septaalfauzan.saku.domain.model.CompressedImage

expect object ImageCompressor{
    fun compress(
        image: ByteArray,
        maxDimension: Int = 2000,
        quality: Int = 85
    ): CompressedImage
}