package com.worldoftamagochi.sim.race

/**
 * A good, not perfect, racer: jumps hurdles and puddles in time, sprints while
 * it has stamina to spare and saves some for the finish. It sets the "author"
 * time medals are measured against, proves every track is clearable, and
 * fills empty lanes as a ghost.
 */
class Autopilot(
    private val track: Track,
) {
    private var sprinting = false

    fun next(runner: Runner): TickInput {
        val ahead = track.obstacles.firstOrNull { it.endMm >= runner.xMm }
        val distance = (ahead?.startMm ?: Int.MAX_VALUE) - runner.xMm
        // Hurdles: take off ~13 ticks out, so the pet is at the top of its arc
        // over the bar. Puddles: take off just before the water.
        // Tyres: the top of the arc as the pet passes the middle of the ring.
        val (target, lead) =
            when (ahead) {
                is Obstacle.Hurdle -> distance to HURDLE_LEAD_TICKS
                is Obstacle.Puddle -> distance to PUDDLE_LEAD_TICKS
                is Obstacle.Tyre -> ahead.centerMm - runner.xMm to HURDLE_LEAD_TICKS
                else -> distance to -1
            }
        val jump = runner.grounded && target in 0..runner.vx * lead
        val finishing = track.lengthMm - runner.xMm < FINAL_SPRINT_MM
        // Seesaws are taken at a trot: stop sprinting in time to slow down.
        val braking = ahead is Obstacle.Seesaw && distance < SEESAW_BRAKE_MM
        sprinting =
            when {
                braking || runner.exhausted || runner.stamina <= RESERVE -> false
                finishing -> true
                sprinting -> runner.stamina > RESERVE
                else -> runner.stamina >= RESUME
            }
        return TickInput(sprint = sprinting, jump = jump)
    }

    /** Plays a whole race and returns what it did. */
    fun play(stats: RaceStats): Pair<RaceResult, InputLog> {
        val race = Race(track, stats)
        val recorder = InputLog.Recorder()
        val inputs =
            generateSequence {
                next(race.runner).also(recorder::record)
            }
        val result = Replay.run(race, inputs)
        return result to recorder.build()
    }

    private companion object {
        const val HURDLE_LEAD_TICKS = 13
        const val PUDDLE_LEAD_TICKS = 2
        const val SEESAW_BRAKE_MM = 2_000
        const val RESERVE = 60
        const val RESUME = 500
        const val FINAL_SPRINT_MM = 25_000
    }
}
