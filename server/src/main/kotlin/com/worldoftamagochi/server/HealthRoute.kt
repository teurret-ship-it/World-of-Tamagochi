package com.worldoftamagochi.server

import com.worldoftamagochi.sim.SimVersion
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** Liveness probe; also tells clients which rules version the server runs. */
fun Route.health() {
    get("/health") {
        call.respond(HealthResponse(status = "ok", simVersion = SimVersion.CURRENT))
    }
}
