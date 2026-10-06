package com.mymagicalpet.sim

import com.mymagicalpet.model.Gauge

/** The needs a player looks after. 100 = fully satisfied, 0 = empty. */
enum class Need {
    SATIETY,
    ENERGY,
    HYGIENE,
    HAPPINESS,
    HEALTH,
}

/**
 * Need levels in exact integer units.
 *
 * One gauge point is [UNITS_PER_POINT] units, chosen so that a rate of N tenths
 * of a point per hour is exactly N units per millisecond. Decay therefore never
 * rounds, and simulating 3 days in one step gives bit-for-bit the same state as
 * simulating them minute by minute.
 */
data class Needs(
    val satiety: Long = FULL,
    val energy: Long = FULL,
    val hygiene: Long = FULL,
    val happiness: Long = FULL,
    val health: Long = FULL,
) {
    init {
        listOf(satiety, energy, hygiene, happiness, health).forEach {
            require(it in 0..FULL) { "Need level $it outside 0..$FULL" }
        }
    }

    operator fun get(need: Need): Long =
        when (need) {
            Need.SATIETY -> satiety
            Need.ENERGY -> energy
            Need.HYGIENE -> hygiene
            Need.HAPPINESS -> happiness
            Need.HEALTH -> health
        }

    /**
     * Display value. Rounds up, so a bar shows 0 only when the need is truly
     * empty: a pet at 0.4 points is "almost empty", not "starving".
     */
    fun gauge(need: Need): Gauge = Gauge.of(((this[need] + UNITS_PER_POINT - 1) / UNITS_PER_POINT).toInt())

    fun with(
        need: Need,
        level: Long,
    ): Needs {
        val clamped = level.coerceIn(0, FULL)
        return when (need) {
            Need.SATIETY -> copy(satiety = clamped)
            Need.ENERGY -> copy(energy = clamped)
            Need.HYGIENE -> copy(hygiene = clamped)
            Need.HAPPINESS -> copy(happiness = clamped)
            Need.HEALTH -> copy(health = clamped)
        }
    }

    companion object {
        /** Units in one gauge point: 1 tenth of a point per hour == 1 unit per ms. */
        const val UNITS_PER_POINT: Long = 36_000_000L
        const val FULL: Long = 100 * UNITS_PER_POINT

        fun points(points: Int): Long = points * UNITS_PER_POINT
    }
}
