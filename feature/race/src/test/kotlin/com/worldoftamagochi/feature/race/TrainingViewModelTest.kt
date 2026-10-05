package com.worldoftamagochi.feature.race

import com.worldoftamagochi.data.GameSave
import com.worldoftamagochi.data.StatsSave
import com.worldoftamagochi.data.toSave
import com.worldoftamagochi.sim.LifeStage
import com.worldoftamagochi.sim.Need
import com.worldoftamagochi.sim.Needs
import com.worldoftamagochi.sim.PetState
import com.worldoftamagochi.sim.SleepWindow
import com.worldoftamagochi.sim.race.RaceStats
import com.worldoftamagochi.sim.training.Exercise
import com.worldoftamagochi.sim.training.TrainingRefusal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeGameRepository()
    private val noon = 1_790_000_000_000L - 1_790_000_000_000L % 86_400_000L + 12 * 3_600_000L

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun training(needs: Needs = Needs()): Pair<TrainingViewModel, MutableList<TrainingEffect>> {
        val pet = PetState(LifeStage.CHILD, needs, SleepWindow(), noon).toSave(seed = 7, name = "Mochi")
        runBlocking { repository.saveGame(GameSave(pet = pet)) }
        val vm = TrainingViewModel(repository, clock = { noon }, zone = ZoneOffset.UTC)
        val effects = mutableListOf<TrainingEffect>()
        TestScope(dispatcher).launch { vm.effects.toList(effects) }
        dispatcher.scheduler.runCurrent()
        return vm to effects
    }

    private val TrainingViewModel.ui: TrainingUiState get() = requireNotNull(state.value)

    @Test
    fun `a session raises the stat, costs energy and is saved`() {
        val (vm, effects) = training()
        assertEquals(3, vm.ui.sessionsLeft)
        assertEquals(FormHint.TOP, vm.ui.formHint)
        vm.onTrain(Exercise.HOOPS)
        dispatcher.scheduler.runCurrent()
        assertEquals(TrainingEffect.Trained(Exercise.HOOPS, 8), effects.single())
        assertEquals(RaceStats(agility = 8), vm.ui.stats)
        assertEquals(2, vm.ui.sessionsLeft)
        val saved = requireNotNull(repository.saved)
        assertEquals(StatsSave(agility = 8), saved.stats)
        assertEquals(Needs.FULL - Needs.points(12), saved.pet.energy)
    }

    @Test
    fun `three sessions a day, then a friendly no`() {
        val (vm, effects) = training()
        repeat(4) { vm.onTrain(Exercise.SPRINTS) }
        dispatcher.scheduler.runCurrent()
        assertEquals(TrainingEffect.Refused(TrainingRefusal.DONE_FOR_TODAY), effects.last())
        assertEquals(8 + 7 + 6, vm.ui.stats.speed)
        assertEquals(0, vm.ui.sessionsLeft)
    }

    @Test
    fun `a hungry pet is told to eat first, and the form says so`() {
        val (vm, effects) = training(Needs(satiety = Needs.points(10)))
        assertEquals(FormHint.HUNGRY, vm.ui.formHint)
        vm.onTrain(Exercise.JOGGING)
        dispatcher.scheduler.runCurrent()
        assertEquals(TrainingEffect.Refused(TrainingRefusal.TOO_HUNGRY), effects.single())
        assertEquals(Needs.points(10), repository.saved!!.pet.satiety)
        assertEquals(10, Needs(satiety = repository.saved!!.pet.satiety).gauge(Need.SATIETY).value)
    }
}
