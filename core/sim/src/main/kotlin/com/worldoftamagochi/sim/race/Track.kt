package com.worldoftamagochi.sim.race

import com.worldoftamagochi.sim.SeededRandom

/** Something on the track, at [startMm] (millimetres from the start line). */
sealed interface Obstacle {
    val startMm: Int
    val lengthMm: Int
    val endMm: Int get() = startMm + lengthMm

    /** Jump it; touching it makes the pet stumble. */
    data class Hurdle(
        override val startMm: Int,
        val heightMm: Int = HURDLE_HEIGHT_MM,
    ) : Obstacle {
        override val lengthMm: Int get() = HURDLE_LENGTH_MM
    }

    /** Running through it slows the pet down; jumping over it does not. */
    data class Puddle(
        override val startMm: Int,
        override val lengthMm: Int,
    ) : Obstacle

    /** Running over it gives a burst of speed and stamina. */
    data class Boost(
        override val startMm: Int,
    ) : Obstacle {
        override val lengthMm: Int get() = BOOST_LENGTH_MM
    }

    companion object {
        const val HURDLE_HEIGHT_MM = 350
        const val HURDLE_LENGTH_MM = 120
        const val BOOST_LENGTH_MM = 1_500
    }
}

/** A race course: a fixed length and a sorted list of obstacles. */
data class Track(
    val id: String,
    val lengthMm: Int,
    val obstacles: List<Obstacle>,
) {
    init {
        require(obstacles.zipWithNext().all { (a, b) -> a.endMm < b.startMm }) { "Obstacles must be sorted and apart" }
        require(obstacles.all { it.startMm > 0 && it.endMm < lengthMm }) { "Obstacles must be on the track" }
    }

    companion object {
        /**
         * Builds a course from a seed. Obstacles are spaced so every one is
         * clearable at full sprint: a jump covers about 4 m, and the gap after a
         * hurdle is never shorter than a landing plus a stride.
         */
        fun generate(
            id: String,
            seed: Long,
            lengthMm: Int,
            density: Int,
        ): Track {
            val random = SeededRandom(seed)
            val obstacles = mutableListOf<Obstacle>()
            var at = START_CLEAR_MM
            while (true) {
                at += random.nextInt(MIN_GAP_MM, MIN_GAP_MM + MAX_EXTRA_GAP_MM / density)
                if (at > lengthMm - FINISH_CLEAR_MM) break
                val obstacle =
                    when (random.nextInt(0, ROLL)) {
                        in 0 until HURDLE_ODDS -> {
                            Obstacle.Hurdle(at)
                        }

                        in HURDLE_ODDS until HURDLE_ODDS + PUDDLE_ODDS -> {
                            Obstacle.Puddle(at, random.nextInt(PUDDLE_MIN_MM, PUDDLE_MAX_MM))
                        }

                        else -> {
                            Obstacle.Boost(at)
                        }
                    }
                obstacles += obstacle
                at = obstacle.endMm
            }
            return Track(id, lengthMm, obstacles)
        }

        private const val START_CLEAR_MM = 15_000
        private const val FINISH_CLEAR_MM = 12_000
        private const val MIN_GAP_MM = 9_000
        private const val MAX_EXTRA_GAP_MM = 40_000
        private const val ROLL = 10
        private const val HURDLE_ODDS = 5
        private const val PUDDLE_ODDS = 3
        private const val PUDDLE_MIN_MM = 2_000
        private const val PUDDLE_MAX_MM = 3_500
    }
}

/** The three launch sprint courses: short and open, long and busy, and a hurdle maze. */
object SprintTracks {
    val MEADOW = Track.generate(id = "sprint-meadow", seed = 1_001, lengthMm = 220_000, density = 2)
    val BEACH = Track.generate(id = "sprint-beach", seed = 2_002, lengthMm = 260_000, density = 3)
    val SNOW = Track.generate(id = "sprint-snow", seed = 3_003, lengthMm = 300_000, density = 4)

    val ALL = listOf(MEADOW, BEACH, SNOW)

    fun byId(id: String): Track? = ALL.firstOrNull { it.id == id }
}
