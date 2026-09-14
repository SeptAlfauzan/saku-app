package com.septaalfauzan.saku.data.remote

import com.septaalfauzan.saku.data.remote.request.OcrRequest
import com.septaalfauzan.saku.data.remote.response.ReceiptResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

interface ApiService {
    suspend fun getReceiptValue(request: OcrRequest) : NetworkResult<ReceiptResponse>
}

class ApiServiceImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : ApiService{
    override suspend fun getReceiptValue(
  request: OcrRequest
    ): NetworkResult<ReceiptResponse> {
        return httpClient.safeRequest<ReceiptResponse>{
            post("https://saku-api.septaalfauzan.my.id/api/v1/ocr") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }
}