package com.septaalfauzan.saku.data.remote

import com.septaalfauzan.saku.data.remote.response.ErrorResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.ContentConvertException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Failure(val errorMessage: String) : NetworkResult<Nothing>()
}

@PublishedApi internal val errorJson = Json { ignoreUnknownKeys = true }

suspend inline fun <reified T> HttpResponse.handleResponse(): NetworkResult<T> {
    return when (status.value) {
        in 200..299 -> {
            try {
                val result = if (T::class == Unit::class) Unit as T else body<T>()
                NetworkResult.Success(result)
            } catch (e: ContentConvertException) {
                com.septaalfauzan.saku.sentry.captureThrowable(e)
                NetworkResult.Failure("Serialization error: ${e.message}")
            } catch (e: SerializationException) {
                com.septaalfauzan.saku.sentry.captureThrowable(e)
                NetworkResult.Failure("Serialization error: ${e.message}")
            }
        }
        else -> {
            com.septaalfauzan.saku.sentry.addNetworkFailureBreadcrumb("HTTP ${status.value}")
            val errorBody = bodyAsText()
            val errorMessage = try {
                errorJson.decodeFromString<ErrorResponse>(errorBody).error
                    .ifBlank { errorBody }
            } catch (_: Exception) {
                errorBody
            }
            NetworkResult.Failure(errorMessage)
        }
    }
}

suspend inline fun <reified T> HttpClient.safeRequest(
    crossinline block: suspend HttpClient.() -> HttpResponse
): NetworkResult<T> {
    return try {
        val response = block()
        response.handleResponse<T>()
    } catch (e: Exception) {
        com.septaalfauzan.saku.sentry.addNetworkFailureBreadcrumb(e.message ?: "network error")
        NetworkResult.Failure("Network error: ${e.message}")
    }
}
