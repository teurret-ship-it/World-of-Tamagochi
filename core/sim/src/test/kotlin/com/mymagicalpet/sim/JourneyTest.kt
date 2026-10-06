package com.mymagicalpet.sim

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class JourneyTest {
    private val rules = JourneyRules.DEFAULT
    private val hour = 3_600_000L
    private val day = 24 * hour
    private val start = 1_790_000_000_000L
    private val pet = PetState(LifeStage.CHILD, Needs(), SleepWindow(), start)

    @Test
    fun `a pet left alone for many days goes on a journey instead of dying`() {
        val gone = NeedsSimulation.advance(pet, start + 10 * day)
        gone.needs.health shouldBe 0L
        gone.onJourney shouldBe true
        CareRules.DEFAULT.apply(gone, CareAction.FEED) shouldBe CareResult.Refused(gone, Refusal.AWAY)
        // A whole day without care makes no one leave: about two days do (sick after about one and a half).
        NeedsSimulation.advance(pet, start + day).onJourney shouldBe false
        NeedsSimulation.advance(pet, start + day + 12 * hour).onJourney shouldBe false
    }

    @Test
    fun `three rescue steps bring the pet home, fine and with nothing lost`() {
        var away = NeedsSimulation.advance(pet.copy(growth = Growth(200)), start + 10 * day)
        repeat(2) {
            val (next, refusal) = rules.rescue(away)
            refusal shouldBe null
            next.onJourney shouldBe true
            away = next
        }
        val (home, _) = rules.rescue(away)
        home.onJourney shouldBe false
        home.needs.gauge(Need.HEALTH).value shouldBe 60
        home.needs.gauge(Need.SATIETY).value shouldBe 60
        home.growth shouldBe Growth(200)
        home.stage shouldBe LifeStage.CHILD
        rules.rescue(home).second shouldBe JourneyRefusal.NOT_ON_JOURNEY
    }

    @Test
    fun `free medicine heals a sick pet, and a healthy one says it is fine`() {
        val sick = pet.copy(needs = Needs(health = Needs.points(30)))
        CareRules.DEFAULT
            .apply(sick, CareAction.MEDICINE)
            .shouldBeInstanceOf<CareResult.Done>()
            .changes shouldBe mapOf(Need.HEALTH to 35)
        CareRules.DEFAULT.apply(pet, CareAction.MEDICINE) shouldBe CareResult.Refused(pet, Refusal.NOT_SICK)
    }

    @Test
    fun `on vacation needs stand still for up to two weeks, then life goes on`() {
        val (away, refusal) = rules.startVacation(pet, days = 7)
        refusal shouldBe null
        val week = NeedsSimulation.advance(away, start + 7 * day)
        week.needs shouldBe pet.needs
        week.vacationUntilEpochMillis shouldBe null
        NeedsSimulation.advance(away, start + 7 * day + 6 * hour).needs.satiety shouldBe
            NeedsSimulation.advance(pet, start + 6 * hour).needs.satiety
        // Splitting time anywhere gives the same pet.
        NeedsSimulation.advance(NeedsSimulation.advance(away, start + 3 * day + 5), start + 9 * day) shouldBe
            NeedsSimulation.advance(away, start + 9 * day)
        CareRules.DEFAULT.apply(away, CareAction.FEED) shouldBe CareResult.Refused(away, Refusal.AWAY)
        rules.endVacation(away).isOnVacation() shouldBe false
        rules.startVacation(pet, days = 15).second shouldBe JourneyRefusal.TOO_LONG
        rules.startVacation(pet.copy(onJourney = true), days = 3).second shouldBe JourneyRefusal.ON_JOURNEY
    }
}
