package com.mymagicalpet.sim

/** What a creature has grown so far, and today's share (capped per local day). */
data class Growth(
    val points: Int = 0,
    val today: Int = 0,
    /** Local epoch day the [today] counter belongs to. */
    val day: Long = 0,
)

/** What made the creature grow. */
enum class GrowthSource { CARE, ANSWERED_NEED, RACE, TRAINING }

/** The result of growing: the new growth, the stage, and the stage just reached (if any). */
data class GrowthUpdate(
    val growth: Growth,
    val stage: LifeStage,
    val evolvedTo: LifeStage?,
)

/**
 * How a creature grows from hatchling to majestic legend (ADR-011). Care,
 * races and training all count; a daily cap makes growth a matter of days,
 * not of tapping (about three weeks to majestic for a daily player); a sick
 * creature does not grow, so care matters. Stages never go back.
 */
data class GrowthRules(
    val points: Map<GrowthSource, Int> = DEFAULT_POINTS,
    val dailyCap: Int = 40,
    /** Growth points at which each stage begins. */
    val thresholds: Map<LifeStage, Int> = DEFAULT_THRESHOLDS,
    /** Below this health the creature is too poorly to grow. */
    val minHealth: Int = 50,
) {
    /** The stage [points] reach, never below [atLeast]. */
    fun stageFor(
        points: Int,
        atLeast: LifeStage = LifeStage.BABY,
    ): LifeStage {
        val reached = thresholds.filterValues { points >= it }.keys.maxOrNull() ?: LifeStage.BABY
        return maxOf(reached, atLeast)
    }

    /** The next stage and the points it starts at; null for a majestic creature. */
    fun nextStage(stage: LifeStage): Pair<LifeStage, Int>? = thresholds.entries.firstOrNull { it.key > stage }?.toPair()

    /** 0..1 progress from the current stage to the next (1 when majestic). */
    fun progress(pet: PetState): Float {
        val next = nextStage(pet.stage) ?: return 1f
        val start = thresholds[pet.stage] ?: 0
        return ((pet.growth.points - start).toFloat() / (next.second - start)).coerceIn(0f, 1f)
    }

    /** Grows [pet] for [source] on [localEpochDay]. Eggs and sick creatures do not grow. */
    fun grow(
        pet: PetState,
        source: GrowthSource,
        localEpochDay: Long,
    ): GrowthUpdate {
        val growth = pet.growth
        if (pet.stage == LifeStage.EGG || pet.needs.gauge(Need.HEALTH).value < minHealth) {
            return GrowthUpdate(growth, pet.stage, null)
        }
        // A clock wound back to an earlier day must not reset the daily cap.
        val day = maxOf(localEpochDay, growth.day)
        val today = if (growth.day == day) growth.today else 0
        val gained = points.getValue(source).coerceAtMost(dailyCap - today).coerceAtLeast(0)
        val next = Growth(growth.points + gained, today + gained, day)
        val stage = stageFor(next.points, atLeast = pet.stage)
        return GrowthUpdate(next, stage, stage.takeIf { it > pet.stage })
    }

    companion object {
        private val DEFAULT_POINTS =
            mapOf(
                GrowthSource.CARE to 1,
                GrowthSource.ANSWERED_NEED to 4,
                GrowthSource.RACE to 3,
                GrowthSource.TRAINING to 4,
            )
        private val DEFAULT_THRESHOLDS =
            mapOf(
                LifeStage.CHILD to 20,
                LifeStage.JUNIOR to 80,
                LifeStage.TEEN to 180,
                LifeStage.ADULT to 340,
                LifeStage.MAJESTIC to 560,
            )
        val DEFAULT = GrowthRules()
    }
}

/** Applies a growth update to the pet. */
fun PetState.grown(update: GrowthUpdate): PetState = copy(stage = update.stage, growth = update.growth)
