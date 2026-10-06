package com.mymagicalpet.sim

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ExpressionTest {
    private val rules = ExpressionRules.DEFAULT

    private fun needs(
        satiety: Int = 100,
        energy: Int = 100,
        hygiene: Int = 100,
        happiness: Int = 100,
        health: Int = 100,
    ) = Needs(
        Needs.points(satiety),
        Needs.points(energy),
        Needs.points(hygiene),
        Needs.points(happiness),
        Needs.points(health),
    )

    @Test
    fun `a well cared-for pet is happy, a mostly fine one content`() {
        rules.expressionOf(needs(), asleep = false) shouldBe Expression.HAPPY
        rules.expressionOf(needs(satiety = 60), asleep = false) shouldBe Expression.CONTENT
        rules.mostUrgentNeed(needs(satiety = 60)) shouldBe null
    }

    @Test
    fun `each need has its own face`() {
        rules.expressionOf(needs(satiety = 10), asleep = false) shouldBe Expression.HUNGRY
        rules.expressionOf(needs(energy = 10), asleep = false) shouldBe Expression.SLEEPY
        rules.expressionOf(needs(hygiene = 10), asleep = false) shouldBe Expression.DIRTY
        rules.expressionOf(needs(happiness = 10), asleep = false) shouldBe Expression.SAD
        rules.expressionOf(needs(health = 10), asleep = false) shouldBe Expression.SICK
    }

    @Test
    fun `the most urgent need wins`() {
        val everythingLow = needs(satiety = 5, energy = 5, hygiene = 5, happiness = 5, health = 5)
        rules.expressionOf(everythingLow, asleep = false) shouldBe Expression.SICK
        rules.mostUrgentNeed(everythingLow) shouldBe Need.HEALTH
        rules.mostUrgentNeed(needs(satiety = 5, hygiene = 5)) shouldBe Need.SATIETY
        rules.mostUrgentNeed(needs(energy = 5, hygiene = 5)) shouldBe Need.ENERGY
        rules.mostUrgentNeed(needs(hygiene = 5, happiness = 5)) shouldBe Need.HYGIENE
        rules.mostUrgentNeed(needs(happiness = 5)) shouldBe Need.HAPPINESS
    }

    @Test
    fun `a sleeping pet always looks asleep`() {
        rules.expressionOf(needs(satiety = 5, health = 5), asleep = true) shouldBe Expression.ASLEEP
    }
}
