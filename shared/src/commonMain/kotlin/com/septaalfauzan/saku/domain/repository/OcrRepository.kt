package com.septaalfauzan.saku.domain.repository

import com.septaalfauzan.saku.domain.model.Receipt

interface OcrRepository{
    suspend fun getOcrReceiptValue(base64Image: String, imageType: String): Receipt
}