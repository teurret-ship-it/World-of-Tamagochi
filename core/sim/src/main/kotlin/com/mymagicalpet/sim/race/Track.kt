package com.mymagicalpet.sim.race

import com.mymagicalpet.sim.SeededRandom

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

    /** Agility: jump through the ring. Passing under it or over its top is a fault. */
    data class Tyre(
        override val startMm: Int,
    ) : Obstacle {
        override val lengthMm: Int get() = TYRE_LENGTH_MM
        val centerMm: Int get() = startMm + TYRE_LENGTH_MM / 2
    }

    /** Agility: crawl through, slowly, catching your breath. Arriving in the air bumps the pet's head: a fault. */
    data class Tunnel(
        override val startMm: Int,
        override val lengthMm: Int,
    ) : Obstacle

    /** Agility: walk over it. Arriving too fast, or jumping onto it, bounces the pet off: a fault. */
    data class Seesaw(
        override val startMm: Int,
    ) : Obstacle {
        override val lengthMm: Int get() = SEESAW_LENGTH_MM
    }

    companion object {
        const val HURDLE_HEIGHT_MM = 350
        const val HURDLE_LENGTH_MM = 120
        const val BOOST_LENGTH_MM = 1_500
        const val TYRE_LENGTH_MM = 150
        const val TYRE_BOTTOM_MM = 180
        const val TYRE_TOP_MM = 640
        const val SEESAW_LENGTH_MM = 3_000
    }
}

/** Sprint: pure speed. Agility: an obstacle course where faults add time. */
enum class Discipline { SPRINT, AGILITY }

/** A race course: a fixed length and a sorted list of obstacles. */
data class Track(
    val id: String,
    val lengthMm: Int,
    val obstacles: List<Obstacle>,
    val discipline: Discipline = Discipline.SPRINT,
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

/**
 * The three launch agility courses, laid out by hand like a real ring: a
 * gentle first course, a hoop course and a long trail with everything.
 * Distances are in metres for readability.
 */
object AgilityTracks {
    val PARK =
        course("agility-park", lengthM = 140) {
            hurdle(14)
            tyre(26)
            tunnel(38, lengthM = 6)
            hurdle(54)
            seesaw(66)
            tyre(82)
            hurdle(94)
            tunnel(106, lengthM = 5)
            hurdle(122)
        }

    val HILLS =
        course("agility-hills", lengthM = 170) {
            tyre(14)
            tyre(25)
            hurdle(36)
            seesaw(48)
            tyre(62)
            tunnel(74, lengthM = 8)
            tyre(92)
            hurdle(103)
            seesaw(114)
            tyre(130)
            hurdle(142)
            tyre(154)
        }

    val TRAIL =
        course("agility-trail", lengthM = 200) {
            hurdle(12)
            add(Obstacle.Puddle(22 * MM, 3 * MM))
            tyre(32)
            tunnel(44, lengthM = 6)
            add(Obstacle.Boost(58 * MM))
            seesaw(72)
            hurdle(86)
            tyre(96)
            tunnel(108, lengthM = 10)
            hurdle(126)
            seesaw(138)
            tyre(154)
            add(Obstacle.Boost(164 * MM))
            hurdle(176)
            tyre(186)
        }

    val ALL = listOf(PARK, HILLS, TRAIL)

    private const val MM = 1_000

    private fun course(
        id: String,
        lengthM: Int,
        build: MutableList<Obstacle>.() -> Unit,
    ): Track = Track(id, lengthM * MM, mutableListOf<Obstacle>().apply(build), Discipline.AGILITY)

    private fun MutableList<Obstacle>.hurdle(atM: Int) = add(Obstacle.Hurdle(atM * MM))

    private fun MutableList<Obstacle>.tyre(atM: Int) = add(Obstacle.Tyre(atM * MM))

    private fun MutableList<Obstacle>.seesaw(atM: Int) = add(Obstacle.Seesaw(atM * MM))

    private fun MutableList<Obstacle>.tunnel(
        atM: Int,
        lengthM: Int,
    ) = add(Obstacle.Tunnel(atM * MM, lengthM * MM))
}

/** Every course the game knows: the app lists these, the server verifies against them. */
object Tracks {
    val ALL: List<Track> = SprintTracks.ALL + AgilityTracks.ALL

    fun byId(id: String): Track? = ALL.firstOrNull { it.id == id }
}
