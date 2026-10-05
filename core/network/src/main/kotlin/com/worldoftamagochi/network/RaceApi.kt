package com.worldoftamagochi.network

import com.worldoftamagochi.api.ErrorResponse
import com.worldoftamagochi.api.GhostsResponse
import com.worldoftamagochi.api.LeaderboardResponse
import com.worldoftamagochi.api.RegisterRequest
import com.worldoftamagochi.api.RegisterResponse
import com.worldoftamagochi.api.RunRequest
import com.worldoftamagochi.api.RunResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.IOException

/** Why a call failed: the server said no (do not retry), or it could not be reached (retry later). */
sealed class ApiException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class Rejected(
        val status: Int,
        message: String,
    ) : ApiException(message)

    class Unavailable(
        cause: Throwable,
    ) : ApiException("Server unavailable", cause)
}

/** The racing server's API (v1), see `:server`. */
interface RaceApi {
    suspend fun register(petName: String): RegisterResponse

    suspend fun submitRun(
        token: String,
        request: RunRequest,
    ): RunResponse

    suspend fun leaderboard(
        trackId: String,
        period: String,
        token: String?,
    ): LeaderboardResponse

    suspend fun ghosts(
        token: String,
        trackId: String,
    ): GhostsResponse
}

class KtorRaceApi(
    private val baseUrl: String,
    engine: HttpClientEngine,
) : RaceApi {
    private val json = Json { ignoreUnknownKeys = true }

    private val client =
        HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = TIMEOUT_MILLIS
                connectTimeoutMillis = TIMEOUT_MILLIS
            }
        }

    override suspend fun register(petName: String): RegisterResponse =
        call {
            client.post("$baseUrl/v1/players") { json(RegisterRequest(petName)) }
        }

    override suspend fun submitRun(
        token: String,
        request: RunRequest,
    ): RunResponse =
        call {
            client.post("$baseUrl/v1/runs") {
                bearerAuth(token)
                json(request)
            }
        }

    override suspend fun leaderboard(
        trackId: String,
        period: String,
        token: String?,
    ): LeaderboardResponse =
        call {
            client.get("$baseUrl/v1/leaderboards/$trackId") {
                parameter("period", period)
                token?.let { bearerAuth(it) }
            }
        }

    override suspend fun ghosts(
        token: String,
        trackId: String,
    ): GhostsResponse = call { client.get("$baseUrl/v1/ghosts/$trackId") { bearerAuth(token) } }

    private inline fun <reified T> HttpRequestBuilder.json(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend inline fun <reified T> call(request: () -> HttpResponse): T {
        val response =
            try {
                request()
            } catch (e: IOException) {
                throw ApiException.Unavailable(e)
            }
        if (response.status.isSuccess()) return response.body()
        val message = runCatching { response.body<ErrorResponse>().error }.getOrDefault(response.status.description)
        throw if (response.status.value >= SERVER_ERROR) {
            ApiException.Unavailable(IOException(message))
        } else {
            ApiException.Rejected(response.status.value, message)
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 8_000L
        const val SERVER_ERROR = 500
    }
}
