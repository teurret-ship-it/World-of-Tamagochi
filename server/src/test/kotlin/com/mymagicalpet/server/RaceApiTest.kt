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
import com.mymagicalpet.api.StatsDto
import com.mymagicalpet.sim.SimVersion
import com.mymagicalpet.sim.race.AgilityTracks
import com.mymagicalpet.sim.race.Autopilot
import com.mymagicalpet.sim.race.InputLog
import com.mymagicalpet.sim.race.RaceStats
import com.mymagicalpet.sim.race.Replay
import com.mymagicalpet.sim.race.SprintTracks
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class RaceApiTest {
    private val track = SprintTracks.MEADOW
    private val fastLog = Autopilot(track).play(RaceStats.ROOKIE).second
    private val slowLog = InputLog(emptyList())
    private var now = LocalDateTime.of(2026, 10, 7, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli() // a Wednesday

    private fun api(test: suspend ApplicationTestBuilder.(HttpClient) -> Unit) =
        testApplication {
            val service = RaceService(GameStore(openDatabase(DatabaseConfig.inMemory(UUID.randomUUID().toString()))), clock = { now })
            application { gameServer(service) }
            test(createClient { install(ContentNegotiation) { json() } })
        }

    private suspend fun HttpClient.register(name: String = "Pip"): RegisterResponse =
        post("/v1/players") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(name))
        }.body()

    private suspend fun HttpClient.submit(
        token: String?,
        log: InputLog,
        trackId: String = track.id,
        simVersion: Int = SimVersion.CURRENT,
        stats: StatsDto = StatsDto(),
    ): HttpResponse =
        post("/v1/runs") {
            token?.let { bearerAuth(it) }
            contentType(ContentType.Application.Json)
            setBody(RunRequest(trackId, simVersion, log.toCodes(), stats))
        }

    @Test
    fun `players register anonymously with a safe display name`() =
        api { client ->
            val player = client.register("Pip")
            player.displayName shouldMatch Regex("Pip #\\d{4}")
            (player.token.length == 64) shouldBe true
            client.register("<script>").displayName shouldMatch Regex("Mochi #\\d{4}")
        }

    @Test
    fun `the server computes the time itself by replaying the log`() =
        api { client ->
            val token = client.register().token
            val response = client.submit(token, fastLog)
            response.status shouldBe HttpStatusCode.Created
            val run = response.body<RunResponse>()
            run.finishMicros shouldBe Replay.run(track, RaceStats.ROOKIE, fastLog).finishMicros
            run.dailyRank shouldBe 1
        }

    @Test
    fun `agility runs are verified too, faults included in the time`() =
        api { client ->
            val token = client.register().token
            val course = AgilityTracks.PARK
            val clean = Autopilot(course).play(RaceStats.ROOKIE).second
            val cleanRun = client.submit(token, clean, trackId = course.id).body<RunResponse>()
            cleanRun.finishMicros shouldBe Replay.run(course, RaceStats.ROOKIE, clean).finishMicros
            val sloppy = Replay.run(course, RaceStats.ROOKIE, slowLog)
            (sloppy.faults > 0) shouldBe true
            client.submit(token, slowLog, trackId = course.id).body<RunResponse>().finishMicros shouldBe sloppy.finishMicros
        }

    @Test
    fun `trained stats are replayed, but only as much as daily training allows`() =
        api { client ->
            val token = client.register().token
            val trained = RaceStats(speed = 40, stamina = 30, agility = 20, jump = 10)
            val log = Autopilot(track).play(trained).second
            val run = client.submit(token, log, stats = StatsDto(40, 30, 20, 10)).body<RunResponse>()
            run.finishMicros shouldBe Replay.run(track, trained, log).finishMicros
            // A brand-new player cannot have a maxed stat yet; nobody can go past 100.
            client.submit(token, log, stats = StatsDto(speed = 100)).status shouldBe HttpStatusCode.UnprocessableEntity
            client.submit(token, log, stats = StatsDto(speed = 101)).status shouldBe HttpStatusCode.UnprocessableEntity
            // Ghosts carry their stats, so they replay exactly.
            val other = client.register("Bean").token
            val ghost =
                client
                    .get("/v1/ghosts/${track.id}") { bearerAuth(other) }
                    .body<GhostsResponse>()
                    .ghosts
                    .single()
            ghost.stats shouldBe StatsDto(40, 30, 20, 10)
            Replay.run(track, trained, InputLog.fromCodes(ghost.log)).finishMicros shouldBe ghost.finishMicros
        }

    @Test
    fun `bad runs are rejected and anonymous runs refused`() =
        api { client ->
            val token = client.register().token
            client.submit(null, fastLog).status shouldBe HttpStatusCode.Unauthorized
            client.submit("not-a-token", fastLog).status shouldBe HttpStatusCode.Unauthorized
            client.submit(token, fastLog, trackId = "nope").status shouldBe HttpStatusCode.UnprocessableEntity
            client.submit(token, fastLog, simVersion = SimVersion.CURRENT + 1).status shouldBe HttpStatusCode.UnprocessableEntity
            val unordered =
                client.post("/v1/runs") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(RunRequest(track.id, SimVersion.CURRENT, listOf(400, 8)))
                }
            unordered.status shouldBe HttpStatusCode.UnprocessableEntity
        }

    @Test
    fun `leaderboards keep each player's best and mark you`() =
        api { client ->
            val slow = client.register("Bean").token
            val fast = client.register("Kiwi").token
            client.submit(slow, slowLog)
            client.submit(fast, slowLog)
            client.submit(fast, fastLog)
            val board = client.get("/v1/leaderboards/${track.id}?period=daily") { bearerAuth(slow) }.body<LeaderboardResponse>()
            board.entries.size shouldBe 2
            board.entries[0].displayName.startsWith("Kiwi") shouldBe true
            board.entries[0].you shouldBe false
            board.entries[1].you shouldBe true
            board.entries[1].rank shouldBe 2
        }

    @Test
    fun `daily boards start fresh, weekly and all-time boards remember`() =
        api { client ->
            val token = client.register().token
            client.submit(token, fastLog)
            now += 24 * 3_600_000L // Thursday
            client
                .get("/v1/leaderboards/${track.id}?period=daily")
                .body<LeaderboardResponse>()
                .entries.size shouldBe 0
            client
                .get("/v1/leaderboards/${track.id}?period=weekly")
                .body<LeaderboardResponse>()
                .entries.size shouldBe 1
            now += 5 * 24 * 3_600_000L // next Tuesday
            client
                .get("/v1/leaderboards/${track.id}")
                .body<LeaderboardResponse>()
                .entries.size shouldBe 0
            client
                .get("/v1/leaderboards/${track.id}?period=all")
                .body<LeaderboardResponse>()
                .entries.size shouldBe 1
        }

    @Test
    fun `ghosts are the players just ahead of you`() =
        api { client ->
            val leader = client.register("Comet").token
            val chaser = client.register("Pebble").token
            client.submit(leader, fastLog)
            client.submit(chaser, slowLog)
            val forChaser = client.get("/v1/ghosts/${track.id}") { bearerAuth(chaser) }.body<GhostsResponse>().ghosts
            forChaser.size shouldBe 1
            forChaser[0].displayName.startsWith("Comet") shouldBe true
            InputLog.fromCodes(forChaser[0].log) shouldBe fastLog
            val forLeader = client.get("/v1/ghosts/${track.id}") { bearerAuth(leader) }.body<GhostsResponse>().ghosts
            forLeader.single().displayName.startsWith("Pebble") shouldBe true
        }

    @Test
    fun `runs are rate limited per player`() =
        api { client ->
            val token = client.register().token
            val statuses = (1..13).map { client.submit(token, slowLog).status }
            statuses.take(12).all { it == HttpStatusCode.Created } shouldBe true
            statuses.last() shouldBe HttpStatusCode.TooManyRequests
        }
}
