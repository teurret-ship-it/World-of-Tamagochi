package com.worldoftamagochi.feature.home

import com.worldoftamagochi.sim.CareAction
import com.worldoftamagochi.sim.Expression
import com.worldoftamagochi.sim.Need
import com.worldoftamagochi.sim.Refusal
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

    private fun viewModel() = HomeViewModel(clock = { now }, seed = 7, zone = ZoneOffset.UTC)

    /** Collects the effects a block of interactions produced. */
    private fun HomeViewModel.effectsOf(block: HomeViewModel.() -> Unit): List<HomeEffect> {
        val seen = Channel<HomeEffect>(Channel.UNLIMITED)
        val scope = TestScope(dispatcher)
        val job = scope.launch { effects.collect { seen.trySend(it) } }
        scope.runCurrent()
        block()
        scope.runCurrent()
        job.cancel()
        return generateSequence { seen.tryReceive().getOrNull() }.toList()
    }

    @Test
    fun `a new pet starts full and happy, at level 1 with no coins`() {
        val state = viewModel().state.value
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
        val state = vm.state.value
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
            vm.state.value.needs
                .getValue(Need.SATIETY),
        )
        assertEquals(5L, vm.state.value.coins)
        assertTrue(vm.state.value.levelProgress > 0f)
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
        assertTrue(vm.state.value.washing)
        val effects = vm.effectsOf { repeat(4) { onStroke() } }
        assertTrue(effects.all { it is HomeEffect.Cared && it.action == CareAction.WASH })
        assertEquals(
            100,
            vm.state.value.needs
                .getValue(Need.HYGIENE),
        )
        assertFalse("soap is put down once clean", vm.state.value.washing)
    }

    @Test
    fun `lights off starts a nap and lights on ends it`() {
        val vm = viewModel()
        now = at(hour = 20) // baby energy 100 - 10 h * 6 = 40
        vm.tick()
        vm.onLights()
        assertTrue(vm.state.value.napping)
        assertTrue(vm.state.value.asleep)
        vm.onLights()
        assertFalse(vm.state.value.napping)
    }

    @Test
    fun `strokes count, but a sleeping pet is left in peace`() {
        val vm = viewModel()
        vm.onStroke()
        vm.onStroke()
        assertEquals(2, vm.state.value.strokes)

        now = at(hour = 23)
        vm.tick()
        assertTrue(vm.state.value.asleep)
        val effects = vm.effectsOf { onStroke() }
        assertEquals(2, vm.state.value.strokes)
        assertEquals(HomeEffect.Refused(CareAction.STROKE, Refusal.ASLEEP), effects.single())
        assertNull(vm.state.value.urgentNeed)
    }

    private fun at(hour: Int): Long = LocalDateTime.of(2026, 3, 2, hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
}
