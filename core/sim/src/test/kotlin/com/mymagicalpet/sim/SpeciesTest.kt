package com.mymagicalpet.sim

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class SpeciesTest {
    @Test
    fun `every seed has one species, and any species can be asked for`(): Unit =
        runBlocking {
            checkAll(Arb.long()) { seed ->
                Species.entries.forEach { species ->
                    val chosen = Species.seedFor(species, seed)
                    Species.of(chosen) shouldBe species
                    Genome.fromSeed(chosen).species shouldBe species
                }
            }
        }

    @Test
    fun `each species keeps to its own storybook colours and markings`(): Unit =
        runBlocking {
            checkAll(Arb.long()) { seed ->
                val genome = Genome.fromSeed(seed)
                genome.species.bodyHues.any { genome.bodyHue in it } shouldBe true
                (genome.pattern in genome.species.patterns) shouldBe true
            }
        }

    @Test
    fun `all five species turn up among ordinary seeds`() {
        (0L until 50L).map { Genome.fromSeed(it).species }.toSet() shouldBe Species.entries.toSet()
    }
}
