package com.worldoftamagochi.sim

/** What a player can do for the pet. Rules for each live in [CareRules]. */
enum class CareAction {
    FEED,
    WASH,
    PLAY,
    NAP,
    WAKE,
    STROKE,

    /** A bought snack: a little food and a lot of joy. Paid for with [TreatRules]. */
    TREAT,
}

/** Why the pet said no. The UI turns each into a friendly reaction, never a scolding. */
enum class Refusal {
    ASLEEP,
    FULL,
    ALREADY_CLEAN,
    TOO_TIRED,
    NOT_SLEEPY,
    NOT_NAPPING,
    EGG,
    NO_COINS,
    NO_TREATS_LEFT,
}

/** What one care action earned the player. */
data class Reward(
    val xp: Int,
    val coins: Int,
) {
    companion object {
        val NONE = Reward(0, 0)
    }
}

sealed interface CareResult {
    val pet: PetState

    data class Done(
        override val pet: PetState,
        /** Gauge points gained (or lost) per need, for the floating "+N" labels. */
        val changes: Map<Need, Int>,
        /** True when the action answered a need the pet was showing: worth a bonus. */
        val answeredNeed: Boolean,
    ) : CareResult

    data class Refused(
        override val pet: PetState,
        val reason: Refusal,
    ) : CareResult
}

/**
 * Effects of care actions, in gauge points. All care balance lives here.
 *
 * Feeding and washing are generous on purpose: a child should see a bar jump,
 * not crawl. Playing costs a little energy and food, so play and care pull on
 * each other the way they do in Pou and Tamagotchi.
 */
data class CareRules(
    val feedSatiety: Int = 35,
    val feedHappiness: Int = 3,
    val washHygiene: Int = 20,
    val playHappiness: Int = 25,
    val playEnergyCost: Int = 8,
    val playSatietyCost: Int = 4,
    val strokeHappiness: Int = 2,
    val treatSatiety: Int = 10,
    val treatHappiness: Int = 20,
    val napMinutes: Int = 45,
    /** Woken at night, the pet stays up this long before dozing off again. */
    val upLateMinutes: Int = 60,
    /** Above this the pet is full and politely refuses food. */
    val fullAt: Int = 95,
    /** Below this energy the pet is too tired to play. */
    val minEnergyToPlay: Int = 10,
    /** A nap is only possible when energy is below this. */
    val napBelowEnergy: Int = 80,
    val expressions: ExpressionRules = ExpressionRules.DEFAULT,
) {
    /** Applies [action] to [pet] at the pet's current time (advance the pet first). */
    fun apply(
        pet: PetState,
        action: CareAction,
    ): CareResult {
        refusalFor(pet, action)?.let { return CareResult.Refused(pet, it) }
        val urgentBefore = expressions.mostUrgentNeed(pet.needs)
        val after =
            when (action) {
                CareAction.FEED -> {
                    pet.change(Need.SATIETY to feedSatiety, Need.HAPPINESS to feedHappiness)
                }

                CareAction.WASH -> {
                    pet.change(Need.HYGIENE to washHygiene)
                }

                CareAction.PLAY -> {
                    pet.change(Need.HAPPINESS to playHappiness, Need.ENERGY to -playEnergyCost, Need.SATIETY to -playSatietyCost)
                }

                CareAction.STROKE -> {
                    pet.change(Need.HAPPINESS to strokeHappiness)
                }

                CareAction.TREAT -> {
                    pet.change(Need.SATIETY to treatSatiety, Need.HAPPINESS to treatHappiness)
                }

                CareAction.NAP -> {
                    // Up late: lights off simply sends the pet back to bed.
                    if (pet.isUpLate()) {
                        pet.copy(awakeUntilEpochMillis = null)
                    } else {
                        pet.copy(napUntilEpochMillis = pet.updatedAtEpochMillis + napMinutes * MILLIS_PER_MINUTE)
                    }
                }

                CareAction.WAKE -> {
                    // From a nap: up for good. At night: up for a while, so a child can always play.
                    val now = pet.updatedAtEpochMillis
                    pet.copy(
                        napUntilEpochMillis = null,
                        awakeUntilEpochMillis = (now + upLateMinutes * MILLIS_PER_MINUTE).takeIf { pet.sleep.isAsleep(now) },
                    )
                }
            }
        val changes =
            Need.entries
                .associateWith { after.needs.gauge(it).value - pet.needs.gauge(it).value }
                .filterValues { it != 0 }
        return CareResult.Done(after, changes, answeredNeed = urgentBefore != null && urgentBefore in action.answers())
    }

    private fun refusalFor(
        pet: PetState,
        action: CareAction,
    ): Refusal? =
        when {
            pet.stage == LifeStage.EGG -> Refusal.EGG
            action == CareAction.WAKE -> Refusal.NOT_NAPPING.takeUnless { pet.isAsleep() }
            pet.isAsleep() -> Refusal.ASLEEP
            action == CareAction.NAP && pet.isUpLate() -> null
            else -> needRefusal(pet.needs, action)
        }

    private fun needRefusal(
        needs: Needs,
        action: CareAction,
    ): Refusal? {
        fun level(need: Need) = needs.gauge(need).value
        return when (action) {
            CareAction.FEED, CareAction.TREAT -> Refusal.FULL.takeIf { level(Need.SATIETY) >= fullAt }
            CareAction.WASH -> Refusal.ALREADY_CLEAN.takeIf { level(Need.HYGIENE) >= HYGIENE_FULL }
            CareAction.PLAY -> Refusal.TOO_TIRED.takeIf { level(Need.ENERGY) < minEnergyToPlay }
            CareAction.NAP -> Refusal.NOT_SLEEPY.takeIf { level(Need.ENERGY) >= napBelowEnergy }
            CareAction.STROKE, CareAction.WAKE -> null
        }
    }

    private fun PetState.change(vararg deltas: Pair<Need, Int>): PetState {
        var needs = this.needs
        deltas.forEach { (need, points) -> needs = needs.with(need, needs[need] + Needs.points(points)) }
        return copy(needs = needs)
    }

    private fun CareAction.answers(): Set<Need> =
        when (this) {
            CareAction.FEED -> setOf(Need.SATIETY)
            CareAction.WASH -> setOf(Need.HYGIENE)
            CareAction.PLAY, CareAction.STROKE -> setOf(Need.HAPPINESS)
            CareAction.TREAT -> setOf(Need.HAPPINESS, Need.SATIETY)
            CareAction.NAP -> setOf(Need.ENERGY)
            CareAction.WAKE -> emptySet()
        }

    companion object {
        val DEFAULT = CareRules()
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val HYGIENE_FULL = 100
    }
}
