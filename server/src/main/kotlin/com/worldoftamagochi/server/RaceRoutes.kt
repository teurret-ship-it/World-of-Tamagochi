package com.worldoftamagochi.server

import com.worldoftamagochi.api.ErrorResponse
import com.worldoftamagochi.api.GhostRun
import com.worldoftamagochi.api.GhostsResponse
import com.worldoftamagochi.api.LeaderboardEntry
import com.worldoftamagochi.api.LeaderboardResponse
import com.worldoftamagochi.api.RegisterRequest
import com.worldoftamagochi.api.RegisterResponse
import com.worldoftamagochi.api.RunRequest
import com.worldoftamagochi.api.RunResponse
import com.worldoftamagochi.api.StatsDto
import com.worldoftamagochi.sim.race.RaceStats
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

internal val RUNS_LIMIT = RateLimitName("runs")

/** The online racing API, version 1. */
fun Route.raceRoutes(service: RaceService) {
    route("/v1") {
        post("/players") {
            val request = call.receive<RegisterRequest>()
            val player = service.register(request.petName)
            call.respond(HttpStatusCode.Created, RegisterResponse(player.playerId, player.token, player.displayName))
        }
        get("/leaderboards/{trackId}") {
            val trackId = call.parameters["trackId"].orEmpty()
            val period = parsePeriod(call.request.queryParameters["period"])
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT
            val you =
                call.request.headers["Authorization"]
                    ?.removePrefix("Bearer ")
                    ?.let(service::authenticate)
            val entries =
                service.leaderboard(trackId, period, limit).map {
                    LeaderboardEntry(it.rank, it.run.displayName, it.run.finishMicros, you = it.run.playerId == you?.id)
                }
            call.respond(LeaderboardResponse(trackId, period.name.lowercase(), entries))
        }
        authenticate(AUTH) {
            rateLimit(RUNS_LIMIT) {
                post("/runs") {
                    val player = requireNotNull(call.principal<Player>())
                    val request = call.receive<RunRequest>()
                    val run = service.submit(player, request.trackId, request.simVersion, request.log, request.stats.toStats())
                    call.respond(HttpStatusCode.Created, RunResponse(run.finishMicros, run.personalBestMicros, run.dailyRank))
                }
            }
            get("/ghosts/{trackId}") {
                val player = requireNotNull(call.principal<Player>())
                val trackId = call.parameters["trackId"].orEmpty()
                val ghosts = service.ghosts(player, trackId).map { GhostRun(it.displayName, it.finishMicros, it.log, it.stats.toDto()) }
                call.respond(GhostsResponse(trackId, ghosts))
            }
        }
    }
}

private fun parsePeriod(value: String?): Period =
    when (value) {
        "daily" -> Period.DAILY
        "all" -> Period.ALL_TIME
        else -> Period.WEEKLY
    }

private const val DEFAULT_LIMIT = 20

/** Stats outside the allowed range are a bad request, like any malformed run. */
private fun StatsDto.toStats(): RaceStats =
    try {
        RaceStats(speed, stamina, agility, jump)
    } catch (e: IllegalArgumentException) {
        throw RejectedRun("Stats out of range", e)
    }

private fun RaceStats.toDto(): StatsDto = StatsDto(speed, stamina, agility, jump)
