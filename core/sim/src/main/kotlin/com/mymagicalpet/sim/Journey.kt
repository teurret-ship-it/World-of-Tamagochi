package com.mymagicalpet.sim

/** Why a rescue step or a vacation was refused. */
enum class JourneyRefusal { NOT_ON_JOURNEY, TOO_LONG, ON_JOURNEY }

/**
 * Sickness without cruelty (CLAUDE.md section 2, roadmap iteration 14). A
 * neglected pet gets sick (medicine helps, and is free) and, if its health
 * runs out, it goes on a journey instead of dying. A short rescue of
 * [rescueSteps] friendly steps brings it home with nothing lost but a bit of
 * time. Vacation mode ("staying at grandma's") pauses the needs for up to
 * [maxVacationDays] days.
 */
data class JourneyRules(
    val rescueSteps: Int = 3,
    /** How the pet comes home: rested and fed enough to be fine. */
    val homecomingLevel: Int = 60,
    val maxVacationDays: Int = 14,
) {
    /** One step of the rescue; the last one brings the pet home. */
    fun rescue(pet: PetState): Pair<PetState, JourneyRefusal?> {
        val steps = pet.rescueSteps + 1
        return when {
            !pet.onJourney -> pet to JourneyRefusal.NOT_ON_JOURNEY
            steps < rescueSteps -> pet.copy(rescueSteps = steps) to null
            else -> homecoming(pet) to null
        }
    }

    private fun homecoming(pet: PetState): PetState {
        val home = Needs.points(homecomingLevel)
        return pet.copy(
            onJourney = false,
            rescueSteps = 0,
            needs = Needs(satiety = home, energy = home, hygiene = home, happiness = home, health = home),
            napUntilEpochMillis = null,
            awakeUntilEpochMillis = null,
        )
    }

    /** Starts a vacation of [days] days from the pet's current time (advance the pet first). */
    fun startVacation(
        pet: PetState,
        days: Int,
    ): Pair<PetState, JourneyRefusal?> =
        when {
            pet.onJourney -> pet to JourneyRefusal.ON_JOURNEY
            days !in 1..maxVacationDays -> pet to JourneyRefusal.TOO_LONG
            else -> pet.copy(vacationUntilEpochMillis = pet.updatedAtEpochMillis + days * MILLIS_PER_DAY) to null
        }

    /** Back early from vacation: the pet picks up where it was. */
    fun endVacation(pet: PetState): PetState = pet.copy(vacationUntilEpochMillis = null)

    companion object {
        val DEFAULT = JourneyRules()
        private const val MILLIS_PER_DAY = 86_400_000L
    }
}
