package com.worldoftamagochi.feature.home

import com.worldoftamagochi.sim.CareAction
import com.worldoftamagochi.sim.Expression
import com.worldoftamagochi.sim.Genome
import com.worldoftamagochi.sim.Need
import com.worldoftamagochi.sim.Refusal
import com.worldoftamagochi.sim.Reward

/** Everything the home screen shows. Built from the pet state; no game logic in UI. */
data class HomeUiState(
    val name: String,
    val genome: Genome,
    val expression: Expression,
    /** Gauge value 0..100 per need, in display order. */
    val needs: Map<Need, Int>,
    val urgentNeed: Need?,
    val level: Int = 1,
    /** Progress through the current level, 0..1. */
    val levelProgress: Float = 0f,
    val coins: Long = 0,
    /** True while a lights-off nap runs (the player can wake the pet). */
    val napping: Boolean = false,
    /** True while the soap is in hand: rubbing the pet washes it. */
    val washing: Boolean = false,
    /** Increments on every accepted stroke so the UI can play one reaction per stroke. */
    val strokes: Int = 0,
    /** What changed while the player was away; shown once as a welcome-back card. */
    val away: AwaySummary? = null,
    val sound: Boolean = true,
    val haptics: Boolean = true,
) {
    val asleep: Boolean get() = expression == Expression.ASLEEP
}

/** The welcome-back card: how long the player was gone and what changed. */
data class AwaySummary(
    val minutes: Long,
    /** Gauge points per need (negative = dropped), only needs that changed. */
    val changes: Map<Need, Int>,
)

/** One-off things the screen should celebrate or explain. */
sealed interface HomeEffect {
    data class Cared(
        val action: CareAction,
        val changes: Map<Need, Int>,
        val reward: Reward,
        val levelUp: Int?,
    ) : HomeEffect

    data class Refused(
        val action: CareAction,
        val reason: Refusal,
    ) : HomeEffect
}
