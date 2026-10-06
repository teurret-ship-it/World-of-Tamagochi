package com.mymagicalpet.sim

/** Everything the rules need to know about a pet at [updatedAtEpochMillis]. */
data class PetState(
    val stage: LifeStage,
    val needs: Needs,
    val sleep: SleepWindow,
    val updatedAtEpochMillis: Long,
    /** A nap started with the lights switched off lasts until this moment (or null: no nap). */
    val napUntilEpochMillis: Long? = null,
    /** Woken up during its night: the pet stays up until this moment, then dozes off again (or null). */
    val awakeUntilEpochMillis: Long? = null,
    /** How far it has grown towards the next life stage (GrowthRules). */
    val growth: Growth = Growth(),
    /** Fixed when it becomes an adult: how it was raised shows (GrowthRules.formFor). */
    val form: Form? = null,
    /**
     * Neglected until its health ran out, the pet went on a journey: it never
     * dies (CLAUDE.md section 2), and a short rescue brings it home
     * ([JourneyRules]).
     */
    val onJourney: Boolean = false,
    /** Rescue steps done so far on the current journey. */
    val rescueSteps: Int = 0,
    /** "Staying at grandma's": needs stand still until this moment (vacation mode). */
    val vacationUntilEpochMillis: Long? = null,
) {
    fun isOnVacation(): Boolean = vacationUntilEpochMillis?.let { it > updatedAtEpochMillis } == true

    /** True while needs stand still: on a journey, or on vacation. */
    internal fun isPausedAt(epochMillis: Long): Boolean = vacationUntilEpochMillis?.let { epochMillis < it } == true

    fun isAsleep(): Boolean = isAsleepAt(updatedAtEpochMillis)

    /** True during a lights-off nap. */
    fun isNapping(): Boolean = napUntilEpochMillis?.let { it > updatedAtEpochMillis } == true

    /** True while the pet is up late: woken during its sleep window and not yet back in bed. */
    fun isUpLate(): Boolean = isUpLateAt(updatedAtEpochMillis)

    internal fun isAsleepAt(epochMillis: Long): Boolean =
        stage != LifeStage.EGG &&
            (napUntilEpochMillis?.let { epochMillis < it } == true || (sleep.isAsleep(epochMillis) && !isUpLateAt(epochMillis)))

    private fun isUpLateAt(epochMillis: Long): Boolean =
        awakeUntilEpochMillis?.let { epochMillis < it } == true && sleep.isAsleep(epochMillis)

    /** The next moment after [epochMillis] at which the pet falls asleep or wakes up. */
    internal fun nextSleepChangeAfter(epochMillis: Long): Long {
        val nap = napUntilEpochMillis?.takeIf { it > epochMillis } ?: Long.MAX_VALUE
        val upLate = awakeUntilEpochMillis?.takeIf { it > epochMillis } ?: Long.MAX_VALUE
        val vacation = vacationUntilEpochMillis?.takeIf { it > epochMillis } ?: Long.MAX_VALUE
        return minOf(sleep.nextBoundaryAfter(epochMillis), nap, upLate, vacation)
    }
}
