package com.worldoftamagochi.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldoftamagochi.sim.ExpressionRules
import com.worldoftamagochi.sim.Genome
import com.worldoftamagochi.sim.LifeStage
import com.worldoftamagochi.sim.Need
import com.worldoftamagochi.sim.Needs
import com.worldoftamagochi.sim.NeedsSimulation
import com.worldoftamagochi.sim.PetState
import com.worldoftamagochi.sim.SleepWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Drives the home screen: keeps the pet's needs moving in real time and turns
 * them into [HomeUiState]. The pet lives in memory until persistence arrives
 * (roadmap iteration 4).
 */
class HomeViewModel(
    private val clock: () -> Long = System::currentTimeMillis,
    seed: Long = clock(),
    zone: ZoneId = ZoneId.systemDefault(),
    private val name: String = DEFAULT_NAME,
    private val expressions: ExpressionRules = ExpressionRules.DEFAULT,
) : ViewModel() {
    private val genome = Genome.fromSeed(seed)
    private var pet =
        PetState(
            stage = LifeStage.BABY,
            needs = Needs(),
            sleep = SleepWindow(zone = zone),
            updatedAtEpochMillis = clock(),
        )
    private val _state = MutableStateFlow(render(strokes = 0))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                tick()
            }
        }
    }

    /** Moves the pet to the current time. Called by the ticker; public for tests. */
    fun tick() {
        pet = NeedsSimulation.advance(pet, clock())
        _state.value = render(_state.value.strokes)
    }

    /** The player rubbed the pet. A sleeping pet is not woken up by strokes. */
    fun onStroke() {
        if (_state.value.asleep) return
        _state.value = render(_state.value.strokes + 1)
    }

    private fun render(strokes: Int): HomeUiState {
        val asleep = pet.isAsleep()
        return HomeUiState(
            name = name,
            genome = genome,
            expression = expressions.expressionOf(pet.needs, asleep),
            needs = Need.entries.associateWith { pet.needs.gauge(it).value },
            urgentNeed = if (asleep) null else expressions.mostUrgentNeed(pet.needs),
            strokes = strokes,
        )
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
        const val DEFAULT_NAME = "Mochi"
    }
}
