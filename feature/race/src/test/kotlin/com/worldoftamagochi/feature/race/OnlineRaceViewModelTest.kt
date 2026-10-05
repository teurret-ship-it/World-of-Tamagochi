package com.worldoftamagochi.feature.race

import com.worldoftamagochi.api.GhostRun
import com.worldoftamagochi.api.GhostsResponse
import com.worldoftamagochi.api.LeaderboardResponse
import com.worldoftamagochi.api.RegisterResponse
import com.worldoftamagochi.api.RunRequest
import com.worldoftamagochi.api.RunResponse
import com.worldoftamagochi.data.GameSave
import com.worldoftamagochi.data.SaveOnlineStore
import com.worldoftamagochi.data.toSave
import com.worldoftamagochi.network.OnlineRacing
import com.worldoftamagochi.network.RaceApi
import com.worldoftamagochi.sim.LifeStage
import com.worldoftamagochi.sim.Needs
import com.worldoftamagochi.sim.PetState
import com.worldoftamagochi.sim.SleepWindow
import com.worldoftamagochi.sim.race.Autopilot
import com.worldoftamagochi.sim.race.InputLog
import com.worldoftamagochi.sim.race.RaceStats
import com.worldoftamagochi.sim.race.SprintTracks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineRaceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeGameRepository()
    private val track = SprintTracks.MEADOW
    private val rivalLog = Autopilot(track).play(RaceStats.ROOKIE).second

    /** A server that always has one rival and ranks every run 4th. */
    private val api =
        object : RaceApi {
            val uploads = mutableListOf<RunRequest>()

            override suspend fun register(petName: String) = RegisterResponse("id", "token", "$petName #1234")

            override suspend fun submitRun(
                token: String,
                request: RunRequest,
            ): RunResponse {
                uploads += request
                return RunResponse(finishMicros = 1, personalBestMicros = 1, dailyRank = 4)
            }

            override suspend fun leaderboard(
                trackId: String,
                period: String,
                token: String?,
            ) = LeaderboardResponse(trackId, period, emptyList())

            override suspend fun ghosts(
                token: String,
                trackId: String,
            ) = GhostsResponse(trackId, listOf(GhostRun("Comet #4821", 31_000_000, rivalLog.toCodes())))
        }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val pet = PetState(LifeStage.CHILD, Needs(), SleepWindow(), 0).toSave(seed = 7, name = "Pip")
        runBlocking { repository.saveGame(GameSave(pet = pet)) }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `online races a real rival and reports today's rank after the finish`() {
        val online = OnlineRacing(api, SaveOnlineStore(repository)) { "Pip" }
        val vm = RaceViewModel(repository, track, clock = { 0 }, zone = ZoneOffset.UTC, online = online)
        dispatcher.scheduler.runCurrent()
        val ui = requireNotNull(vm.state.value)
        assertEquals(GhostKind.RIVAL, ui.ghostKind)
        assertEquals("Comet #4821", ui.ghostName)

        vm.onFrame(0)
        var tick = 0
        while (requireNotNull(vm.state.value).phase != RacePhase.FINISHED) {
            tick++
            vm.onFrame(3_000_000_000L + tick * (1_000_000_000L / 60))
        }
        dispatcher.scheduler.runCurrent()
        val result = requireNotNull(requireNotNull(vm.state.value).result)
        assertEquals(OnlineOutcome.Ranked(4), result.online)
        assertEquals(track.id, api.uploads.single().trackId)
        assertEquals("Pip #1234", repository.saved?.online?.displayName)
        assertEquals(emptyList<Any>(), repository.saved?.online?.pending)
        // The upload is exactly what was played.
        assertEquals(InputLog(emptyList()), InputLog.fromCodes(api.uploads.single().log))
    }
}
