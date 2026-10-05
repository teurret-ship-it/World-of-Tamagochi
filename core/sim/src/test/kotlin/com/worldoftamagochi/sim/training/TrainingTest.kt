package com.worldoftamagochi.sim.training

import com.worldoftamagochi.sim.LifeStage
import com.worldoftamagochi.sim.Need
import com.worldoftamagochi.sim.Needs
import com.worldoftamagochi.sim.PetState
import com.worldoftamagochi.sim.SleepWindow
import com.worldoftamagochi.sim.race.AgilityTracks
import com.worldoftamagochi.sim.race.Autopilot
import com.worldoftamagochi.sim.race.RaceForm
import com.worldoftamagochi.sim.race.RaceStats
import com.worldoftamagochi.sim.race.SprintTracks
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class TrainingTest {
    private val rules = TrainingRules.DEFAULT
    private val day = 20_000L
    private val noon = 12 * 3_600_000L
    private val pet = PetState(LifeStage.CHILD, Needs(), SleepWindow(), noon)

    @Test
    fun `a session raises one stat and costs energy and food, but is fun`() {
        val done = rules.train(pet, RaceStats.ROOKIE, TrainingLog(), Exercise.SPRINTS, day).shouldBeInstanceOf<TrainingResult.Done>()
        done.gained shouldBe 8
        done.stats shouldBe RaceStats(speed = 8)
        done.pet.needs
            .gauge(Need.ENERGY)
            .value shouldBe 88
        done.pet.needs
            .gauge(Need.SATIETY)
            .value shouldBe 92
        done.log shouldBe TrainingLog(1, day)
    }

    @Test
    fun `gains shrink as a stat grows, and stats never pass 100`() {
        rules.gain(0) shouldBe 8
        rules.gain(50) shouldBe 4
        rules.gain(90) shouldBe 1
        rules.gain(100) shouldBe 0
        var stats = RaceStats.ROOKIE
        repeat(500) { stats = stats.with(Exercise.HOOPS, stats.agility + rules.gain(stats.agility)) }
        stats.agility shouldBe 100
    }

    @Test
    fun `three sessions a day, and a pet must be fed and rested to train`() {
        var log = TrainingLog()
        var stats = RaceStats.ROOKIE
        repeat(3) {
            val done = rules.train(pet, stats, log, Exercise.JOGGING, day).shouldBeInstanceOf<TrainingResult.Done>()
            stats = done.stats
            log = done.log
        }
        rules.train(pet, stats, log, Exercise.JOGGING, day) shouldBe TrainingResult.Refused(TrainingRefusal.DONE_FOR_TODAY)
        rules.sessionsLeft(log, day + 1) shouldBe 3
        // A clock wound back to yesterday does not hand out new sessions.
        rules.sessionsLeft(log, day - 1) shouldBe 0
        val tired = pet.copy(needs = Needs(energy = Needs.points(15)))
        rules.train(tired, stats, TrainingLog(), Exercise.JOGGING, day) shouldBe TrainingResult.Refused(TrainingRefusal.TOO_TIRED)
        val hungry = pet.copy(needs = Needs(satiety = Needs.points(10)))
        rules.train(hungry, stats, TrainingLog(), Exercise.JOGGING, day) shouldBe TrainingResult.Refused(TrainingRefusal.TOO_HUNGRY)
        val night = pet.copy(updatedAtEpochMillis = 0)
        rules.train(night, stats, TrainingLog(), Exercise.JOGGING, day) shouldBe TrainingResult.Refused(TrainingRefusal.ASLEEP)
        rules.train(pet.copy(stage = LifeStage.EGG), stats, TrainingLog(), Exercise.JOGGING, day) shouldBe
            TrainingResult.Refused(TrainingRefusal.EGG)
    }

    @Test
    fun `the server's ceiling - no stat can be above what daily training allows`() {
        rules.maxStatAfter(0) shouldBe 0
        rules.maxStatAfter(1) shouldBe 8 + 7 + 6
        rules.maxStatAfter(1_000) shouldBe 100
        // About a month of focused training for a maxed stat; a balanced pet takes longer.
        (1L..60L).first { rules.maxStatAfter(it) >= 80 }.toInt() shouldBeInRange 7..30
    }

    @Test
    fun `form - a fed and rested pet races at its stats, a hungry one below them`() {
        RaceForm.perMille(Needs()) shouldBe 1_000
        RaceForm.perMille(Needs(satiety = Needs.points(60), energy = Needs.points(50))) shouldBe 1_000
        RaceForm.perMille(Needs(satiety = Needs.points(25))) shouldBe 500
        RaceForm.perMille(Needs(energy = 0)) shouldBe 0
        val trained = RaceStats(60, 60, 60, 60)
        RaceForm.effective(trained, 1_000) shouldBe trained
        RaceForm.effective(trained, 500) shouldBe RaceStats(5, 5, 5, 5)
        RaceForm.effective(RaceStats.ROOKIE, 0) shouldBe RaceStats(-50, -50, -50, -50)
        RaceForm.effective(RaceStats.CHAMPION, 0) shouldBe RaceStats(-50, -50, -50, -50)
    }

    @Test
    fun `a pet in poor form is noticeably slower, and every course is still clearable`() {
        val worst = RaceForm.effective(RaceStats.ROOKIE, 0)
        val track = SprintTracks.MEADOW
        val fed = requireNotNull(Autopilot(track).play(RaceStats.ROOKIE).first.finishMicros)
        val starving = requireNotNull(Autopilot(track).play(worst).first.finishMicros)
        fed shouldBeLessThan starving
        // At least 2% slower: enough to cost a medal.
        ((starving - fed) * 1_000 / fed).toInt() shouldBeGreaterThan 20
        (SprintTracks.ALL + AgilityTracks.ALL).forEach { Autopilot(it).play(worst).first.finished shouldBe true }
    }
}
