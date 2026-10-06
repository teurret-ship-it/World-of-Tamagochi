package com.mymagicalpet.sim

/**
 * The five kinds of magical creature (ADR-011). A species decides the parts
 * the rig draws (wings, horns, beak, mane, crest, fox tails), the egg and the
 * colours a pet can have: storybook colours, never neon.
 *
 * @property bodyHues the body colours this species comes in, as hue ranges in degrees.
 * @property patterns the markings it can have.
 * @property patternOffsets how far (in degrees) the markings' hue sits from the body's:
 *   close neighbours look harmonious, a far jump looks like a toy (playtest).
 */
enum class Species(
    val bodyHues: List<IntRange>,
    val patterns: List<Pattern>,
    val patternOffsets: IntRange,
) {
    /** Emerald, teal, plum or ruby; horns, little bat wings, a spade-tipped tail. */
    DRAGON(listOf(95..170, 175..205, 265..300, 345..359), listOf(Pattern.BELLY, Pattern.SPOTS), 25..55),

    /** Golden and tawny, with a white feathered head, a beak and feathered wings. */
    GRIFFIN(listOf(32..48), listOf(Pattern.PATCH, Pattern.BELLY, Pattern.SPOTS), 345..355),

    /** Pearly pastel, with a golden horn and a rainbow mane. */
    UNICORN(listOf(270..340, 180..215), listOf(Pattern.PLAIN, Pattern.SPOTS, Pattern.BELLY), 40..120),

    /** Warm coral and flame red, with a flame crest and a long tail of plumes. */
    PHOENIX(listOf(0..24, 350..359), listOf(Pattern.BELLY, Pattern.PLAIN), 25..40),

    /** Fox orange or frost blue, with big ears and more fluffy tails as it grows. */
    KITSUNE(listOf(18..30, 195..225), listOf(Pattern.BELLY, Pattern.PATCH, Pattern.PLAIN), 340..355),
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
