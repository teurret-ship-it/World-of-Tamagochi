package com.mymagicalpet.sim

/**
 * The five kinds of magical creature (ADR-011). A species decides the parts
 * the rig draws (wings, horns, beak, mane, crest, fox tails), the egg and the
 * colours a pet can have: storybook colours, never neon.
 *
 * @property bodyHues the body colours this species comes in, as hue ranges in degrees.
 * @property patterns the markings it can have.
 */
enum class Species(
    val bodyHues: List<IntRange>,
    val patterns: List<Pattern>,
) {
    /** Emerald, teal, plum or ruby; horns, little bat wings, a spade-tipped tail. */
    DRAGON(listOf(95..170, 175..205, 265..300, 345..359), listOf(Pattern.BELLY, Pattern.SPOTS, Pattern.STRIPES)),

    /** Golden and tawny, with a white feathered head, a beak and feathered wings. */
    GRIFFIN(listOf(25..48), listOf(Pattern.PATCH, Pattern.BELLY, Pattern.SPOTS)),

    /** Pearly pastel, with a golden horn and a rainbow mane. */
    UNICORN(listOf(270..340, 180..215), listOf(Pattern.PLAIN, Pattern.SPOTS, Pattern.BELLY)),

    /** Warm red, orange and gold, with a flame crest and a long tail of plumes. */
    PHOENIX(listOf(0..42), listOf(Pattern.BELLY, Pattern.PLAIN, Pattern.STRIPES)),

    /** Fox orange or frost blue, with big ears and more fluffy tails as it grows. */
    KITSUNE(listOf(18..34, 195..225), listOf(Pattern.BELLY, Pattern.PATCH, Pattern.PLAIN)),
    ;

    companion object {
        /** Every seed belongs to exactly one species, so the species never needs storing. */
        fun of(seed: Long): Species = entries[Math.floorMod(seed, entries.size)]

        /** A seed within a few of [seed] that hatches into [species] (kept clear of overflow at the ends). */
        fun seedFor(
            species: Species,
            seed: Long,
        ): Long {
            val n = entries.size
            val base =
                when {
                    seed < Long.MIN_VALUE + n -> seed - Math.floorMod(seed, n) + n
                    seed > Long.MAX_VALUE - n -> seed - Math.floorMod(seed, n) - n
                    else -> seed - Math.floorMod(seed, n)
                }
            return base + species.ordinal
        }
    }
}
