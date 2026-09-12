package com.septaalfauzan.saku.data.repository

import com.septaalfauzan.saku.data.remote.ApiService
import com.septaalfauzan.saku.data.remote.NetworkResult
import com.septaalfauzan.saku.data.remote.request.OcrRequest
import com.septaalfauzan.saku.data.remote.response.ReceiptData
import com.septaalfauzan.saku.data.remote.response.ReceiptResponse
import com.septaalfauzan.saku.data.remote.response.toDomain
import com.septaalfauzan.saku.domain.model.Receipt
import com.septaalfauzan.saku.domain.repository.OcrRepository

class RemoteOcrRepository(
    private val apiService: ApiService
) : OcrRepository {
    override suspend fun getOcrReceiptValue(
        base64Image: String,
        imageType: String
    ): Receipt {
        val request = OcrRequest(
            base64Image, imageType
        )
        return when (val result = apiService.getReceiptValue(request)) {
            is NetworkResult.Success<ReceiptResponse> -> {
                val data = result.data.data.toDomain()
                data
            }

            is NetworkResult.Failure -> {
                throw Exception(result.errorMessage)
            }
        }
    }

}