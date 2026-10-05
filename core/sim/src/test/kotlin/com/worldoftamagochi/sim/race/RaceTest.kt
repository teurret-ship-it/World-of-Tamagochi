package com.worldoftamagochi.sim.race

import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(io.kotest.common.ExperimentalKotest::class)
class RaceTest {
    private val meadow = SprintTracks.MEADOW

    private fun micros(seconds: Int) = seconds * 1_000_000L

    @Test
    fun `the same inputs always give the same run`() {
        val (first, log) = Autopilot(meadow).play(RaceStats.ROOKIE)
        val second = Autopilot(meadow).play(RaceStats.ROOKIE).first
        first shouldBe second
        Replay.run(meadow, RaceStats.ROOKIE, log) shouldBe first
    }

    @Test
    fun `a replayed log reproduces the live run frame by frame`() {
        val (result, log) = Autopilot(SprintTracks.SNOW).play(RaceStats(speed = 40, stamina = 70, agility = 10, jump = 90))
        val frames = Replay.frames(SprintTracks.SNOW, RaceStats(speed = 40, stamina = 70, agility = 10, jump = 90), log)
        frames.last().finishTick shouldBe result.ticks
        frames.size shouldBe result.ticks + 1
    }

    @Test
    fun `every launch track is clearable without touching a hurdle, in 25 to 50 seconds`() {
        SprintTracks.ALL.forEach { track ->
            val (result, _) = Autopilot(track).play(RaceStats.ROOKIE)
            result.finished shouldBe true
            result.hurdlesHit shouldBe 0
            requireNotNull(result.finishMicros) shouldBeGreaterThan micros(25)
            result.finishMicros shouldBeLessThan micros(50)
        }
    }

    @Test
    fun `any generated track is clearable`(): Unit =
        runBlocking {
            checkAll(PropTestConfig(iterations = 60), Arb.long()) { seed ->
                val track = Track.generate("t", seed, lengthMm = 260_000, density = 4)
                val result = Autopilot(track).play(RaceStats.ROOKIE).first
                result.finished shouldBe true
                result.hurdlesHit shouldBe 0
            }
        }

    @Test
    fun `skill matters - doing nothing is slow and crashes into hurdles`() {
        val idle = Replay.run(meadow, RaceStats.ROOKIE, InputLog(emptyList()))
        val pilot = Autopilot(meadow).play(RaceStats.ROOKIE).first
        idle.finished shouldBe true
        (idle.hurdlesHit > 0) shouldBe true
        requireNotNull(idle.finishMicros) shouldBeGreaterThan requireNotNull(pilot.finishMicros) * 115 / 100
    }

    @Test
    fun `stats help, but by at most 10 percent (CLAUDE md section 2)`() {
        SprintTracks.ALL.forEach { track ->
            val rookie = requireNotNull(Autopilot(track).play(RaceStats.ROOKIE).first.finishMicros)
            val champion = requireNotNull(Autopilot(track).play(RaceStats.CHAMPION).first.finishMicros)
            champion shouldBeLessThan rookie
            rookie * 1_000 / champion shouldBeLessThanOrEqual 1_100
        }
    }

    @Test
    fun `hitting a hurdle makes the pet stumble and slow down`() {
        val race = Race(Track("t", 100_000, listOf(Obstacle.Hurdle(20_000))), RaceStats.ROOKIE)
        while (race.runner.hurdlesHit == 0) race.step(TickInput.IDLE)
        race.runner.stumbleTicks shouldNotBe 0
        val slowed = race.runner.vx
        race.step(TickInput(sprint = true))
        race.runner.sprinting shouldBe false
        (slowed < RacePhysics.DEFAULT.cruiseSpeed) shouldBe true
    }

    @Test
    fun `a boost pad speeds the pet up once`() {
        val race = Race(Track("t", 100_000, listOf(Obstacle.Boost(20_000))), RaceStats.ROOKIE)
        var before = 0
        while (race.runner.xMm < 20_000) {
            before = race.runner.vx
            race.step(TickInput.IDLE)
        }
        (race.runner.vx > before) shouldBe true
    }

