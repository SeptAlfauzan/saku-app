package com.septaalfauzan.saku.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import com.septaalfauzan.saku.domain.model.CompressedImage

actual object ImageCompressor {
    actual fun compress(
        image: ByteArray,
        maxDimension: Int,
        quality: Int
    ): CompressedImage {
        val bitmap = BitmapFactory.decodeByteArray(
            image,
            0,
            image.size
        ) ?: error("Unable to decode image")

        val scale = minOf(
            1f,
            maxDimension.toFloat() /
                    maxOf(bitmap.width, bitmap.height)
        )

        val targetWidth = (bitmap.width * scale).toInt()
        val targetHeight = (bitmap.height * scale).toInt()

        val resizedBitmap =
            if (bitmap.width != targetWidth || bitmap.height != targetHeight) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    targetWidth,
                    targetHeight,
                    true
                )
            } else {
                bitmap
            }

        val compressedBytes = ByteArrayOutputStream().use { output ->
            resizedBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                quality,
                output
            )

            output.toByteArray()
        }

        if (resizedBitmap !== bitmap) {
            resizedBitmap.recycle()
        }

        bitmap.recycle()

        return CompressedImage(
            bytes = compressedBytes,
            mimeType = "image/jpeg"
        )
    }
}