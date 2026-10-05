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
        val lead =
            when (ahead) {
                is Obstacle.Hurdle -> HURDLE_LEAD_TICKS
                is Obstacle.Puddle -> PUDDLE_LEAD_TICKS
                else -> -1
            }
        val jump = runner.grounded && distance in 0..runner.vx * lead
        val finishing = track.lengthMm - runner.xMm < FINAL_SPRINT_MM
        sprinting =
            when {
                runner.exhausted || runner.stamina <= RESERVE -> false
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
        const val RESERVE = 60
        const val RESUME = 500
        const val FINAL_SPRINT_MM = 25_000
    }
}
