package com.septaalfauzan.saku.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@Serializable
private data class Dummy(val id: Int, val name: String)

class NetworkRequestTest {

    private fun client(engine: MockEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json()
        }
    }

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun twoHundredWithValidBodyReturnsSuccess() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"id":1,"name":"ok"}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }
        val result = client(engine).safeRequest<Dummy> { get("https://test/api") }
        assertIs<NetworkResult.Success<Dummy>>(result)
        assertEquals(1, result.data.id)
    }

    @Test
    fun twoHundredWithUndecodableBodyReturnsSerializationError() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"not_a_dummy":true}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }
        val result = client(engine).safeRequest<Dummy> { get("https://test/api") }
        assertIs<NetworkResult.Failure>(result)
        assertTrue(result.errorMessage.startsWith("Serialization error:"))
    }

    @Test
    fun fourHundredWithErrorJsonReturnsExtractedError() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"error":"bad image"}""",
                status = HttpStatusCode.BadRequest,
                headers = jsonHeaders(),
            )
        }
        val result = client(engine).safeRequest<Dummy> { get("https://test/api") }
        assertEquals("bad image", (result as NetworkResult.Failure).errorMessage)
    }

    @Test
    fun fourHundredWithBlankErrorFallsBackToRawBody() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"error":""}""",
                status = HttpStatusCode.BadRequest,
                headers = jsonHeaders(),
            )
        }
        val result = client(engine).safeRequest<Dummy> { get("https://test/api") }
        assertEquals("""{"error":""}""", (result as NetworkResult.Failure).errorMessage)
    }

    @Test
    fun fourHundredWithNonJsonBodyReturnsRawBody() = runTest {
        val engine = MockEngine {
            respond(
                content = "oops",
                status = HttpStatusCode.BadRequest,
            )
        }
        val result = client(engine).safeRequest<Dummy> { get("https://test/api") }
        assertEquals("oops", (result as NetworkResult.Failure).errorMessage)
    }

    @Test
    fun thrownTransportExceptionBecomesNetworkError() = runTest {
        val engine = MockEngine { throw RuntimeException("boom") }
        val result = client(engine).safeRequest<Dummy> { get("https://test/api") }
        assertEquals("Network error: boom", (result as NetworkResult.Failure).errorMessage)
    }
}
