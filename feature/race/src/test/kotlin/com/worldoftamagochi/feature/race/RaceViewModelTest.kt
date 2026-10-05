package com.worldoftamagochi.feature.race

import com.worldoftamagochi.data.GameSave
import com.worldoftamagochi.data.toSave
import com.worldoftamagochi.sim.LifeStage
import com.worldoftamagochi.sim.Needs
import com.worldoftamagochi.sim.PetState
import com.worldoftamagochi.sim.SleepWindow
import com.worldoftamagochi.sim.race.Autopilot
import com.worldoftamagochi.sim.race.InputLog
import com.worldoftamagochi.sim.race.Medal
import com.worldoftamagochi.sim.race.SprintTracks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class RaceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeGameRepository()
    private val track = SprintTracks.MEADOW

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val pet = PetState(LifeStage.CHILD, Needs(), SleepWindow(), 0).toSave(seed = 7, name = "Mochi")
        runBlocking { repository.saveGame(GameSave(pet = pet)) }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        RaceViewModel(repository, track, clock = { 1_790_000_000_000 }, zone = ZoneOffset.UTC).also {
            dispatcher.scheduler.runCurrent()
        }

    private val RaceViewModel.ui: RaceUiState get() = requireNotNull(state.value)

    /** Plays the race the way the autopilot would, one display frame per tick. */
    private fun RaceViewModel.playLikeAutopilot() {
        val pilot = Autopilot(track)
        onFrame(START)
        var tick = 0
        while (ui.phase != RacePhase.FINISHED) {
            val input = pilot.next(ui.runner)
            onSprint(input.sprint)
            if (input.jump) onJump()
            tick++
            onFrame(START + COUNTDOWN + tick * TICK)
        }
        dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `a race opens with a countdown and the coach as ghost`() {
        val vm = viewModel()
        assertEquals(RacePhase.COUNTDOWN, vm.ui.phase)
        assertEquals(3, vm.ui.countdown)
        assertEquals(GhostKind.COACH, vm.ui.ghostKind)
        vm.onFrame(START)
        vm.onFrame(START + 1_500_000_000)
        assertEquals(2, vm.ui.countdown)
        vm.onFrame(START + COUNTDOWN + TICK)
        assertEquals(RacePhase.RUNNING, vm.ui.phase)
        assertEquals(1, vm.ui.runner.tick)
    }

    @Test
    fun `matching the coach earns the trophy, every medal tier and a saved record`() {
        val vm = viewModel()
        vm.playLikeAutopilot()
        val result = requireNotNull(vm.ui.result)
        assertEquals(Medal.AUTHOR, result.medal)
        assertTrue(result.newRecord)
        assertEquals(3 + 5 + 10 + 20 + 30, result.reward.coins)
        assertNull(result.nextMedal)

        val saved = requireNotNull(repository.saved)
        assertEquals(68L, saved.progress.coins)
        val record = requireNotNull(saved.records[track.id])
        assertEquals(result.finishMicros, record.finishMicros)
        assertEquals(Autopilot(track).play(com.worldoftamagochi.sim.race.RaceStats.ROOKIE).second, InputLog.fromCodes(record.log))
    }

    @Test
    fun `racing again races your best and only pays for finishing`() {
        viewModel().playLikeAutopilot()
        val again = viewModel()
        assertEquals(GhostKind.PERSONAL_BEST, again.ui.ghostKind)
        assertNotNull(again.ui.bestMicros)
        again.playLikeAutopilot()
        val result = requireNotNull(again.ui.result)
        assertEquals(3, result.reward.coins)
        assertEquals(false, result.newRecord)
    }

    @Test
    fun `retry resets the run`() {
        val vm = viewModel()
        vm.playLikeAutopilot()
        vm.onRetry()
        assertEquals(RacePhase.COUNTDOWN, vm.ui.phase)
        assertEquals(0, vm.ui.runner.tick)
        assertNull(vm.ui.result)
    }

    private companion object {
        const val START = 10_000_000_000L
        const val COUNTDOWN = 3_000_000_000L
        const val TICK = 1_000_000_000L / 60
    }
}
