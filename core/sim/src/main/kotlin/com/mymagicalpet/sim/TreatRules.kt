package com.mymagicalpet.sim

/**
 * Treats are the shop's repeatable coin sink: cheap, joyful, and capped per
 * local day so a child cannot pour every coin into snacks (or overfeed).
 */
data class TreatRules(
    val price: Int = 15,
    val perDay: Int = 3,
) {
    /** How many treats are still allowed on [localEpochDay]. */
    fun leftToday(
        progress: PlayerProgress,
        localEpochDay: Long,
    ): Int = perDay - boughtOn(progress, localEpochDay)

    /** Why a treat cannot be bought now, or null when it can. */
    fun refusal(
        progress: PlayerProgress,
        localEpochDay: Long,
    ): Refusal? =
        when {
            leftToday(progress, localEpochDay) <= 0 -> Refusal.NO_TREATS_LEFT
            progress.coins < price -> Refusal.NO_COINS
            else -> null
        }

    /** Pays for one treat. Call only when [refusal] is null. */
    fun pay(
        progress: PlayerProgress,
        localEpochDay: Long,
    ): PlayerProgress {
        // A clock wound back to an earlier day must not reset the daily cap.
        val day = maxOf(localEpochDay, progress.treatsDay)
        return progress.copy(
            coins = progress.coins - price,
            treatsToday = boughtOn(progress, day) + 1,
            treatsDay = day,
        )
    }

    private fun boughtOn(
        progress: PlayerProgress,
        localEpochDay: Long,
    ): Int = if (progress.treatsDay >= localEpochDay) progress.treatsToday else 0

    companion object {
        val DEFAULT = TreatRules()
    }
}
