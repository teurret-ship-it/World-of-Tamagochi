package com.mymagicalpet.sim.race

/**
 * A pet's race stats, 0..100 each, grown by training and never bought
 * (CLAUDE.md section 2). On race day a hungry or tired pet runs with lower
 * effective stats, down to [MIN] ([RaceForm]). Each stat shifts its physics
 * value by at most a few percent: together they change a run by at most 10%.
 */
data class RaceStats(
    val speed: Int = 0,
    val stamina: Int = 0,
    val agility: Int = 0,
    val jump: Int = 0,
) {
    init {
        require(listOf(speed, stamina, agility, jump).all { it in MIN..MAX }) { "Stats are $MIN..$MAX" }
    }

    companion object {
        /** A rookie in poor form. */
        const val MIN = -50
        const val MAX = 100
        val ROOKIE = RaceStats()
        val CHAMPION = RaceStats(MAX, MAX, MAX, MAX)
    }
}
