package com.mymagicalpet.feature.home

import com.mymagicalpet.data.toState
import com.mymagicalpet.sim.CareAction
import com.mymagicalpet.sim.Expression
import com.mymagicalpet.sim.LifeStage
import com.mymagicalpet.sim.Need
import com.mymagicalpet.sim.Needs
import com.mymagicalpet.sim.Refusal
import com.mymagicalpet.ui.time.shiftTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private var now = at(hour = 10)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val repository = FakeGameRepository()

    private fun viewModel(): HomeViewModel =
        HomeViewModel(repository, clock = { now }, zone = ZoneOffset.UTC, newPetSeed = { 7 }).also {
            dispatcher.scheduler.runCurrent() // loads the save
        }

    private val HomeViewModel.ui: HomeUiState get() = requireNotNull(state.value)

    /** Collects the effects a block of interactions produced. */
    private fun HomeViewModel.effectsOf(block: HomeViewModel.() -> Unit): List<HomeEffect> {
        val seen = Channel<HomeEffect>(Channel.UNLIMITED)
        val scope = TestScope(dispatcher)
        val job = scope.launch { effects.collect { seen.trySend(it) } }
        scope.runCurrent()
        // Effects of earlier interactions are still buffered in the channel: skip them.
        generateSequence { seen.tryReceive().getOrNull() }.toList()
        block()
        scope.runCurrent()
        job.cancel()
        return generateSequence { seen.tryReceive().getOrNull() }.toList()
    }

    @Test
    fun `a new pet starts full and happy, at level 1 with no coins`() {
        val state = viewModel().ui
        assertEquals(Expression.HAPPY, state.expression)
        assertTrue(state.needs.values.all { it == 100 })
        assertNull(state.urgentNeed)
        assertEquals(1, state.level)
        assertEquals(0L, state.coins)
    }

    @Test
    fun `time passing makes the pet hungry and the screen says so`() {
        val vm = viewModel()
        now = at(hour = 16) // a baby awake for 6 hours: tummy 100 -> 16
        vm.tick()
        val state = vm.ui
        assertEquals(16, state.needs.getValue(Need.SATIETY))
        assertEquals(Expression.HUNGRY, state.expression)
        assertEquals(Need.SATIETY, state.urgentNeed)
    }

    @Test
    fun `feeding a hungry pet fills the tummy and pays coins and XP`() {
        val vm = viewModel()
        now = at(hour = 16)
        val effects = vm.effectsOf { onFeed() }
        val cared = effects.single() as HomeEffect.Cared
        assertEquals(CareAction.FEED, cared.action)
        assertEquals(35, cared.changes[Need.SATIETY])
        assertEquals(5, cared.reward.coins)
        assertEquals(
            51,
            vm.ui.needs
                .getValue(Need.SATIETY),
        )
        assertEquals(5L, vm.ui.coins)
        assertTrue(vm.ui.levelProgress > 0f)
    }

    @Test
    fun `a full pet refuses food, politely`() {
        val vm = viewModel()
        val effects = vm.effectsOf { onFeed() }
        assertEquals(HomeEffect.Refused(CareAction.FEED, Refusal.FULL), effects.single())
    }

    @Test
    fun `holding the soap turns strokes into scrubs until the pet is clean`() {
        val vm = viewModel()
        now = at(hour = 18) // hygiene 100 - 8 h * 8 = 36
        vm.tick()
        vm.onToggleSoap()
        assertTrue(vm.ui.washing)
        val effects = vm.effectsOf { repeat(4) { onStroke() } }
        assertTrue(effects.all { it is HomeEffect.Cared && it.action == CareAction.WASH })
        assertEquals(
            100,
            vm.ui.needs
                .getValue(Need.HYGIENE),
        )
        assertFalse("soap is put down once clean", vm.ui.washing)
    }

    @Test
    fun `lights off starts a nap and lights on ends it`() {
        val vm = viewModel()
        now = at(hour = 20) // baby energy 100 - 10 h * 6 = 40
        vm.tick()
        vm.onLights()
        assertTrue(vm.ui.napping)
        assertTrue(vm.ui.asleep)
        vm.onLights()
        assertFalse(vm.ui.napping)
    }

    @Test
    fun `strokes count, but a sleeping pet is left in peace`() {
        val vm = viewModel()
        vm.onStroke()
        vm.onStroke()
        assertEquals(2, vm.ui.strokes)

        now = at(hour = 23)
        vm.tick()
        assertTrue(vm.ui.asleep)
        val effects = vm.effectsOf { onStroke() }
        assertEquals(2, vm.ui.strokes)
        assertEquals(HomeEffect.Refused(CareAction.STROKE, Refusal.ASLEEP), effects.single())
        assertNull(vm.ui.urgentNeed)
    }

    @Test
    fun `the game is saved after care and reloaded with the same pet, coins and time`() {
        val vm = viewModel()
        now = at(hour = 16)
        vm.onFeed()
        dispatcher.scheduler.runCurrent()
        val saved = requireNotNull(repository.saved)
        assertEquals(5L, saved.progress.coins)
        assertEquals(7L, saved.pet.seed)

        now = at(hour = 16) + 10 * 60_000
        val again = viewModel()
        assertEquals(5L, again.ui.coins)
        assertEquals(vm.ui.genome, again.ui.genome)
        assertNull("10 minutes is not 'away'", again.ui.away)
    }

    @Test
    fun `coming back after hours shows what changed, once`() {
        viewModel()
        now = at(hour = 15)
        val back = viewModel()
        val away = requireNotNull(back.ui.away)
        assertEquals(300L, away.minutes)
        assertEquals(-70, away.changes[Need.SATIETY])
        back.onDismissAway()
        assertNull(back.ui.away)
    }

    @Test
    fun `coins paid elsewhere (a race) show up and are not overwritten by saving the pet`() {
        val vm = viewModel()
        kotlinx.coroutines.runBlocking { repository.updateGame { it.copy(progress = it.progress.copy(coins = 40)) } }
        dispatcher.scheduler.runCurrent()
        assertEquals(40L, vm.ui.coins)
        repeat(20) { vm.tick() } // triggers the periodic pet save
        dispatcher.scheduler.runCurrent()
        assertEquals(40L, requireNotNull(repository.saved).progress.coins)
    }

    @Test
    fun `treats cost coins, cheer the pet up and stop after three a day`() {
        val vm = viewModel()
        now = at(hour = 16) // hungry, so not too full for a treat
        assertEquals(Refusal.NO_COINS, (vm.effectsOf { onTreat() }.single() as HomeEffect.Refused).reason)
        kotlinx.coroutines.runBlocking { repository.updateGame { it.copy(progress = it.progress.copy(coins = 100)) } }
        dispatcher.scheduler.runCurrent()
        assertEquals(3, vm.ui.treatsLeft)
        val treat = vm.effectsOf { onTreat() }.single() as HomeEffect.Cared
        assertEquals(CareAction.TREAT, treat.action)
        assertEquals(10, treat.changes[Need.SATIETY])
        assertEquals(0, treat.reward.coins)
        assertEquals(85L, vm.ui.coins)
        vm.onTreat()
        vm.onTreat()
        assertEquals(Refusal.NO_TREATS_LEFT, (vm.effectsOf { onTreat() }.single() as HomeEffect.Refused).reason)
        assertEquals(0, vm.ui.treatsLeft)
        dispatcher.scheduler.runCurrent()
        assertEquals(55L, requireNotNull(repository.saved).progress.coins)
    }

    @Test
    fun `a pet changed elsewhere (training) is adopted, not overwritten`() {
        val vm = viewModel()
        kotlinx.coroutines.runBlocking {
            repository.updateGame { it.copy(pet = it.pet.copy(energy = it.pet.energy - Needs.points(30))) }
        }
        dispatcher.scheduler.runCurrent()
        assertEquals(70, vm.ui.needs.getValue(Need.ENERGY))
        repeat(20) { vm.tick() } // the periodic save keeps the trained pet
        dispatcher.scheduler.runCurrent()
        assertEquals(
            70,
            requireNotNull(repository.saved)
                .pet
                .toState()
                .needs
                .gauge(Need.ENERGY)
                .value,
        )
    }

    @Test
    fun `at night the pet can be woken to play, and lights off sends it back to bed`() {
        val vm = viewModel()
        now = at(hour = 23)
        vm.tick()
        assertTrue(vm.ui.asleep)
        val woken = vm.effectsOf { onLights() }.single() as HomeEffect.Cared
        assertEquals(CareAction.WAKE, woken.action)
        assertFalse(vm.ui.asleep)
        assertTrue(vm.ui.upLate)
        assertEquals(CareAction.PLAY, (vm.effectsOf { onPlay() }.single() as HomeEffect.Cared).action)
        vm.onLights()
        assertTrue(vm.ui.asleep)
        assertFalse(vm.ui.upLate)
    }

    @Test
    fun `the sleep time follows the player's settings`() {
        val vm = viewModel()
        now = at(hour = 23)
        vm.onSleepWindow(bedtimeMinute = 23 * 60 + 30, wakeMinute = 8 * 60)
        dispatcher.scheduler.runCurrent()
        assertFalse(vm.ui.asleep)
        assertEquals(23 * 60 + 30, vm.ui.bedtimeMinute)
        assertEquals(23 * 60 + 30, requireNotNull(repository.saved).pet.sleepStartMinute)
        assertEquals(0, shiftTime(23 * 60 + 30, 30))
        assertEquals(23 * 60 + 30, shiftTime(0, -30))
    }

    @Test
    fun `care makes the creature grow, and reaching a stage is celebrated`() {
        val vm = viewModel()
        kotlinx.coroutines.runBlocking { repository.updateGame { it.copy(pet = it.pet.copy(growthPoints = 18)) } }
        dispatcher.scheduler.runCurrent()
        now = at(hour = 16) // hungry: feeding answers a need (+4 growth)
        val effects = vm.effectsOf { onFeed() }
        assertEquals(HomeEffect.Evolved(LifeStage.CHILD), effects.last())
        assertEquals(LifeStage.CHILD, vm.ui.stage)
        dispatcher.scheduler.runCurrent()
        repeat(20) { vm.tick() }
        dispatcher.scheduler.runCurrent()
        assertEquals(22, requireNotNull(repository.saved).pet.growthPoints)
        assertEquals(LifeStage.CHILD, requireNotNull(repository.saved).pet.stage)
        // Strokes never count towards growth.
        assertTrue(vm.effectsOf { onStroke() }.none { it is HomeEffect.Evolved })
    }

    @Test
    fun `settings toggles are stored`() {
        val vm = viewModel()
        vm.onSoundToggled(false)
        dispatcher.scheduler.runCurrent()
        assertFalse(vm.ui.sound)
        assertTrue(vm.ui.haptics)
    }

    private fun at(hour: Int): Long = LocalDateTime.of(2026, 3, 2, hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
}
