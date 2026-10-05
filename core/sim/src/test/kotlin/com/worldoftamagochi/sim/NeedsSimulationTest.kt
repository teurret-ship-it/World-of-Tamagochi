package com.worldoftamagochi.sim

import com.worldoftamagochi.model.Gauge
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.ZoneId

class NeedsSimulationTest {
    private val start = utc(2026, 3, 1, 7)
    private val awakeAllDay = SleepWindow(startMinuteOfDay = 0, endMinuteOfDay = 0)

    private fun pet(
        stage: LifeStage = LifeStage.CHILD,
        needs: Needs = Needs(),
        sleep: SleepWindow = SleepWindow(),
        at: Long = start,
    ) = PetState(stage, needs, sleep, at)

    @Test
    fun `an awake child gets hungry at the configured rate`() {
        val after = NeedsSimulation.advance(pet(sleep = awakeAllDay), start + 5 * HOUR)
        // CHILD satiety: 12 points per hour awake.
        after.needs.gauge(Need.SATIETY) shouldBe Gauge.of(40)
        after.needs.gauge(Need.HEALTH) shouldBe Gauge.FULL
        after.updatedAtEpochMillis shouldBe start + 5 * HOUR
    }

    @Test
    fun `asleep the pet gets hungry at least four times slower and recovers energy`() {
        NeedRules.DEFAULT.rates.values.forEach { r ->
            (r.satietyAsleep * 4) shouldBeLessThanOrEqual r.satietyAwake
            (r.hygieneAsleep * 4) shouldBeLessThanOrEqual r.hygieneAwake
            (r.happinessAsleep * 4) shouldBeLessThanOrEqual r.happinessAwake
        }
        val tired = pet(needs = Needs(energy = Needs.points(20)), at = utc(2026, 3, 1, 23))
        val morning = NeedsSimulation.advance(tired, utc(2026, 3, 2, 6))
        morning.needs.energy shouldBeGreaterThan tired.needs.energy
        morning.needs.gauge(Need.HAPPINESS) shouldBe Gauge.FULL
    }

    @Test
    fun `an egg needs nothing`() {
        val egg = pet(stage = LifeStage.EGG, needs = Needs(satiety = Needs.points(50)))
        NeedsSimulation.advance(egg, start + 72 * HOUR).needs shouldBe egg.needs
        egg.isAsleep() shouldBe false
    }

    @Test
    fun `a clock wound back changes nothing`() {
        val state = pet()
        NeedsSimulation.advance(state, start - HOUR) shouldBe state
        NeedsSimulation.advance(state, start) shouldBe state
    }

    @Test
    fun `health falls only while satiety or hygiene is empty, faster when both are`() {
        val hungry = pet(needs = Needs(satiety = 0), sleep = awakeAllDay)
        NeedsSimulation.advance(hungry, start + 2 * HOUR).needs.gauge(Need.HEALTH) shouldBe Gauge.of(92)

        val both = pet(needs = Needs(satiety = 0, hygiene = 0), sleep = awakeAllDay)
        NeedsSimulation.advance(both, start + 2 * HOUR).needs.gauge(Need.HEALTH) shouldBe Gauge.of(84)

        // Satiety 12 points runs out after exactly one hour, then health starts to fall.
        val almost = pet(needs = Needs(satiety = Needs.points(12)), sleep = awakeAllDay)
        NeedsSimulation.advance(almost, start + 3 * HOUR).needs.gauge(Need.HEALTH) shouldBe Gauge.of(92)

        val neglected = pet(sleep = awakeAllDay)
        NeedsSimulation.advance(neglected, start + 30 * 24 * HOUR).needs shouldBe
            Needs(satiety = 0, energy = 0, hygiene = 0, happiness = 0, health = 0)
    }

    @Test
    fun `splitting a period anywhere gives exactly the same pet`(): Unit =
        runBlocking {
            checkAll(states, Arb.long(0L..10 * 24 * HOUR), Arb.long(0L..10 * 24 * HOUR)) { state, x, y ->
                val (first, second) = minOf(x, y) to maxOf(x, y)
                val b = state.updatedAtEpochMillis + first
                val c = state.updatedAtEpochMillis + second
                val direct = NeedsSimulation.advance(state, c)
                val split = NeedsSimulation.advance(NeedsSimulation.advance(state, b), c)
                split shouldBe direct
            }
        }

