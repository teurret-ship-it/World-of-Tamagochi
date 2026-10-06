package com.mymagicalpet.server

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

private const val DEFAULT_PORT = 8080

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT
    embeddedServer(Netty, port = port, module = { gameServer() }).start(wait = true)
}
