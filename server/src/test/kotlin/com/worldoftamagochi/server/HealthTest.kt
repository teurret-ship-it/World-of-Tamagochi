package com.worldoftamagochi.server

import com.worldoftamagochi.sim.SimVersion
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test

class HealthTest {
    @Test
    fun `health reports ok and the rules version the server runs`() =
        testApplication {
            application { gameServer() }
            val response = client.get("/health")
            response.status shouldBe HttpStatusCode.OK
            val body = response.bodyAsText()
            body shouldContain "\"status\":\"ok\""
            body shouldContain "\"simVersion\":${SimVersion.CURRENT}"
        }
}
