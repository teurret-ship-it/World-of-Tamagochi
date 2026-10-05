package com.worldoftamagochi.sim.race

/** Trackmania-style medals, measured against the author time. */
enum class Medal {
    BRONZE,
    SILVER,
    GOLD,
    AUTHOR,
}

/** Medal times for one track, from the autopilot's rookie run. */
data class MedalTimes(
    val authorMicros: Long,
) {
    val gold: Long get() = authorMicros * GOLD_PER_MILLE / PER_MILLE
    val silver: Long get() = authorMicros * SILVER_PER_MILLE / PER_MILLE
    val bronze: Long get() = authorMicros * BRONZE_PER_MILLE / PER_MILLE

    fun medalFor(finishMicros: Long?): Medal? =
        when {
            finishMicros == null -> null
            finishMicros <= authorMicros -> Medal.AUTHOR
            finishMicros <= gold -> Medal.GOLD
            finishMicros <= silver -> Medal.SILVER
            finishMicros <= bronze -> Medal.BRONZE
            else -> null
        }

    companion object {
        private const val PER_MILLE = 1_000L
        private const val GOLD_PER_MILLE = 1_040L
        private const val SILVER_PER_MILLE = 1_120L
        private const val BRONZE_PER_MILLE = 1_300L

        /** Author time = the autopilot with rookie stats, so a skilled rookie can earn every medal. */
        fun of(track: Track): MedalTimes = MedalTimes(requireNotNull(Autopilot(track).play(RaceStats.ROOKIE).first.finishMicros))
    }
}
