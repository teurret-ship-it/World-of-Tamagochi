package com.mymagicalpet.sim

/**
 * SplitMix64: a tiny, fully specified PRNG. Game rules use it instead of
 * `kotlin.random.Random` so that the same seed gives the same pet, track or
 * race on every device, server and Kotlin version, forever.
 */
class SeededRandom(
    seed: Long,
) {
    private var state: Long = seed

    fun nextLong(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr SHIFT_1)) * MIX_1
        z = (z xor (z ushr SHIFT_2)) * MIX_2
        return z xor (z ushr SHIFT_3)
    }

    /** Uniform integer in [from, until). */
    fun nextInt(
        from: Int,
        until: Int,
    ): Int {
        require(from < until) { "Empty range [$from, $until)" }
        val span = until.toLong() - from
        return (from + Math.floorMod(nextLong(), span)).toInt()
    }

    fun <T> pick(options: List<T>): T = options[nextInt(0, options.size)]

    private companion object {
        const val GOLDEN_GAMMA = -0x61c8864680b583ebL
        const val MIX_1 = -0x40a7b892e31b1a47L
        const val MIX_2 = -0x6b2fb644ecceee15L
        const val SHIFT_1 = 30
        const val SHIFT_2 = 27
        const val SHIFT_3 = 31
    }
}
