package com.mymagicalpet.feature.home

import com.mymagicalpet.sim.CareAction
import com.mymagicalpet.sim.Expression
import com.mymagicalpet.sim.Genome
import com.mymagicalpet.sim.LifeStage
import com.mymagicalpet.sim.Need
import com.mymagicalpet.sim.Refusal
import com.mymagicalpet.sim.Reward
import com.mymagicalpet.sim.SleepWindow
import com.mymagicalpet.sim.Species

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
    /** Treats the player may still buy today, and what one costs. */
    val treatsLeft: Int = 0,
    val treatPrice: Int = 0,
    /** Ids of the cosmetics the pet wears. */
    val wearing: List<String> = emptyList(),
    val species: Species = Species.DRAGON,
    val stage: LifeStage = LifeStage.BABY,
    /** 0..1 towards the next life stage (1 when majestic). */
    val growthProgress: Float = 0f,
    /** True while a lights-off nap runs (the player can wake the pet). */
    val napping: Boolean = false,
    /** Woken during its night: up for a while, lights off sends it back to bed. */
    val upLate: Boolean = false,
    /** The sleep window, minutes after local midnight (settings). */
    val bedtimeMinute: Int = SleepWindow.DEFAULT_BEDTIME,
    val wakeMinute: Int = SleepWindow.DEFAULT_WAKE_UP,
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

    /** The creature grew into its next life stage. */
    data class Evolved(
        val stage: LifeStage,
    ) : HomeEffect
}
