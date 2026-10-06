package com.mymagicalpet.sim

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class CareTest {
    private val rules = CareRules.DEFAULT
    private val noon = utc(2026, 3, 2, 12)
    private val awake = SleepWindow(zone = java.time.ZoneOffset.UTC)

    private fun pet(
        satiety: Int = 50,
        energy: Int = 50,
        hygiene: Int = 50,
        happiness: Int = 50,
        health: Int = 100,
        stage: LifeStage = LifeStage.CHILD,
        at: Long = noon,
    ) = PetState(
        stage = stage,
        needs =
            Needs(
                Needs.points(satiety),
                Needs.points(energy),
                Needs.points(hygiene),
                Needs.points(happiness),
                Needs.points(health),
            ),
        sleep = awake,
        updatedAtEpochMillis = at,
    )

    private fun done(
        pet: PetState,
        action: CareAction,
    ): CareResult.Done = rules.apply(pet, action).shouldBeInstanceOf<CareResult.Done>()

    private fun refused(
        pet: PetState,
        action: CareAction,
    ): Refusal = rules.apply(pet, action).shouldBeInstanceOf<CareResult.Refused>().reason

    @Test
    fun `feeding fills the tummy and cheers the pet up a little`() {
        val result = done(pet(), CareAction.FEED)
        result.changes shouldBe mapOf(Need.SATIETY to 35, Need.HAPPINESS to 3)
        result.answeredNeed shouldBe false
        done(pet(satiety = 10), CareAction.FEED).answeredNeed shouldBe true
    }

    @Test
    fun `a full pet politely refuses more food`() {
        refused(pet(satiety = 96), CareAction.FEED) shouldBe Refusal.FULL
        done(pet(satiety = 80), CareAction.FEED).changes[Need.SATIETY] shouldBe 20
    }

    @Test
    fun `washing scrubs in steps until clean`() {
        done(pet(hygiene = 10), CareAction.WASH).changes shouldBe mapOf(Need.HYGIENE to 20)
        refused(pet(hygiene = 100), CareAction.WASH) shouldBe Refusal.ALREADY_CLEAN
    }

    @Test
    fun `play makes the pet happy but costs energy and food`() {
        done(pet(happiness = 20), CareAction.PLAY).let {
            it.changes shouldBe mapOf(Need.HAPPINESS to 25, Need.ENERGY to -8, Need.SATIETY to -4)
            it.answeredNeed shouldBe true
        }
        refused(pet(energy = 5), CareAction.PLAY) shouldBe Refusal.TOO_TIRED
    }

    @Test
    fun `strokes give a little joy`() {
        done(pet(), CareAction.STROKE).changes shouldBe mapOf(Need.HAPPINESS to 2)
    }

    @Test
    fun `lights off starts a nap that recovers energy, and the pet can be woken`() {
        val tired = pet(energy = 20)
        val napping = done(tired, CareAction.NAP).pet
        napping.isAsleep() shouldBe true
        refused(napping, CareAction.FEED) shouldBe Refusal.ASLEEP

        val later = NeedsSimulation.advance(napping, noon + 30 * MINUTE)
        later.needs.gauge(Need.ENERGY).value shouldBe 20 + 13 / 2 + 1 // 13 points/h for 30 min, rounded up
        done(later, CareAction.WAKE).pet.isAsleep() shouldBe false

        val afterNap = NeedsSimulation.advance(napping, noon + 2 * HOUR)
        afterNap.isAsleep() shouldBe false
        afterNap.napUntilEpochMillis shouldBe null
        refused(afterNap, CareAction.WAKE) shouldBe Refusal.NOT_NAPPING
        refused(pet(energy = 90), CareAction.NAP) shouldBe Refusal.NOT_SLEEPY
    }

    @Test
    fun `nobody is cared for in their sleep, and eggs need no care`() {
        val night = pet(at = utc(2026, 3, 2, 23))
        CareAction.entries.filter { it != CareAction.WAKE }.forEach { refused(night, it) shouldBe Refusal.ASLEEP }
        CareAction.entries.forEach { refused(pet(stage = LifeStage.EGG), it) shouldBe Refusal.EGG }
    }

    @Test
    fun `a pet woken at night stays up for an hour, can play, and lights off puts it back to bed`() {
        val night = pet(at = utc(2026, 3, 2, 23), happiness = 50)
        val up = done(night, CareAction.WAKE).pet
        up.isAsleep() shouldBe false
        up.isUpLate() shouldBe true
        done(up, CareAction.PLAY).changes[Need.HAPPINESS] shouldBe 25
        // Awake at night, needs drain at the daytime pace; after an hour it dozes off again.
        val soon = NeedsSimulation.advance(up, utc(2026, 3, 2, 23) + 30 * MINUTE)
        soon.isAsleep() shouldBe false
        soon.needs.energy shouldBe NeedsSimulation.advance(pet(at = utc(2026, 3, 2, 12)), utc(2026, 3, 2, 12) + 30 * MINUTE).needs.energy
        val later = NeedsSimulation.advance(up, utc(2026, 3, 3, 1))
        later.isAsleep() shouldBe true
        later.awakeUntilEpochMillis shouldBe null
        // Lights off ends the late night at once, however full of energy the pet is.
        val bed = done(up, CareAction.NAP).pet
        bed.isAsleep() shouldBe true
        bed.napUntilEpochMillis shouldBe null
        // Simulating in pieces gives the same night as in one go.
        NeedsSimulation.advance(NeedsSimulation.advance(up, utc(2026, 3, 2, 23) + 20 * MINUTE), utc(2026, 3, 3, 9)) shouldBe
            NeedsSimulation.advance(up, utc(2026, 3, 3, 9))
    }

    @Test
    fun `waking from a nap that runs into the night also keeps the pet up for a while`() {
        val evening = pet(at = utc(2026, 3, 2, 21) + 45 * MINUTE, energy = 20)
        val napping = done(evening, CareAction.NAP).pet
        val inTheNight = NeedsSimulation.advance(napping, utc(2026, 3, 2, 22) + 10 * MINUTE)
        val woken = done(inTheNight, CareAction.WAKE).pet
        woken.isAsleep() shouldBe false
        woken.napUntilEpochMillis shouldBe null
    }
}
