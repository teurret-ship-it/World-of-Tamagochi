package com.worldoftamagochi.sim.race

import com.worldoftamagochi.sim.Need
import com.worldoftamagochi.sim.Needs

/**
 * Race-day form: a pet with a full tummy and energy races at its best; a
 * hungry or tired one runs below its stats, so care matters in competitions
 * (Chao Garden's lesson). Form only ever lowers stats, so the server needs
 * no proof of it: a client claiming top form gains nothing a fed pet lacks.
 */
object RaceForm {
    /** At or above this gauge on both tummy and energy, the pet is in top form. */
    const val TOP_FORM_GAUGE = 50

    /** Per-mille 0..1000: 1000 is top form, 0 a pet with an empty tummy or no energy. */
    fun perMille(needs: Needs): Int {
        val low = minOf(needs.gauge(Need.SATIETY).value, needs.gauge(Need.ENERGY).value)
        return (low * PER_MILLE / TOP_FORM_GAUGE).coerceAtMost(PER_MILLE)
    }

    /** The stats a pet races with today: scaled by form, minus up to 50 points in the worst form. */
    fun effective(
        stats: RaceStats,
        formPerMille: Int,
    ): RaceStats {
        val penalty = (PER_MILLE - formPerMille) * -RaceStats.MIN / PER_MILLE

        fun of(stat: Int) = (stat * formPerMille / PER_MILLE - penalty).coerceIn(RaceStats.MIN, RaceStats.MAX)
        return RaceStats(of(stats.speed), of(stats.stamina), of(stats.agility), of(stats.jump))
    }

    private const val PER_MILLE = 1_000
}
