package com.mymagicalpet.sim

import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GrowthTest {
    private val rules = GrowthRules.DEFAULT
    private val day = 20_000L
    private val hatchling = PetState(LifeStage.BABY, Needs(), SleepWindow(), 0)

    @Test
    fun `care, races and training make a creature grow, a few points at a time`() {
        rules.grow(hatchling, GrowthSource.ANSWERED_NEED, day).growth shouldBe Growth(4, 4, day)
        rules.grow(hatchling, GrowthSource.CARE, day).growth.points shouldBe 1
        rules.grow(hatchling, GrowthSource.RACE, day).growth.points shouldBe 3
        rules.grow(hatchling, GrowthSource.TRAINING, day).growth.points shouldBe 4
    }

    @Test
    fun `the first evolution comes in the first session, the next ones over days`() {
        var pet = hatchling
        var evolutions = mutableListOf<LifeStage>()
        repeat(5) {
            val update = rules.grow(pet, GrowthSource.ANSWERED_NEED, day)
            pet = pet.grown(update)
            update.evolvedTo?.let { evolutions += it }
        }
        pet.stage shouldBe LifeStage.CHILD
        evolutions shouldBe listOf(LifeStage.CHILD)
        // The daily cap: no more growth today, however much is done.
        repeat(50) { pet = pet.grown(rules.grow(pet, GrowthSource.ANSWERED_NEED, day)) }
        pet.growth.points shouldBe 40
        pet.grown(rules.grow(pet, GrowthSource.RACE, day + 1)).growth.points shouldBe 43
    }

    @Test
    fun `a daily player becomes majestic in about three weeks`() {
        // A typical day: 6 answered needs, 4 other care actions, 3 races, 2 training sessions.
        val daily =
            List(6) { GrowthSource.ANSWERED_NEED } + List(4) { GrowthSource.CARE } + List(3) { GrowthSource.RACE } +
                List(2) { GrowthSource.TRAINING }
        var pet = hatchling
        var majesticDay = -1
        for (d in 1..60) {
            daily.forEach { pet = pet.grown(rules.grow(pet, it, day + d)) }
            if (pet.stage == LifeStage.MAJESTIC && majesticDay < 0) majesticDay = d
        }
        majesticDay shouldBeInRange 14..28
    }

    @Test
    fun `sick creatures and eggs do not grow, and stages never go back`() {
        val sick = hatchling.copy(needs = Needs(health = Needs.points(30)))
        rules.grow(sick, GrowthSource.ANSWERED_NEED, day).growth shouldBe Growth()
        val egg = hatchling.copy(stage = LifeStage.EGG)
        rules.grow(egg, GrowthSource.ANSWERED_NEED, day).stage shouldBe LifeStage.EGG
        // A creature saved as an adult before growth points existed stays an adult.
        val oldAdult = hatchling.copy(stage = LifeStage.ADULT)
        rules.grow(oldAdult, GrowthSource.CARE, day).stage shouldBe LifeStage.ADULT
        rules.stageFor(0, atLeast = LifeStage.TEEN) shouldBe LifeStage.TEEN
    }

    @Test
    fun `progress towards the next stage, and nothing after majestic`() {
        rules.progress(hatchling.copy(growth = Growth(10))) shouldBe 0.5f
        rules.progress(hatchling.copy(stage = LifeStage.CHILD, growth = Growth(50))) shouldBe 0.5f
        rules.nextStage(LifeStage.MAJESTIC) shouldBe null
        rules.progress(hatchling.copy(stage = LifeStage.MAJESTIC, growth = Growth(600))) shouldBe 1f
        rules.nextStage(LifeStage.BABY) shouldBe (LifeStage.CHILD to 20)
    }

    @Test
    fun `a wound-back clock does not reset the daily cap`() {
        val capped = hatchling.copy(growth = Growth(40, 40, day))
        rules.grow(capped, GrowthSource.ANSWERED_NEED, day - 1).growth.points shouldBe 40
    }
}
