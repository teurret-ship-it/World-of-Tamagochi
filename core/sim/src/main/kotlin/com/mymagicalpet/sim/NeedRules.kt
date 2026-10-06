package com.mymagicalpet.sim

/**
 * How fast needs change, in tenths of a gauge point per hour (== units per ms,
 * see [Needs.UNITS_PER_POINT]). All balance numbers for needs live here.
 */
data class NeedRates(
    val satietyAwake: Long,
    val satietyAsleep: Long,
    val hygieneAwake: Long,
    val hygieneAsleep: Long,
    val happinessAwake: Long,
    val happinessAsleep: Long,
    val energyDrainAwake: Long,
    val energyRecoveryAsleep: Long,
) {
    init {
        val all =
            listOf(
                satietyAwake,
                satietyAsleep,
                hygieneAwake,
                hygieneAsleep,
                happinessAwake,
                happinessAsleep,
                energyDrainAwake,
                energyRecoveryAsleep,
            )
        require(all.all { it >= 0 }) { "Rates are magnitudes and cannot be negative" }
    }

    companion object {
        val NONE = NeedRates(0, 0, 0, 0, 0, 0, 0, 0)
    }
}

data class NeedRules(
    val rates: Map<LifeStage, NeedRates>,
    /** Health lost per hour (tenths of a point) for each of satiety / hygiene that is empty. */
    val healthLossPerEmptyNeed: Long,
) {
    fun ratesFor(stage: LifeStage): NeedRates = rates.getValue(stage)

    companion object {
        /**
         * Tuned so that a player checking in 3 times a day (morning, midday,
         * evening) never lets a need run empty (CLAUDE.md section 2); the test
         * `three check-ins a day keep every need above zero` guards it.
         *
         * Babies need the most attention, adults the least: the Tamagotchi rhythm
         * of an intense first day followed by a calmer routine. Asleep, needs fall
         * at most a quarter as fast as awake, and happiness does not fall at all.
         */
        val DEFAULT =
            NeedRules(
                rates =
                    mapOf(
                        LifeStage.EGG to NeedRates.NONE,
                        LifeStage.BABY to
                            NeedRates(
                                satietyAwake = 140,
                                satietyAsleep = 30,
                                hygieneAwake = 80,
                                hygieneAsleep = 20,
                                happinessAwake = 120,
                                happinessAsleep = 0,
                                energyDrainAwake = 60,
                                energyRecoveryAsleep = 150,
                            ),
                        LifeStage.CHILD to
                            NeedRates(
                                satietyAwake = 120,
                                satietyAsleep = 30,
                                hygieneAwake = 70,
                                hygieneAsleep = 15,
                                happinessAwake = 100,
                                happinessAsleep = 0,
                                energyDrainAwake = 60,
                                energyRecoveryAsleep = 130,
                            ),
                        LifeStage.JUNIOR to
                            NeedRates(
                                satietyAwake = 110,
                                satietyAsleep = 27,
                                hygieneAwake = 65,
                                hygieneAsleep = 15,
                                happinessAwake = 95,
                                happinessAsleep = 0,
                                energyDrainAwake = 60,
                                energyRecoveryAsleep = 125,
                            ),
                        LifeStage.TEEN to
                            NeedRates(
                                satietyAwake = 100,
                                satietyAsleep = 25,
                                hygieneAwake = 60,
                                hygieneAsleep = 15,
                                happinessAwake = 90,
                                happinessAsleep = 0,
                                energyDrainAwake = 60,
                                energyRecoveryAsleep = 120,
                            ),
                        LifeStage.ADULT to
                            NeedRates(
                                satietyAwake = 90,
                                satietyAsleep = 20,
                                hygieneAwake = 50,
                                hygieneAsleep = 12,
                                happinessAwake = 80,
                                happinessAsleep = 0,
                                energyDrainAwake = 55,
                                energyRecoveryAsleep = 120,
                            ),
                        LifeStage.MAJESTIC to
                            NeedRates(
                                satietyAwake = 80,
                                satietyAsleep = 18,
                                hygieneAwake = 45,
                                hygieneAsleep = 10,
                                happinessAwake = 70,
                                happinessAsleep = 0,
                                energyDrainAwake = 50,
                                energyRecoveryAsleep = 120,
                            ),
                    ),
                healthLossPerEmptyNeed = 15,
            )
    }
}
