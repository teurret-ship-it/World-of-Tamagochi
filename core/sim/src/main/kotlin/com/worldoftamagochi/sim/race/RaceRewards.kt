package com.worldoftamagochi.sim.race

import com.worldoftamagochi.sim.PlayerProgress
import com.worldoftamagochi.sim.ProgressRules
import com.worldoftamagochi.sim.ProgressUpdate
import com.worldoftamagochi.sim.Reward

/**
 * What a finished race pays. Two kinds of coins:
 * - taking part: a few coins per finish, capped per day, so racing is always
 *   worth a little but never a grind;
 * - getting better: a one-time payout for each medal tier reached for the
 *   first time on a track, so improving is what really pays.
 */
data class RaceRewards(
    val xpPerFinish: Int = 10,
    val xpPerMedalTier: Int = 10,
    val coinsPerFinish: Int = 3,
    val finishCoinCapPerDay: Int = 30,
    val bronzeCoins: Int = 5,
    val silverCoins: Int = 10,
    val goldCoins: Int = 20,
    val authorCoins: Int = 30,
    val progression: ProgressRules = ProgressRules.DEFAULT,
) {
    fun coinsFor(medal: Medal): Int =
        when (medal) {
            Medal.BRONZE -> bronzeCoins
            Medal.SILVER -> silverCoins
            Medal.GOLD -> goldCoins
            Medal.AUTHOR -> authorCoins
        }

    /** Pays for a run that ended with [medal] on a track where the best medal so far was [bestBefore]. */
    fun reward(
        progress: PlayerProgress,
        finished: Boolean,
        medal: Medal?,
        bestBefore: Medal?,
        localEpochDay: Long,
    ): ProgressUpdate {
        if (!finished) return ProgressUpdate(progress, Reward.NONE, null)
        // A clock wound back to an earlier day must not reset the daily cap.
        val day = maxOf(localEpochDay, progress.raceCoinsDay)
        val paidToday = if (day == progress.raceCoinsDay) progress.raceCoinsToday else 0
        val finishCoins = coinsPerFinish.coerceAtMost(finishCoinCapPerDay - paidToday).coerceAtLeast(0)
        val newTiers = Medal.entries.filter { tier -> medal != null && tier <= medal && (bestBefore == null || tier > bestBefore) }
        val medalCoins = newTiers.sumOf(::coinsFor)
        val earned = Reward(xp = xpPerFinish + xpPerMedalTier * newTiers.size, coins = finishCoins + medalCoins)
        val next =
            progress.copy(
                xp = progress.xp + earned.xp,
                coins = progress.coins + earned.coins,
                raceCoinsToday = paidToday + finishCoins,
                raceCoinsDay = day,
            )
        val levelUp = progression.levelFor(next.xp).takeIf { it > progression.levelFor(progress.xp) }
        return ProgressUpdate(next, earned, levelUp)
    }

    companion object {
        val DEFAULT = RaceRewards()
    }
}
