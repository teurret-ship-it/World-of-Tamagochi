package com.worldoftamagochi.sim.race

/**
 * A pet's race stats, 0..100 each. Grown by training and care (iteration 9),
 * never bought (CLAUDE.md section 2). Each stat shifts its physics value by at
 * most a few percent: together they may change a run by at most 10%.
 */
data class RaceStats(
    val speed: Int = 0,
    val stamina: Int = 0,
    val agility: Int = 0,
    val jump: Int = 0,
) {
    init {
        require(listOf(speed, stamina, agility, jump).all { it in MIN..MAX }) { "Stats are 0..100" }
    }

    companion object {
        const val MIN = 0
        const val MAX = 100
        val ROOKIE = RaceStats()
        val CHAMPION = RaceStats(MAX, MAX, MAX, MAX)
    }
}
