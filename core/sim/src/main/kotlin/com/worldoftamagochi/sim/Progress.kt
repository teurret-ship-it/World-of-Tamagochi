package com.worldoftamagochi.sim

/**
 * The player's progression: XP and level, and the soft currency (coins).
 * Coins earned from care are capped per local day ([careCoinsDay]), so caring
 * stays a relationship, never a grind.
 */
data class PlayerProgress(
    val xp: Long = 0,
    val coins: Long = 0,
    val careCoinsToday: Int = 0,
    /** Local epoch day the [careCoinsToday] counter belongs to. */
    val careCoinsDay: Long = 0,
) {
    val level: Int get() = ProgressRules.DEFAULT.levelFor(xp)
}

/** A level-up is its own event: the UI celebrates it with a jingle and stars. */
data class ProgressUpdate(
    val progress: PlayerProgress,
    val earned: Reward,
    val levelUp: Int?,
)

data class ProgressRules(
    val xpPerCare: Int = 5,
    val xpBonusForAnsweredNeed: Int = 10,
    val coinsForAnsweredNeed: Int = 5,
    val careCoinCapPerDay: Int = 60,
    /** XP to go from level n to n+1 is [xpStep] * n: 50, 100, 150... */
    val xpStep: Int = 50,
) {
    fun levelFor(xp: Long): Int {
        var level = 1
        var needed = xpStep.toLong()
        var remaining = xp
        while (remaining >= needed) {
            remaining -= needed
            level++
            needed = xpStep.toLong() * level
        }
        return level
    }

    /** Total XP at which [level] starts. */
    fun xpAtLevel(level: Int): Long = xpStep.toLong() * (level - 1) * level / 2

    /** Rewards a successful care action done on [localEpochDay]. */
    fun reward(
        progress: PlayerProgress,
        result: CareResult.Done,
        action: CareAction,
        localEpochDay: Long,
    ): ProgressUpdate {
        val xp = if (action == CareAction.STROKE || action == CareAction.WAKE) 1 else xpPerCare
        val bonusXp = if (result.answeredNeed) xpBonusForAnsweredNeed else 0
        // A clock wound back to an earlier day must not reset the daily cap.
        val day = maxOf(localEpochDay, progress.careCoinsDay)
        val todayCount = if (progress.careCoinsDay == day) progress.careCoinsToday else 0
        val wanted = if (result.answeredNeed) coinsForAnsweredNeed else 0
        val coins = wanted.coerceAtMost(careCoinCapPerDay - todayCount).coerceAtLeast(0)
        val earned = Reward(xp + bonusXp, coins)
        val next =
            progress.copy(
                xp = progress.xp + earned.xp,
                coins = progress.coins + earned.coins,
                careCoinsToday = todayCount + coins,
                careCoinsDay = day,
            )
        val levelUp = levelFor(next.xp).takeIf { it > levelFor(progress.xp) }
        return ProgressUpdate(next, earned, levelUp)
    }

    companion object {
        val DEFAULT = ProgressRules()
    }
}
