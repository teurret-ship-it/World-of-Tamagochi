package com.mymagicalpet.sim.journal

import com.mymagicalpet.sim.SeededRandom

/** Something the player did that quests, streaks and stickers can count. */
enum class Deed {
    /** Care that answered a need the pet was showing. */
    ANSWERED_NEED,
    FEED,
    WASH,
    PLAY,
    STROKE,
    TREAT,
    RACE,
    MEDAL,
    GOLD_MEDAL,
    TRAINING,
    PURCHASE,
    EVOLUTION,
}

/** A kind of daily quest: do [deed] [target] times. */
enum class QuestKind(
    val deed: Deed,
    val target: Int,
    val coins: Int,
) {
    FEED_3(Deed.FEED, 3, 10),
    WASH_2(Deed.WASH, 2, 10),
    PLAY_3(Deed.PLAY, 3, 10),
    STROKE_10(Deed.STROKE, 10, 8),
    ANSWER_5(Deed.ANSWERED_NEED, 5, 12),
    RACE_2(Deed.RACE, 2, 12),
    RACE_4(Deed.RACE, 4, 18),
    MEDAL_1(Deed.MEDAL, 1, 15),
    TRAIN_2(Deed.TRAINING, 2, 12),
}

data class Quest(
    val kind: QuestKind,
    val progress: Int = 0,
) {
    val done: Boolean get() = progress >= kind.target
}

/** One streak: consecutive days with at least one quest done, and the freezes that protect it. */
data class Streak(
    val days: Int = 0,
    /** The last local day that counted. */
    val lastDay: Long = Long.MIN_VALUE,
    val best: Int = 0,
    /** Freezes in hand (at most one, refilled every week). */
    val freezes: Int = 1,
    /** The local week ([day] / 7) the last freeze refill belongs to. */
    val freezeWeek: Long = Long.MIN_VALUE,
)

/**
 * The player's book of quests, streaks and stickers. Lifetime [counts] feed
 * the sticker album; [stickers] are the achievements unlocked so far.
 */
data class Journal(
    val day: Long = Long.MIN_VALUE,
    val quests: List<Quest> = emptyList(),
    val allDoneClaimed: Boolean = false,
    val streak: Streak = Streak(),
    val counts: Map<Deed, Long> = emptyMap(),
    val stickers: Set<Sticker> = emptySet(),
)

/** An achievement in the sticker album: reach [count] of [deed] in a lifetime. */
enum class Sticker(
    val deed: Deed,
    val count: Long,
) {
    FIRST_MEAL(Deed.FEED, 1),
    GOURMET(Deed.FEED, 100),
    SQUEAKY_CLEAN(Deed.WASH, 50),
    BEST_FRIENDS(Deed.STROKE, 500),
    FIRST_RACE(Deed.RACE, 1),
    RACER(Deed.RACE, 50),
    FIRST_MEDAL(Deed.MEDAL, 1),
    GOLDEN(Deed.GOLD_MEDAL, 1),
    GOLD_RUSH(Deed.GOLD_MEDAL, 10),
    ATHLETE(Deed.TRAINING, 30),
    SHOPPER(Deed.PURCHASE, 5),
    GROWING_UP(Deed.EVOLUTION, 1),
    LEGEND(Deed.EVOLUTION, 5),
}

/** What one deed changed, for the screen to celebrate. */
data class JournalUpdate(
    val journal: Journal,
    /** Quests completed by this deed. */
    val completed: List<QuestKind>,
    /** True when this deed finished the day's last quest. */
    val allDone: Boolean,
    val newStickers: List<Sticker>,
    /** Coins earned: quest rewards plus the all-done bonus. */
    val coins: Int,
    /** Streak milestone reached today (3, 7, 14, 30...), if any. */
    val streakMilestone: Int?,
)

/**
 * Daily quests, streaks and stickers (roadmap iteration 10). Three quests a
 * day, the same for a player all day long, picked from [seed] and the day so
 * the server can re-derive them. Rewards are paid at once (no claim button to
 * forget). A streak counts days with a quest done; one free freeze a week
 * covers a missed day, and losing a streak costs nothing but the streak
 * (CLAUDE.md section 2).
 */
