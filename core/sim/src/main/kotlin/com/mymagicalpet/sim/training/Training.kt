package com.mymagicalpet.sim.training

import com.mymagicalpet.sim.LifeStage
import com.mymagicalpet.sim.Need
import com.mymagicalpet.sim.Needs
import com.mymagicalpet.sim.PetState
import com.mymagicalpet.sim.race.RaceStats

/** One training session; each raises one race stat. */
enum class Exercise {
    /** Speed. */
    SPRINTS,

    /** Stamina. */
    JOGGING,

    /** Agility. */
    HOOPS,

    /** Jump. */
    JUMP_ROPE,
}

/** Why the pet cannot train now. Friendly in the UI, never a scolding. */
enum class TrainingRefusal { EGG, ASLEEP, TOO_TIRED, TOO_HUNGRY, DONE_FOR_TODAY }

/** Sessions done today; the counter belongs to local day [day]. */
data class TrainingLog(
    val sessionsToday: Int = 0,
    val day: Long = 0,
)

sealed interface TrainingResult {
    data class Done(
        val pet: PetState,
        val stats: RaceStats,
        val log: TrainingLog,
        /** Stat points gained by this session. */
        val gained: Int,
    ) : TrainingResult

    data class Refused(
        val reason: TrainingRefusal,
    ) : TrainingResult
}

/**
 * Training balance. Gains shrink as a stat grows (a rookie learns fast, a
 * champion slowly), sessions are capped per day so nobody can grind, and
 * every session costs energy and food, so training and care pull on each
 * other.
 */
data class TrainingRules(
    val sessionsPerDay: Int = 3,
    val energyCost: Int = 12,
    val satietyCost: Int = 8,
    /** Exercise is fun. */
    val happinessGain: Int = 6,
    val minEnergy: Int = 20,
    val minSatiety: Int = 20,
    val gainAtRookie: Int = 8,
    val minGain: Int = 1,
) {
    /** Points one session adds to a stat at [current]. */
    fun gain(current: Int): Int =
        (gainAtRookie * (RaceStats.MAX - current) / RaceStats.MAX).coerceAtLeast(minGain).coerceAtMost(RaceStats.MAX - current)

    fun sessionsLeft(
        log: TrainingLog,
        localEpochDay: Long,
    ): Int = sessionsPerDay - doneOn(log, localEpochDay)

    /** One session of [exercise] at the pet's current time (advance the pet first). */
    fun train(
        pet: PetState,
        stats: RaceStats,
        log: TrainingLog,
        exercise: Exercise,
        localEpochDay: Long,
    ): TrainingResult {
        refusal(pet, log, localEpochDay)?.let { return TrainingResult.Refused(it) }
        val current = stats.of(exercise)
        val gained = gain(current)
        val needs =
            pet.needs
                .change(Need.ENERGY, -energyCost)
                .change(Need.SATIETY, -satietyCost)
                .change(Need.HAPPINESS, happinessGain)
        // A clock wound back to an earlier day must not reset the daily cap.
        val day = maxOf(localEpochDay, log.day)
        return TrainingResult.Done(
            pet = pet.copy(needs = needs),
            stats = stats.with(exercise, current + gained),
            log = TrainingLog(doneOn(log, day) + 1, day),
            gained = gained,
        )
    }

    /**
     * The highest any stat can be after [days] days of training (everything
     * into one stat, every day). The server rejects runs claiming more.
     */
    fun maxStatAfter(days: Long): Int {
        var stat = 0
        var sessions = days.coerceAtLeast(0) * sessionsPerDay
        while (sessions > 0 && stat < RaceStats.MAX) {
            stat += gain(stat)
            sessions--
        }
        return stat
    }

    private fun refusal(
        pet: PetState,
        log: TrainingLog,
        localEpochDay: Long,
    ): TrainingRefusal? =
        when {
            pet.stage == LifeStage.EGG -> TrainingRefusal.EGG
            pet.isAsleep() -> TrainingRefusal.ASLEEP
            sessionsLeft(log, localEpochDay) <= 0 -> TrainingRefusal.DONE_FOR_TODAY
            pet.needs.gauge(Need.ENERGY).value < minEnergy -> TrainingRefusal.TOO_TIRED
            pet.needs.gauge(Need.SATIETY).value < minSatiety -> TrainingRefusal.TOO_HUNGRY
            else -> null
        }

    private fun doneOn(
        log: TrainingLog,
        localEpochDay: Long,
    ): Int = if (log.day >= localEpochDay) log.sessionsToday else 0

    private fun Needs.change(
        need: Need,
        points: Int,
    ): Needs = with(need, this[need] + Needs.points(points))

    companion object {
        val DEFAULT = TrainingRules()
    }
}

/** The stat an exercise trains. */
fun RaceStats.of(exercise: Exercise): Int =
    when (exercise) {
        Exercise.SPRINTS -> speed
        Exercise.JOGGING -> stamina
        Exercise.HOOPS -> agility
        Exercise.JUMP_ROPE -> jump
    }

fun RaceStats.with(
    exercise: Exercise,
    value: Int,
): RaceStats =
    when (exercise) {
        Exercise.SPRINTS -> copy(speed = value)
        Exercise.JOGGING -> copy(stamina = value)
        Exercise.HOOPS -> copy(agility = value)
        Exercise.JUMP_ROPE -> copy(jump = value)
    }
