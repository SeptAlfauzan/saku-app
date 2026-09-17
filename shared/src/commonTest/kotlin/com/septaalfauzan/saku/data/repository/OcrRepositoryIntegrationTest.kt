package com.septaalfauzan.saku.data.repository

import com.septaalfauzan.saku.data.remote.ApiServiceImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OcrRepositoryIntegrationTest {

    private fun repositoryReturning(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): RemoteOcrRepository {
        val engine = MockEngine { request ->
            val sentBody = (request.body as TextContent).text
            val sentJson = Json.parseToJsonElement(sentBody).jsonObject
            assertEquals(HttpMethod.Post, request.method)
            assertEquals(
                "https://saku-api.septaalfauzan.my.id/api/v1/ocr",
                request.url.toString(),
            )
            assertEquals("abc", sentJson["image"]?.jsonPrimitive?.content)
            assertEquals("image/jpeg", sentJson["mime_type"]?.jsonPrimitive?.content)
            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json() }
        }
        return RemoteOcrRepository(ApiServiceImpl(client, "https://test"))
    }

    private fun receiptJson(paymentMethod: String? = null) = """{"data":{
        "merchant_name":"Alfamart",
        "transaction_date":"16-09-2026",
        "transaction_time":"10:00",
        "currency":"IDR",
        "subtotal":10000,
        "tax":0,
        "discount":500,
        "total":9500,
        "payment_method":${paymentMethod?.let { "\"$it\"" } ?: "null"},
        "items":[{"name":"Nasi","quantity":1,"unit_price":10000,"total_price":10000}]
    }}"""

    @Test
    fun successWithoutPaymentMethodYieldsDash() = runTest {
        val repo = repositoryReturning(receiptJson())
        val receipt = repo.getOcrReceiptValue("abc", "image/jpeg")
        assertEquals(9500L, receipt.total)
        assertEquals("Alfamart", receipt.merchantName)
        assertEquals("-", receipt.paymentMethod)
        assertEquals(1, receipt.items.size)
    }

    @Test
    fun successWithPaymentMethodPreservesIt() = runTest {
        val repo = repositoryReturning(receiptJson(paymentMethod = "GoPay"))
        val receipt = repo.getOcrReceiptValue("abc", "image/jpeg")
        assertEquals("GoPay", receipt.paymentMethod)
    }

    @Test
    fun errorResponseSurfacesExtractedMessage() = runTest {
        val repo = repositoryReturning(
            content = """{"error":"bad image"}""",
            status = HttpStatusCode.BadRequest,
        )
        val e = assertFailsWith<Exception> { repo.getOcrReceiptValue("abc", "image/jpeg") }
        assertEquals("bad image", e.message)
    }

    @Test
    fun errorResponseSurfacesRawBodyWhenNotJson() = runTest {
        val repo = repositoryReturning(
            content = "oops",
            status = HttpStatusCode.BadRequest,
        )
        val e = assertFailsWith<Exception> { repo.getOcrReceiptValue("abc", "image/jpeg") }
        assertEquals("oops", e.message)
    }

    @Test
    fun transportFailureSurfacesNetworkError() = runTest {
        val engine = MockEngine { throw RuntimeException("boom") }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json() }
        }
        val repo = RemoteOcrRepository(ApiServiceImpl(client, "https://test"))
        val e = assertFailsWith<Exception> { repo.getOcrReceiptValue("abc", "image/jpeg") }
        assertEquals("Network error: boom", e.message)
    }
}