data class JournalRules(
    val questsPerDay: Int = 3,
    val allDoneBonus: Int = 25,
    val streakMilestones: Map<Int, Int> = DEFAULT_MILESTONES,
) {
    /** The journal on [localEpochDay]: today's quests (new ones on a new day) and the streak checked for gaps. */
    fun today(
        journal: Journal,
        seed: Long,
        localEpochDay: Long,
    ): Journal {
        if (localEpochDay <= journal.day) return journal
        return journal.copy(
            day = localEpochDay,
            quests = questsFor(seed, localEpochDay),
            allDoneClaimed = false,
            streak = checkStreak(journal.streak, localEpochDay),
        )
    }

    /** Records one deed done on [localEpochDay]. */
    fun record(
        journal: Journal,
        seed: Long,
        deed: Deed,
        localEpochDay: Long,
    ): JournalUpdate {
        val current = today(journal, seed, localEpochDay)
        val quests = current.quests.map { if (it.kind.deed == deed && !it.done) it.copy(progress = it.progress + 1) else it }
        val completed = quests.filterIndexed { i, q -> q.done && !current.quests[i].done }.map { it.kind }
        val allDone = quests.isNotEmpty() && quests.all { it.done } && !current.allDoneClaimed
        val counts = current.counts + (deed to (current.counts[deed] ?: 0L) + 1)
        val newStickers = Sticker.entries.filter { it !in current.stickers && (counts[it.deed] ?: 0L) >= it.count }
        val (streak, milestone) = countStreak(current.streak, current.day, completed.isNotEmpty())
        val coins =
            completed.sumOf { it.coins } + (if (allDone) allDoneBonus else 0) + (milestone?.let { streakMilestones.getValue(it) } ?: 0)
        val next =
            current.copy(
                quests = quests,
                allDoneClaimed = current.allDoneClaimed || allDone,
                streak = streak,
                counts = counts,
                stickers = current.stickers + newStickers,
            )
        return JournalUpdate(next, completed, allDone, newStickers, coins, milestone)
    }

    /** The first quest done on a day adds that day to the streak (and may reach a milestone). */
    private fun countStreak(
        streak: Streak,
        day: Long,
        questDone: Boolean,
    ): Pair<Streak, Int?> {
        if (!questDone || streak.lastDay == day) return streak to null
        val days = streak.days + 1
        return streak.copy(days = days, lastDay = day, best = maxOf(streak.best, days)) to days.takeIf { it in streakMilestones }
    }

    /** Three different quests for the day: the same for this player all day, different tomorrow. */
    fun questsFor(
        seed: Long,
        localEpochDay: Long,
    ): List<Quest> {
        val random = SeededRandom(seed xor (localEpochDay * DAY_MIX))
        val pool = QuestKind.entries.toMutableList()
        return List(questsPerDay.coerceAtMost(pool.size)) { Quest(pool.removeAt(random.nextInt(0, pool.size))) }
    }

    /**
     * A new day: a gap of more than one day breaks the streak unless a freeze
     * covers it (one missed day per freeze). Freezes refill to one each week.
     */
    private fun checkStreak(
        streak: Streak,
        localEpochDay: Long,
    ): Streak {
        val week = Math.floorDiv(localEpochDay, DAYS_PER_WEEK)
        var current = if (week > streak.freezeWeek) streak.copy(freezes = 1, freezeWeek = week) else streak
        if (current.days == 0) return current
        val missed = localEpochDay - current.lastDay - 1
        current =
            when {
                missed <= 0 -> current
                missed <= current.freezes -> current.copy(freezes = current.freezes - missed.toInt(), lastDay = localEpochDay - 1)
                else -> current.copy(days = 0)
            }
        return current
    }

    companion object {
        private const val DAYS_PER_WEEK = 7L
        private const val DAY_MIX = -0x61c8864680b583ebL // spreads consecutive days apart
        private val DEFAULT_MILESTONES = mapOf(3 to 15, 7 to 40, 14 to 80, 30 to 150, 60 to 250, 100 to 400)
        val DEFAULT = JournalRules()
    }
}
