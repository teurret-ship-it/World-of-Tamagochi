package com.mymagicalpet.sim

/** What the pet's face shows. One at a time, the most urgent wins. */
enum class Expression {
    HAPPY,
    CONTENT,
    HUNGRY,
    SLEEPY,
    DIRTY,
    SAD,
    SICK,
    ASLEEP,
}

/** Need levels (gauge points) below which the pet starts to show a need. */
data class ExpressionRules(
    val sickBelowHealth: Int = 50,
    val hungryBelow: Int = 30,
    val sleepyBelow: Int = 25,
    val dirtyBelow: Int = 30,
    val sadBelow: Int = 30,
    /** Every need at or above this makes the pet visibly happy, not just content. */
    val happyFrom: Int = 70,
) {
    /**
     * Priority follows urgency for the player: sickness first (health is the
     * consequence of neglect), then hunger, tiredness, dirt, boredom. A sleeping
     * pet always shows that it sleeps, so the player knows not to wake it.
     */
    fun expressionOf(
        needs: Needs,
        asleep: Boolean,
    ): Expression {
        fun level(need: Need) = needs.gauge(need).value
        return when {
            asleep -> Expression.ASLEEP
            level(Need.HEALTH) < sickBelowHealth -> Expression.SICK
            level(Need.SATIETY) < hungryBelow -> Expression.HUNGRY
            level(Need.ENERGY) < sleepyBelow -> Expression.SLEEPY
            level(Need.HYGIENE) < dirtyBelow -> Expression.DIRTY
            level(Need.HAPPINESS) < sadBelow -> Expression.SAD
            Need.entries.all { level(it) >= happyFrom } -> Expression.HAPPY
            else -> Expression.CONTENT
        }
    }

    /** The need the player should look after first, or null when all are fine. */
    fun mostUrgentNeed(needs: Needs): Need? =
        when (expressionOf(needs, asleep = false)) {
            Expression.SICK -> Need.HEALTH
            Expression.HUNGRY -> Need.SATIETY
            Expression.SLEEPY -> Need.ENERGY
            Expression.DIRTY -> Need.HYGIENE
            Expression.SAD -> Need.HAPPINESS
            else -> null
        }

    companion object {
        val DEFAULT = ExpressionRules()
    }
}
