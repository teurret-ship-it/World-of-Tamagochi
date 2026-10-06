package com.mymagicalpet.sim

/**
 * Moves a pet forward in time. Pure and analytic: the cost depends on the number
 * of sleep/wake boundaries crossed, not on the elapsed time, and splitting a
 * period anywhere gives exactly the same result as simulating it in one step.
 */
object NeedsSimulation {
    fun advance(
        state: PetState,
        toEpochMillis: Long,
        rules: NeedRules = NeedRules.DEFAULT,
    ): PetState {
        // Time never runs backwards for a pet (see Elapsed): a rewound clock changes nothing.
        if (toEpochMillis <= state.updatedAtEpochMillis) return state
        val rates = rules.ratesFor(state.stage)
        var now = state.updatedAtEpochMillis
        var needs = state.needs
        while (now < toEpochMillis) {
            val segmentEnd = minOf(toEpochMillis, state.nextSleepChangeAfter(now))
            val asleep = state.isAsleepAt(now)
            needs = advanceSegment(needs, segmentEnd - now, rates, asleep, rules.healthLossPerEmptyNeed)
            now = segmentEnd
        }
        val nap = state.napUntilEpochMillis?.takeIf { it > toEpochMillis }
        val upLate = state.awakeUntilEpochMillis?.takeIf { it > toEpochMillis }
        return state.copy(needs = needs, updatedAtEpochMillis = toEpochMillis, napUntilEpochMillis = nap, awakeUntilEpochMillis = upLate)
    }

    /** One stretch of time with constant rates (the pet neither falls asleep nor wakes up). */
    private fun advanceSegment(
        needs: Needs,
        millis: Long,
        rates: NeedRates,
        asleep: Boolean,
        healthLossPerEmptyNeed: Long,
    ): Needs {
        val satietyRate = if (asleep) rates.satietyAsleep else rates.satietyAwake
        val hygieneRate = if (asleep) rates.hygieneAsleep else rates.hygieneAwake
        val happinessRate = if (asleep) rates.happinessAsleep else rates.happinessAwake
        val energy =
            if (asleep) {
                minOf(Needs.FULL, needs.energy + rates.energyRecoveryAsleep * millis)
            } else {
                drain(needs.energy, rates.energyDrainAwake, millis)
            }
        val starving = timeSpentEmpty(needs.satiety, satietyRate, millis)
        val filthy = timeSpentEmpty(needs.hygiene, hygieneRate, millis)
        val healthLoss = healthLossPerEmptyNeed * (starving + filthy)
        return Needs(
            satiety = drain(needs.satiety, satietyRate, millis),
            energy = energy,
            hygiene = drain(needs.hygiene, hygieneRate, millis),
            happiness = drain(needs.happiness, happinessRate, millis),
            health = (needs.health - healthLoss).coerceAtLeast(0),
        )
    }

    private fun drain(
        level: Long,
        ratePerMilli: Long,
        millis: Long,
    ): Long = (level - ratePerMilli * millis).coerceAtLeast(0)

    /** How many of the next [millis] milliseconds a need draining at [ratePerMilli] spends at zero. */
    private fun timeSpentEmpty(
        level: Long,
        ratePerMilli: Long,
        millis: Long,
    ): Long {
        val emptyAfter =
            when {
                level == 0L -> 0L
                ratePerMilli == 0L -> return 0L
                else -> (level + ratePerMilli - 1) / ratePerMilli
            }
        return (millis - emptyAfter).coerceAtLeast(0)
    }
}
