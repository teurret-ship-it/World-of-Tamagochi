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
) {
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
        return minOf(sleep.nextBoundaryAfter(epochMillis), nap, upLate)
    }
}
