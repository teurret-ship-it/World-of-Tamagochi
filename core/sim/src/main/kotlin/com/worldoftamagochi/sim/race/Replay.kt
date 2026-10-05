package com.worldoftamagochi.sim.race

/** The outcome of a whole run. */
data class RaceResult(
    val finished: Boolean,
    val ticks: Int,
    /** Exact finish time; null when the run did not finish in time. */
    val finishMicros: Long?,
    val hurdlesHit: Int,
    /** Agility faults; each one is already included in [finishMicros]. */
    val faults: Int = 0,
)

object Replay {
    /** Hard stop for a run: two minutes. Nobody needs longer for 300 m. */
    const val MAX_TICKS = 120 * RacePhysics.TICKS_PER_SECOND

    /** Re-runs [log] from scratch. The server uses exactly this to verify a time. */
    fun run(
        track: Track,
        stats: RaceStats,
        log: InputLog,
        physics: RacePhysics = RacePhysics.DEFAULT,
    ): RaceResult = run(Race(track, stats, physics), log.inputs())

    /** Every runner state of [log], tick by tick: a ghost or a replay. */
    fun frames(
        track: Track,
        stats: RaceStats,
        log: InputLog,
        physics: RacePhysics = RacePhysics.DEFAULT,
    ): List<Runner> {
        val race = Race(track, stats, physics)
        val inputs = log.inputs().iterator()
        val frames = mutableListOf(race.runner)
        while (!race.finished && frames.size <= MAX_TICKS) frames += race.step(inputs.next())
        return frames
    }

    internal fun run(
        race: Race,
        inputs: Sequence<TickInput>,
    ): RaceResult {
        val iterator = inputs.iterator()
        while (!race.finished && race.runner.tick < MAX_TICKS) race.step(iterator.next())
        return RaceResult(race.finished, race.runner.tick, race.finishMicros, race.runner.hurdlesHit, race.runner.faults)
    }
}
