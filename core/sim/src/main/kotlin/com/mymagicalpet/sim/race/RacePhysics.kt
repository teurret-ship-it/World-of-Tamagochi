package com.mymagicalpet.sim.race

/**
 * Every physics number of the race, in integer units: millimetres, ticks
 * (1/60 s) and per-mille. Integers only, so the app, the server and every
 * future version compute bit-for-bit the same run.
 */
data class RacePhysics(
    val cruiseSpeed: Int = 100,
    val sprintSpeed: Int = 150,
    val tiredSpeed: Int = 72,
    val puddleSpeed: Int = 55,
    val acceleration: Int = 3,
    val deceleration: Int = 5,
    val staminaMax: Int = 1_000,
    val sprintDrain: Int = 7,
    val staminaRegen: Int = 3,
    /** After running dry the pet must recover to this much stamina before sprinting again. */
    val recoverTo: Int = 250,
    val jumpImpulse: Int = 66,
    val gravity: Int = 5,
    val stumbleTicks: Int = 24,
    val boostSpeed: Int = 45,
    val boostStamina: Int = 250,
    /** Agility: speed while crawling through a tunnel and the fastest safe speed onto a seesaw. */
    val tunnelSpeed: Int = 60,
    val seesawSafeSpeed: Int = 110,
    /** Agility: every fault adds this much to the time (2 s). */
    val faultPenaltyTicks: Int = 120,
    /** Per-mille bonus at stat 100 (linear from 0). */
    val speedStatBonus: Int = 35,
    val staminaStatBonus: Int = 120,
    val jumpStatBonus: Int = 60,
    val agilityStatBonus: Int = 300,
) {
    internal fun scaled(
        base: Int,
        stat: Int,
        bonusAtMax: Int,
    ): Int = base * (PER_MILLE + bonusAtMax * stat / RaceStats.MAX) / PER_MILLE

    companion object {
        val DEFAULT = RacePhysics()
        const val TICKS_PER_SECOND = 60
        private const val PER_MILLE = 1_000
    }
}
