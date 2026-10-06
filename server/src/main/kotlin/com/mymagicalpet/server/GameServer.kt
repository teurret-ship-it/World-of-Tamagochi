package com.mymagicalpet.server

import com.mymagicalpet.api.ErrorResponse
import com.mymagicalpet.api.GhostRun
import com.mymagicalpet.api.GhostsResponse
import com.mymagicalpet.api.LeaderboardEntry
import com.mymagicalpet.api.LeaderboardResponse
import com.mymagicalpet.api.RegisterRequest
import com.mymagicalpet.api.RegisterResponse
import com.mymagicalpet.api.RunRequest
import com.mymagicalpet.api.RunResponse
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlin.time.Duration.Companion.minutes

internal const val AUTH = "player"

/** Ktor module of the authoritative game server. */
fun Application.gameServer(service: RaceService = RaceService(GameStore(openDatabase(DatabaseConfig.fromEnvironment())))) {
    install(ContentNegotiation) { json() }
    install(StatusPages) {
        exception<RejectedRun> { call, cause -> call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponse(cause.message.orEmpty())) }
        exception<BadRequestException> { call, _ -> call.respond(HttpStatusCode.BadRequest, ErrorResponse("Bad request")) }
    }
    install(Authentication) {
        bearer(AUTH) { authenticate { credential -> service.authenticate(credential.token) } }
    }
    install(RateLimit) {
        // A race lasts 30+ seconds: a dozen runs a minute is already generous.
        register(RUNS_LIMIT) {
            rateLimiter(limit = RUNS_PER_MINUTE, refillPeriod = 1.minutes)
            requestKey { call -> call.request.headers["Authorization"].orEmpty() }
        }
    }
    routing {
        health()
        raceRoutes(service)
    }
}

private const val RUNS_PER_MINUTE = 12
