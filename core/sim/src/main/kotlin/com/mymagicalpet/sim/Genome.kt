package com.mymagicalpet.sim

/** Ear shapes; NONE gives a round, seal-like silhouette. */
enum class EarShape { ROUND, POINTY, FLOPPY, NONE }

/** Body markings drawn over the base colour. */
enum class Pattern { PLAIN, SPOTS, STRIPES, PATCH, BELLY }

/**
 * Everything that makes one pet look different from another. Generated once
 * from a seed when the egg is laid, so the server can re-derive any pet's look
 * from a single number and two players practically never share a pet.
 *
 * Proportions are per-mille of the standard body (1000 = standard).
 */
data class Genome(
    val bodyHue: Int,
    val patternHue: Int,
    val pattern: Pattern,
    val ears: EarShape,
    val hasTail: Boolean,
    val widthPerMille: Int,
    val heightPerMille: Int,
    val eyeSizePerMille: Int,
) {
    init {
        require(bodyHue in HUES && patternHue in HUES) { "Hues are degrees 0..359" }
        require(widthPerMille in WIDTH && heightPerMille in HEIGHT && eyeSizePerMille in EYES) {
            "Proportions out of range"
        }
    }

    companion object {
        val HUES = 0 until 360
        val WIDTH = 880..1120
        val HEIGHT = 900..1100
        val EYES = 850..1200

        /** Pattern hue sits at least this far from the body hue, so markings are always visible. */
        const val MIN_PATTERN_HUE_DISTANCE = 40

        fun fromSeed(seed: Long): Genome {
            val random = SeededRandom(seed)
            val bodyHue = random.nextInt(HUES.first, HUES.last + 1)
            val offset = random.nextInt(MIN_PATTERN_HUE_DISTANCE, HUES.last + 2 - MIN_PATTERN_HUE_DISTANCE)
            return Genome(
                bodyHue = bodyHue,
                patternHue = (bodyHue + offset) % HUES.count(),
                pattern = random.pick(Pattern.entries),
                ears = random.pick(EarShape.entries),
                hasTail = random.nextInt(0, 2) == 1,
                widthPerMille = random.nextInt(WIDTH.first, WIDTH.last + 1),
                heightPerMille = random.nextInt(HEIGHT.first, HEIGHT.last + 1),
                eyeSizePerMille = random.nextInt(EYES.first, EYES.last + 1),
            )
        }
    }
}
