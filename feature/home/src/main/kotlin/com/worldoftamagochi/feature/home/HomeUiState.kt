package com.worldoftamagochi.feature.home

import com.worldoftamagochi.sim.Expression
import com.worldoftamagochi.sim.Genome
import com.worldoftamagochi.sim.Need

/** Everything the home screen shows. Built from the pet state; no game logic in UI. */
data class HomeUiState(
    val name: String,
    val genome: Genome,
    val expression: Expression,
    /** Gauge value 0..100 per need, in display order. */
    val needs: Map<Need, Int>,
    val urgentNeed: Need?,
    /** Increments on every accepted stroke so the UI can play one reaction per stroke. */
    val strokes: Int = 0,
) {
    val asleep: Boolean get() = expression == Expression.ASLEEP
}
