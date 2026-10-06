package com.mymagicalpet.sim

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.abs

class GenomeTest {
    @Test
    fun `the same seed always grows the same pet`(): Unit =
        runBlocking {
            checkAll(Arb.long()) { seed -> Genome.fromSeed(seed) shouldBe Genome.fromSeed(seed) }
        }

    @Test
    fun `the generator is pinned so a pet never changes its look after an update`() {
        // SplitMix64 reference output for seed 0. If this fails, every existing
        // pet would change appearance: bump SimVersion and migrate instead.
        SeededRandom(0).nextLong() shouldBe -2152535657050944081L
    }

    @Test
    fun `markings always contrast with the body`(): Unit =
        runBlocking {
            checkAll(Arb.long()) { seed ->
                val g = Genome.fromSeed(seed)
                val distance = abs(g.bodyHue - g.patternHue).let { minOf(it, 360 - it) }
                (distance >= Genome.MIN_PATTERN_HUE_DISTANCE) shouldBe true
            }
        }

    @Test
    fun `a thousand eggs hatch into a varied population`() {
        val pets = (1L..1000L).map(Genome::fromSeed)
        pets.map { it.pattern }.toSet() shouldContainAll Pattern.entries
        pets.map { it.ears }.toSet() shouldContainAll EarShape.entries
        pets.map { it.hasTail }.toSet() shouldBe setOf(true, false)
        pets.map { it.bodyHue / 30 }.toSet().size shouldBeGreaterThan 10
        pets.toSet().size shouldBe 1000
    }

    @Test
    fun `random ranges are respected and empty ranges rejected`() {
        val random = SeededRandom(7)
        repeat(1000) { (random.nextInt(-3, 4) in -3..3) shouldBe true }
        assertThrows<IllegalArgumentException> { random.nextInt(5, 5) }
        assertThrows<IllegalArgumentException> { Genome.fromSeed(1).copy(bodyHue = 360) }
        assertThrows<IllegalArgumentException> { Genome.fromSeed(1).copy(widthPerMille = 10) }
    }
}
