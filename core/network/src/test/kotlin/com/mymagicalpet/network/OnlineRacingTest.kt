package com.mymagicalpet.network

import com.mymagicalpet.server.DatabaseConfig
import com.mymagicalpet.server.GameStore
import com.mymagicalpet.server.RaceService
import com.mymagicalpet.server.gameServer
import com.mymagicalpet.server.openDatabase
import com.mymagicalpet.sim.race.Autopilot
import com.mymagicalpet.sim.race.InputLog
import com.mymagicalpet.sim.race.RaceStats
import com.mymagicalpet.sim.race.Replay
import com.mymagicalpet.sim.race.SprintTracks
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.MockEngine
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.UUID

class OnlineRacingTest {
    private val track = SprintTracks.MEADOW
    private val fastLog = Autopilot(track).play(RaceStats.ROOKIE).second

    private class MemoryStore : OnlineStore {
        var account: OnlineAccount? = null
        var pending: List<PendingRun> = emptyList()

        override suspend fun account() = account

        override suspend fun saveAccount(account: OnlineAccount) {
            this.account = account
        }

        override suspend fun pendingRuns() = pending

        override suspend fun savePendingRuns(runs: List<PendingRun>) {
            pending = runs
        }
    }

    /** The app's client against the real server, in memory. */
    private fun withServer(test: suspend (RaceApi) -> Unit) =
        testApplication {
            val service = RaceService(GameStore(openDatabase(DatabaseConfig.inMemory(UUID.randomUUID().toString()))))
            application { gameServer(service) }
            startApplication()
            test(KtorRaceApi("http://localhost", client.engine))
        }

    @Test
    fun `a finished race is uploaded, verified and ranked`() =
        withServer { api ->
            val store = MemoryStore()
            val online = OnlineRacing(api, store) { "Kiwi" }
            val upload = online.submit(track.id, fastLog).shouldBeInstanceOf<Upload.Verified>()
            upload.response.finishMicros shouldBe Replay.run(track, RaceStats.ROOKIE, fastLog).finishMicros
            upload.response.dailyRank shouldBe 1
            store.account?.displayName?.startsWith("Kiwi #") shouldBe true
            store.pending shouldBe emptyList()
            online.leaderboard(track.id, "daily")?.single()?.you shouldBe true
        }

    @Test
    fun `the rival ghost is the player just ahead`() =
        withServer { api ->
            OnlineRacing(api, MemoryStore()) { "Comet" }.submit(track.id, fastLog)
            val chaser = OnlineRacing(api, MemoryStore()) { "Pip" }
            chaser.submit(track.id, InputLog(emptyList()))
            val rival = requireNotNull(chaser.rivalGhost(track.id))
            rival.displayName.startsWith("Comet #") shouldBe true
            InputLog.fromCodes(rival.log) shouldBe fastLog
        }

    @Test
    fun `refused runs are dropped so they never block the queue`() =
        withServer { api ->
            val store = MemoryStore()
            val online = OnlineRacing(api, store) { "Pip" }
            online.submit("no-such-track", fastLog).shouldBeInstanceOf<Upload.Refused>()
            store.pending shouldBe emptyList()
            online.submit(track.id, fastLog).shouldBeInstanceOf<Upload.Verified>()
        }

    @Test
    fun `offline runs wait in the queue and go up later`(): Unit =
        runBlocking {
            val store = MemoryStore()
            val offline = OnlineRacing(KtorRaceApi("http://localhost", MockEngine { throw IOException("no network") }), store) { "Pip" }
            offline.submit(track.id, fastLog) shouldBe Upload.Queued
            offline.submit(track.id, fastLog) shouldBe Upload.Queued
            store.pending.size shouldBe 2
            offline.leaderboard(track.id, "daily") shouldBe null
            offline.rivalGhost(track.id) shouldBe null
        }

    @Test
    fun `queued runs upload in order once the server is back`() =
        withServer { api ->
            val store = MemoryStore()
            store.pending = listOf(PendingRun(track.id, InputLog(emptyList()).toCodes()), PendingRun(track.id, fastLog.toCodes()))
            val results = OnlineRacing(api, store) { "Bean" }.flush()
            results.size shouldBe 2
            results.all { it is Upload.Verified } shouldBe true
            store.pending shouldBe emptyList()
        }
}
