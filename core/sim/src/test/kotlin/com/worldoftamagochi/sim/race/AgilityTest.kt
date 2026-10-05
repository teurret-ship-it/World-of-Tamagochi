package com.worldoftamagochi.sim.race

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AgilityTest {
    private val physics = RacePhysics.DEFAULT

    /** Runs [track] with [policy] deciding each tick's input from the runner. */
    private fun run(
        track: Track,
        policy: (Runner) -> TickInput,
    ): Race {
        val race = Race(track, RaceStats.ROOKIE)
        while (!race.finished && race.runner.tick < Replay.MAX_TICKS) race.step(policy(race.runner))
        return race
    }

    @Test
    fun `every agility course is clearable without a fault, in 20 to 50 seconds`() {
        AgilityTracks.ALL.forEach { track ->
            val (result, log) = Autopilot(track).play(RaceStats.ROOKIE)
            result.faults shouldBe 0
            result.hurdlesHit shouldBe 0
            requireNotNull(result.finishMicros) shouldBeInRange 20_000_000L..50_000_000L
            Replay.run(track, RaceStats.ROOKIE, log) shouldBe result
        }
    }

    @Test
    fun `every fault adds two seconds to the time`() {
        val track = AgilityTracks.PARK
        val lazy = run(track) { TickInput.IDLE }
        lazy.runner.faults shouldBeGreaterThan 0
        val result = Replay.run(track, RaceStats.ROOKIE, InputLog(emptyList()))
        result.faults shouldBe lazy.runner.faults
        // The penalty is on top of the time the pet actually ran.
        val ranMicros = requireNotNull(result.finishMicros) - result.faults * 2_000_000L
        ranMicros shouldBeInRange (result.ticks - 1) * Race.MICROS_PER_TICK..result.ticks * Race.MICROS_PER_TICK
    }

    @Test
    fun `walking under a tyre is a fault, jumping through it is not`() {
        val track = Track("t", 30_000, listOf(Obstacle.Tyre(10_000)), Discipline.AGILITY)
        run(track) { TickInput.IDLE }.runner.faults shouldBe 1
        Autopilot(track).play(RaceStats.ROOKIE).first.faults shouldBe 0
    }

    @Test
    fun `jumping into a tunnel bumps the pet's head, and nobody sprints inside`() {
        val track = Track("t", 40_000, listOf(Obstacle.Tunnel(10_000, 6_000)), Discipline.AGILITY)
        // Jump about a metre before the entrance: still in the air when arriving.
        val bumped = run(track) { TickInput(jump = it.xMm in 9_000..9_200) }
        bumped.runner.faults shouldBe 1
        val calm = Race(track, RaceStats.ROOKIE)
        while (calm.runner.xMm < 12_000) calm.step(TickInput(sprint = true))
        calm.runner.sprinting shouldBe false
        calm.runner.vx shouldBe physics.tunnelSpeed
    }

    @Test
    fun `a seesaw taken at a sprint bounces the pet off, at a trot it does not`() {
        val track = Track("t", 40_000, listOf(Obstacle.Seesaw(15_000)), Discipline.AGILITY)
        run(track) { TickInput(sprint = true) }.runner.faults shouldBe 1
        run(track) { TickInput.IDLE }.runner.faults shouldBe 0
    }

    @Test
    fun `hurdles are faults in agility but only a stumble in sprint races`() {
        val agility = Track("t", 30_000, listOf(Obstacle.Hurdle(10_000)), Discipline.AGILITY)
        run(agility) { TickInput.IDLE }.runner.faults shouldBe 1
        val sprint = agility.copy(discipline = Discipline.SPRINT)
        val tripped = run(sprint) { TickInput.IDLE }.runner
        tripped.faults shouldBe 0
        tripped.hurdlesHit shouldBe 1
    }

    @Test
    fun `every course has a unique id and is found by it`() {
        Tracks.ALL
            .map { it.id }
            .toSet()
            .size shouldBe Tracks.ALL.size
        Tracks.ALL.forEach { Tracks.byId(it.id) shouldBe it }
        Tracks.byId("nope") shouldBe null
        AgilityTracks.ALL.all { it.discipline == Discipline.AGILITY } shouldBe true
    }

    @Test
    fun `print the agility balance table`() {
        AgilityTracks.ALL.forEach { track ->
            val rookie = Autopilot(track).play(RaceStats.ROOKIE).first
            val lazy = Replay.run(track, RaceStats.ROOKIE, InputLog(emptyList()))
            println(
                "${track.id}: autopilot ${rookie.finishMicros} faults ${rookie.faults}; idle ${lazy.finishMicros} faults ${lazy.faults}",
            )
        }
    }
}