    @Test
    fun `sprinting drains stamina and a tired pet slows down`() {
        val race = Race(Track("t", 1_000_000, emptyList()), RaceStats.ROOKIE)
        while (!race.runner.exhausted) race.step(TickInput(sprint = true))
        race.runner.stamina shouldBe 0
        repeat(60) { race.step(TickInput(sprint = true)) }
        race.runner.vx shouldBe RacePhysics.DEFAULT.tiredSpeed
        race.runner.sprinting shouldBe false
        // Rest until recovered, then sprinting works again.
        while (race.runner.exhausted) race.step(TickInput.IDLE)
        (race.runner.stamina - RacePhysics.DEFAULT.recoverTo in 0 until RacePhysics.DEFAULT.staminaRegen) shouldBe true
        race.step(TickInput(sprint = true)).sprinting shouldBe true
    }

    @Test
    fun `a tampered log cannot claim the original time`() {
        val (honest, log) = Autopilot(meadow).play(RaceStats.ROOKIE)
        val tampered = InputLog(log.events.filter { it.kind != InputLog.Kind.JUMP })
        val replayed = Replay.run(meadow, RaceStats.ROOKIE, tampered)
        replayed.finishMicros shouldNotBe honest.finishMicros
    }

    @Test
    fun `physics are pinned - changing them must be a deliberate rules version bump`() {
        // If this fails, every stored record and ghost is invalid: bump
        // SimVersion and reset leaderboards instead of editing this number.
        Autopilot(meadow).play(RaceStats.ROOKIE).first.ticks shouldBe PINNED_MEADOW_TICKS
    }

    @Test
    fun `medals are earned against the author time`() {
        val times = MedalTimes.of(meadow)
        times.medalFor(times.authorMicros) shouldBe Medal.AUTHOR
        times.medalFor(times.gold) shouldBe Medal.GOLD
        times.medalFor(times.silver) shouldBe Medal.SILVER
        times.medalFor(times.bronze) shouldBe Medal.BRONZE
        times.medalFor(times.bronze + 1) shouldBe null
        times.medalFor(null) shouldBe null
    }

    @Test
    fun `logs record only changes and expand back exactly`() {
        val recorder = InputLog.Recorder()
        val played =
            listOf(
                TickInput(),
                TickInput(sprint = true),
                TickInput(sprint = true, jump = true),
                TickInput(),
                TickInput(jump = true),
            )
        played.forEach(recorder::record)
        val log = recorder.build()
        log.events.size shouldBe 4
        log.inputs().take(played.size).toList() shouldBe played
        val unordered = listOf(InputLog.Event(5, InputLog.Kind.JUMP), InputLog.Event(2, InputLog.Kind.JUMP))
        assertThrows<IllegalArgumentException> { InputLog(unordered) }
    }

    @Test
    fun `tracks reject overlapping or off-course obstacles and unknown ids`() {
        assertThrows<IllegalArgumentException> { Track("t", 50_000, listOf(Obstacle.Boost(10_000), Obstacle.Hurdle(10_500))) }
        assertThrows<IllegalArgumentException> { Track("t", 50_000, listOf(Obstacle.Hurdle(60_000))) }
        assertThrows<IllegalArgumentException> { RaceStats(speed = 101) }
        SprintTracks.byId("sprint-beach") shouldBe SprintTracks.BEACH
        SprintTracks.byId("nope") shouldBe null
    }

    @Test
    fun `print the balance table`() {
        // Not an assertion: a readable record in the test log of what the
        // physics produce, so balance changes show up in review.
        SprintTracks.ALL.forEach { track ->
            val rookie = requireNotNull(Autopilot(track).play(RaceStats.ROOKIE).first.finishMicros)
            val champion = requireNotNull(Autopilot(track).play(RaceStats.CHAMPION).first.finishMicros)
            val idle = Replay.run(track, RaceStats.ROOKIE, InputLog(emptyList()))
            val idleMillis = requireNotNull(idle.finishMicros) / 1000
            println("BALANCE ${track.id}: obstacles=${track.obstacles.size} rookie=${rookie / 1000} ms")
            println("BALANCE ${track.id}: champion=${champion / 1000} ms (x${rookie * 1000 / champion}/1000)")
            println("BALANCE ${track.id}: idle=$idleMillis ms hits=${idle.hurdlesHit}")
        }
    }

    private companion object {
        const val PINNED_MEADOW_TICKS = 1_916
    }
}
