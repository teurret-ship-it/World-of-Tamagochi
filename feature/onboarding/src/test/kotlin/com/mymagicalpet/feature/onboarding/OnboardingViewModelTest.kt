package com.mymagicalpet.feature.onboarding

import com.mymagicalpet.data.GameSave
import com.mymagicalpet.data.ProgressSave
import com.mymagicalpet.data.StatsSave
import com.mymagicalpet.data.toSave
import com.mymagicalpet.sim.LifeStage
import com.mymagicalpet.sim.Needs
import com.mymagicalpet.sim.PetNames
import com.mymagicalpet.sim.PetState
import com.mymagicalpet.sim.SleepWindow
import com.mymagicalpet.sim.Species
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneOffset
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeGameRepository()
    private val now = 1_790_000_000_000L

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun onboarding() = OnboardingViewModel(repository, clock = { now }, zone = ZoneOffset.UTC, random = Random(7))

    private val OnboardingViewModel.ui: OnboardingUiState get() = state.value

    @Test
    fun `pick an egg, a name and a bedtime, then hatch the pet with five taps`() {
        val vm = onboarding()
        assertEquals(Species.entries, vm.ui.eggs.map(Species::of)) // one egg of every species
        assertFalse(vm.ui.canContinue)
        vm.onNext() // nothing chosen yet: stays
        assertEquals(OnboardingStep.CHOOSE_EGG, vm.ui.step)
        val egg = vm.ui.eggs[2]
        vm.onChooseEgg(egg)
        vm.onNext()
        assertEquals(OnboardingStep.NAME, vm.ui.step)
        assertTrue(vm.ui.names.all(PetNames::isAllowed))
        vm.onPickName("Not On The List")
        assertNull(vm.ui.name)
        vm.onPickName(vm.ui.names.first())
        vm.onNext()
        vm.onSleepWindow(21 * 60, 6 * 60 + 30)
        vm.onNext()
        assertEquals(OnboardingStep.HATCH, vm.ui.step)
        repeat(4) { vm.onTapEgg() }
        assertFalse(vm.ui.hatched)
        vm.onBack() // no going back once the egg is cracking
        assertEquals(OnboardingStep.HATCH, vm.ui.step)
        vm.onTapEgg()
        assertTrue(vm.ui.hatched)
        dispatcher.scheduler.runCurrent()

        val pet = requireNotNull(repository.saved).pet
        assertEquals(egg, pet.seed)
        assertEquals(vm.ui.name, pet.name)
        assertEquals(LifeStage.BABY, pet.stage)
        assertEquals(21 * 60, pet.sleepStartMinute)
        assertEquals(6 * 60 + 30, pet.sleepEndMinute)
        assertEquals(now, pet.updatedAtEpochMillis)
    }

    @Test
    fun `other eggs and other names on request, and a chosen egg must be on offer`() {
        val vm = onboarding()
        val first = vm.ui.eggs
        vm.onChooseEgg(first[0])
        vm.onMoreEggs()
        assertNotEquals(first, vm.ui.eggs)
        assertNull(vm.ui.chosenEgg)
        vm.onChooseEgg(first[0])
        assertNull(vm.ui.chosenEgg)
        val names = vm.ui.names
        vm.onMoreNames()
        assertEquals(8, vm.ui.names.size)
        assertNotEquals(names, vm.ui.names)
    }

    @Test
    fun `a new pet in an existing game keeps the coins but starts its own training`() {
        val old = PetState(LifeStage.ADULT, Needs(), SleepWindow(), 0).toSave(seed = 1, name = "Pip")
        runBlocking { repository.saveGame(GameSave(pet = old, progress = ProgressSave(coins = 77), stats = StatsSave(speed = 40))) }
        val vm = onboarding()
        vm.onChooseEgg(vm.ui.eggs[0])
        vm.onNext()
        vm.onPickName(vm.ui.names[0])
        vm.onNext()
        vm.onNext()
        repeat(5) { vm.onTapEgg() }
        dispatcher.scheduler.runCurrent()
        val saved = requireNotNull(repository.saved)
        assertEquals(77L, saved.progress.coins)
        assertEquals(StatsSave(), saved.stats)
        assertEquals(vm.ui.eggs[0], saved.pet.seed)
    }
}
