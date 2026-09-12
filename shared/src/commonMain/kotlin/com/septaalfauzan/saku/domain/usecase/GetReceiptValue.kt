package com.septaalfauzan.saku.domain.usecase

import com.septaalfauzan.saku.domain.model.Receipt
import com.septaalfauzan.saku.domain.repository.OcrRepository

class GetReceiptValue(private val repository: OcrRepository) {
    suspend fun invoke(imageBase64: String, imageType: String) : Receipt = repository.getOcrReceiptValue(imageBase64, imageType)
}