    @Test
    fun `needs never rise on their own, except energy while asleep`(): Unit =
        runBlocking {
            checkAll(states, Arb.long(1L..5 * 24 * HOUR)) { state, span ->
                val after = NeedsSimulation.advance(state, state.updatedAtEpochMillis + span).needs
                after.satiety shouldBeLessThanOrEqual state.needs.satiety
                after.hygiene shouldBeLessThanOrEqual state.needs.hygiene
                after.happiness shouldBeLessThanOrEqual state.needs.happiness
                after.health shouldBeLessThanOrEqual state.needs.health
            }
        }

    @Test
    fun `energy never rises while awake`(): Unit =
        runBlocking {
            checkAll(states, Arb.long(1L..5 * 24 * HOUR)) { state, span ->
                val awake = state.copy(sleep = awakeAllDay, napUntilEpochMillis = null)
                val after = NeedsSimulation.advance(awake, awake.updatedAtEpochMillis + span)
                after.needs.energy shouldBeLessThanOrEqual awake.needs.energy
            }
        }

    @Test
    fun `three check-ins a day keep every need above zero at every stage`() {
        // The ethics rule from CLAUDE.md section 2: morning, midday and evening
        // care is enough. Care refills satiety, hygiene and happiness; energy comes
        // only from sleep (22:00-07:00).
        val checkIns = setOf(8, 14, 20)
        for (stage in listOf(LifeStage.BABY, LifeStage.CHILD, LifeStage.TEEN, LifeStage.ADULT)) {
            var state = pet(stage = stage, sleep = SleepWindow(zone = WARSAW), at = local(WARSAW, "2026-03-01T07:00"))
            val end = state.updatedAtEpochMillis + 7 * 24 * HOUR
            while (state.updatedAtEpochMillis < end) {
                state = NeedsSimulation.advance(state, state.updatedAtEpochMillis + 15 * MINUTE)
                Need.entries.forEach { need -> state.needs[need] shouldBeGreaterThan 0L }
                val localTime =
                    java.time.Instant
                        .ofEpochMilli(state.updatedAtEpochMillis)
                        .atZone(WARSAW)
                if (localTime.minute == 0 && localTime.hour in checkIns) {
                    state = state.copy(needs = state.needs.copy(satiety = Needs.FULL, hygiene = Needs.FULL, happiness = Needs.FULL))
                }
            }
        }
    }

    @Test
    fun `a year away is computed in a few hundred steps, not millions`() {
        val state = pet(stage = LifeStage.ADULT)
        val began = System.nanoTime()
        NeedsSimulation.advance(state, start + 365L * 24 * HOUR)
        val elapsedMillis = (System.nanoTime() - began) / 1_000_000
        elapsedMillis shouldBeLessThanOrEqual 500L
    }

    private companion object {
        val zones = listOf("UTC", "Europe/Warsaw", "America/New_York", "Asia/Kolkata", "Pacific/Chatham").map(ZoneId::of)

        val states =
            arbitrary {
                val level = Arb.long(0L..Needs.FULL)
                val minute = Arb.int(0 until 24 * 60)
                PetState(
                    stage = Arb.element(LifeStage.entries).bind(),
                    needs = Needs(level.bind(), level.bind(), level.bind(), level.bind(), level.bind()),
                    sleep = SleepWindow(minute.bind(), minute.bind(), Arb.element(zones).bind()),
                    updatedAtEpochMillis = Arb.long(1_700_000_000_000L..1_900_000_000_000L).bind(),
                ).let { pet ->
                    // Half the pets are mid-nap, for up to two hours.
                    val nap = Arb.long(-2 * HOUR..2 * HOUR).bind()
                    if (nap > 0) pet.copy(napUntilEpochMillis = pet.updatedAtEpochMillis + nap) else pet
                }
            }
    }
}
