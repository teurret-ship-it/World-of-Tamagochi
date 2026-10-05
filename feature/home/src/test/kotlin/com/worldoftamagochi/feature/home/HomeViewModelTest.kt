package com.worldoftamagochi.feature.home

import com.worldoftamagochi.sim.Expression
import com.worldoftamagochi.sim.Need
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private var now = at(hour = 10)

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = HomeViewModel(clock = { now }, seed = 7, zone = ZoneOffset.UTC)

    @Test
    fun `a new pet starts full and happy`() {
        val state = viewModel().state.value
        assertEquals(Expression.HAPPY, state.expression)
        assertTrue(state.needs.values.all { it == 100 })
        assertNull(state.urgentNeed)
        assertEquals(Need.entries, state.needs.keys.toList())
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
    fun `strokes count, but a sleeping pet is left in peace`() {
        val vm = viewModel()
        vm.onStroke()
        vm.onStroke()
        assertEquals(2, vm.state.value.strokes)

        now = at(hour = 23)
        vm.tick()
        assertTrue(vm.state.value.asleep)
        vm.onStroke()
        assertEquals(2, vm.state.value.strokes)
        assertNull(vm.state.value.urgentNeed)
    }

    private fun at(hour: Int): Long = LocalDateTime.of(2026, 3, 2, hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
}
