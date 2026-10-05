package com.worldoftamagochi.sim

/** Everything the rules need to know about a pet at [updatedAtEpochMillis]. */
data class PetState(
    val stage: LifeStage,
    val needs: Needs,
    val sleep: SleepWindow,
    val updatedAtEpochMillis: Long,
) {
    fun isAsleep(): Boolean = stage != LifeStage.EGG && sleep.isAsleep(updatedAtEpochMillis)
}
