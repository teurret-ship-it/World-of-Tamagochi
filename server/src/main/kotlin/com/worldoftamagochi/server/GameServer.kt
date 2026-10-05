package com.worldoftamagochi.server

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

/** Ktor module of the authoritative game server. */
fun Application.gameServer() {
    install(ContentNegotiation) { json() }
    routing {
        health()
    }
}
