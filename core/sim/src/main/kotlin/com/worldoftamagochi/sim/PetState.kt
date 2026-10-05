package com.worldoftamagochi.sim

/** Everything the rules need to know about a pet at [updatedAtEpochMillis]. */
data class PetState(
    val stage: LifeStage,
    val needs: Needs,
    val sleep: SleepWindow,
    val updatedAtEpochMillis: Long,
    /** A nap started with the lights switched off lasts until this moment (or null: no nap). */
    val napUntilEpochMillis: Long? = null,
) {
    fun isAsleep(): Boolean = isAsleepAt(updatedAtEpochMillis)

    internal fun isAsleepAt(epochMillis: Long): Boolean =
        stage != LifeStage.EGG &&
            (sleep.isAsleep(epochMillis) || napUntilEpochMillis?.let { epochMillis < it } == true)

    /** The next moment after [epochMillis] at which the pet falls asleep or wakes up. */
    internal fun nextSleepChangeAfter(epochMillis: Long): Long {
        val nap = napUntilEpochMillis?.takeIf { it > epochMillis } ?: Long.MAX_VALUE
        return minOf(sleep.nextBoundaryAfter(epochMillis), nap)
    }
}
